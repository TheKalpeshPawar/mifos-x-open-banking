#!/usr/bin/env bash
# WS2 — Provision the STANDARD type vocabulary on the OBP sandbox bank.
#
# Idempotent: transaction-types are skipped if their short_code already exists;
# products and attribute-definitions use create-or-update (PUT) so re-runs are no-ops.
#
# Creates:
#   - Transaction types (registry): POS ECOM ATM TFR DD SO REF FEE INT SAL DEP CHG
#   - Account-type Products:        CURRENT SAVINGS CREDIT_CARD DEBIT LOAN FIXED_DEPOSIT WALLET BUSINESS
#   - Transaction attribute defs:   CARD_ID, TXN_TYPE (STRING)
#
# Self-grants CanCreateTransactionType (uses the held CanCreateEntitlementAtOneBank).
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=obp-common.sh
source "$HERE/obp-common.sh"

CURRENCY="${OBP_BANK_CURRENCY:-GBP}"

# code|summary
TXN_TYPES=(
  "POS|Card payment (point of sale)"
  "ECOM|Online card payment"
  "ATM|ATM cash withdrawal"
  "TFR|Account transfer"
  "DD|Direct debit"
  "SO|Standing order"
  "REF|Refund"
  "FEE|Fee"
  "INT|Interest"
  "SAL|Salary"
  "DEP|Deposit"
  "CHG|Charge"
)

# code|name
ACCOUNT_TYPES=(
  "CURRENT|Current Account"
  "SAVINGS|Savings Account"
  "CREDIT_CARD|Credit Card Account"
  "DEBIT|Debit Account"
  "LOAN|Loan Account"
  "FIXED_DEPOSIT|Fixed Deposit Account"
  "WALLET|Wallet Account"
  "BUSINESS|Business Current Account"
)

grant_role() {
  local role="$1"
  local body resp
  body=$(jq -nc --arg b "$OBP_BANK_ID" --arg r "$role" '{bank_id:$b, role_name:$r}')
  resp=$(obp_api POST "/obp/v7.0.0/users/$OBP_USER_ID/entitlements" "$body")
  if echo "$resp" | jq -e '.entitlement_id // empty' >/dev/null 2>&1; then
    echo "  granted $role"
  else
    # Already granted is fine.
    echo "  $role: $(echo "$resp" | jq -r '.message // "ok"' | head -c 80)"
  fi
}

provision_txn_types() {
  echo "== Transaction types (registry — optional; app uses TXN_TYPE attribute) =="
  local existing
  existing=$(obp_api GET "/obp/v2.0.0/banks/$OBP_BANK_ID/transaction-types" \
    | jq -r '[.transaction_types[]?.short_code] | join(" ")')
  for entry in "${TXN_TYPES[@]}"; do
    local code="${entry%%|*}" summary="${entry#*|}"
    if grep -qw "$code" <<<"$existing"; then
      echo "  skip $code (exists)"; continue
    fi
    # TransactionTypeJsonV200: id is a wrapped object {value: ...}, not a bare string.
    local body resp
    body=$(jq -nc --arg id "$(uuid)" --arg b "$OBP_BANK_ID" --arg sc "$code" \
      --arg s "$summary" --arg cur "$CURRENCY" \
      '{id:{value:$id}, bank_id:$b, short_code:$sc, summary:$s, description:$s, charge:{currency:$cur, amount:"0"}}')
    resp=$(obp_api PUT "/obp/v2.1.0/banks/$OBP_BANK_ID/transaction-types" "$body")
    if echo "$resp" | jq -e '.short_code // .shortCode // empty' >/dev/null 2>&1; then
      echo "  created $code"
    elif echo "$resp" | jq -e 'select(.message? | test("OBP-20006|OBP-40005"))' >/dev/null 2>&1; then
      echo "  SKIP registry — current user lacks CanCreateTransactionType and OBP forbids self-grant."
      echo "       The standard codes are still applied per-transaction via the TXN_TYPE attribute"
      echo "       + the client-side TransactionType map. Ask a bank admin to grant the role to enable"
      echo "       the retrievable registry (re-run is a no-op once granted)."
      return 0
    else
      echo "  FAIL $code: $(echo "$resp" | jq -rc '.message // .' | head -c 120)"
    fi
  done
}

provision_products() {
  echo "== Account-type Products =="
  for entry in "${ACCOUNT_TYPES[@]}"; do
    local code="${entry%%|*}" name="${entry#*|}"
    local body resp
    body=$(jq -nc --arg b "$OBP_BANK_ID" --arg n "$name" \
      '{bank_id:$b, name:$n, parent_product_code:"", description:$n,
        meta:{license:{id:"ODbL-1.0", name:"Open Database License"}}}')
    resp=$(obp_api PUT "/obp/v5.0.0/banks/$OBP_BANK_ID/products/$code" "$body")
    if echo "$resp" | jq -e '.product_code // empty' >/dev/null 2>&1; then
      echo "  upserted $code"
    else
      echo "  FAIL $code: $(echo "$resp" | jq -rc '.message // .' | head -c 120)"
    fi
  done
}

provision_attr_defs() {
  echo "== Transaction attribute definitions =="
  for name in CARD_ID TXN_TYPE; do
    local body resp
    body=$(jq -nc --arg n "$name" \
      '{name:$n, category:"Transaction", type:"STRING",
        description:($n + " (app-managed)"), alias:"", can_be_seen_on_views:["owner"], is_active:true}')
    resp=$(obp_api PUT "/obp/v4.0.0/banks/$OBP_BANK_ID/attribute-definitions/transaction" "$body")
    if echo "$resp" | jq -e '.attribute_definition_id // empty' >/dev/null 2>&1; then
      echo "  upserted $name"
    else
      echo "  FAIL $name: $(echo "$resp" | jq -rc '.message // .' | head -c 120)"
    fi
  done
}

main() {
  obp_login
  echo "Bank: $OBP_BANK_ID  Base: $OBP_BASE_URL"
  echo "== Roles =="
  grant_role CanCreateTransactionType
  provision_txn_types
  provision_products
  provision_attr_defs
  echo "== Verify =="
  echo "  transaction-types: $(obp_api GET "/obp/v2.0.0/banks/$OBP_BANK_ID/transaction-types" | jq -r '[.transaction_types[]?.short_code]|sort|join(",")')"
  echo "  products:          $(obp_api GET "/obp/v6.0.0/banks/$OBP_BANK_ID/products" | jq -r '[.products[]?.product_code]|sort|join(",")')"
}

main "$@"
