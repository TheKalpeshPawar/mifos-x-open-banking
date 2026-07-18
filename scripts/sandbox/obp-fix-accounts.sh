#!/usr/bin/env bash
# Fix demo accounts on the OBP sandbox:
#   1. Rename "Main Checking (relabelled R3 retest)" -> "Main Checking" and give it the
#      standard CURRENT type.
#   2. Bring Main Checking's balance positive (credit "Salary" transaction tagged TXN_TYPE=SAL
#      if the direct balance endpoints are not permitted).
#   3. Assign every demo account a STANDARD account type (Products provisioned by
#      obp-provision-types.sh). Accounts at banks where CanUpdateAccount is not held are
#      attempted and reported as SKIP — ac.bank.uk accounts are the guaranteed set.
#
# Idempotent: rename/type updates are upserts; the balance credit only fires while the
# balance is negative.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=obp-common.sh
source "$HERE/obp-common.sh"

MAIN_ACCOUNT="ac.checking.001"
MAIN_LABEL="Main Checking"
SAVINGS_SOURCE="ac.savings.001"
SALARY_AMOUNT="${OBP_SALARY_AMOUNT:-5000.00}"

# account_id|standard_type  (bank resolved at run time from /my/accounts)
TYPE_MAP=(
  "ac.checking.001|CURRENT"
  "ac.savings.001|SAVINGS"
  "ac.alice.current.001|CURRENT"
  "ac.raj.savings.001|SAVINGS"
  "ac.maria.student.001|CURRENT"
  "ac.akpass1.current.001|CURRENT"
  "76601d84-1b91-4866-9175-422fd1310ab0|SAVINGS"
  "mifos.techstart.current|BUSINESS"
  "mifos.techstart.workcap|BUSINESS"
  "mifos.techstart.fxeur|BUSINESS"
  "neon.sophie.wallet|WALLET"
  "neon.aisha.wallet|WALLET"
  "neon.marco.freelance|BUSINESS"
)

MY_ACCOUNTS=""

load_accounts() {
  MY_ACCOUNTS=$(obp_api GET "/obp/v4.0.0/my/accounts")
}

bank_of() { echo "$MY_ACCOUNTS" | jq -r --arg id "$1" '.accounts[]? | select(.id==$id) | .bank_id // empty'; }
label_of() { echo "$MY_ACCOUNTS" | jq -r --arg id "$1" '.accounts[]? | select(.id==$id) | .label // empty'; }
type_of() { echo "$MY_ACCOUNTS" | jq -r --arg id "$1" '.accounts[]? | select(.id==$id) | .account_type // empty'; }

balance_of() {
  # $1 bank  $2 account -> prints amount (may be negative) or empty
  obp_api GET "/obp/v6.0.0/my/banks/$1/accounts/$2/account" | jq -r '.balance.amount // empty'
}

# Update an account's label + type via the management endpoint (role CanUpdateAccount).
# OBP UpdateAccountRequestJsonV310: { label, type, branch_id, account_routings[] }.
update_account() {
  # $1 bank  $2 account  $3 new_label  $4 new_type  -> echoes PASS/SKIP line
  local bank="$1" acc="$2" lbl="$3" typ="$4" body resp
  body=$(jq -nc --arg l "$lbl" --arg t "$typ" \
    '{label:$l, type:$t, branch_id:"", account_routings:[]}')
  resp=$(obp_api PUT "/obp/v5.1.0/management/banks/$bank/accounts/$acc" "$body")
  if echo "$resp" | jq -e '(.label // empty) | length > 0' >/dev/null 2>&1; then
    echo "  PASS $acc @ $bank -> type=$typ label=\"$lbl\""
    return 0
  fi
  # Some OBP builds want a singular account_routing object — retry that shape once.
  body=$(jq -nc --arg l "$lbl" --arg t "$typ" \
    '{label:$l, type:$t, branch_id:"", account_routing:{scheme:"AccountNumber", address:""}}')
  resp=$(obp_api PUT "/obp/v5.1.0/management/banks/$bank/accounts/$acc" "$body")
  if echo "$resp" | jq -e '(.label // empty) | length > 0' >/dev/null 2>&1; then
    echo "  PASS $acc @ $bank -> type=$typ label=\"$lbl\""
  else
    echo "  SKIP $acc @ $bank: $(echo "$resp" | jq -rc '.message // .' | head -c 120)"
    return 1
  fi
}

ensure_product() {
  # $1 bank  $2 product code — create-or-update (CanCreateProduct held at all three banks)
  local body resp
  body=$(jq -nc --arg b "$1" --arg n "$2" \
    '{bank_id:$b, name:($n | ascii_downcase | split("_") | map((.[0:1]|ascii_upcase)+.[1:]) | join(" ") + " Account"),
      parent_product_code:"", description:($n + " standard account type"),
      meta:{license:{id:"ODbL-1.0", name:"Open Database License"}}}')
  resp=$(obp_api PUT "/obp/v5.0.0/banks/$1/products/$2" "$body")
  if echo "$resp" | jq -e '.product_code // empty' >/dev/null 2>&1; then
    echo "  product $2 @ $1 ok"
  else
    echo "  product $2 @ $1: $(echo "$resp" | jq -rc '.message // .' | head -c 100)"
  fi
}

fix_main_checking_balance() {
  echo "== Main Checking balance =="
  local bal
  bal=$(balance_of ac.bank.uk "$MAIN_ACCOUNT")
  echo "  current balance: $bal EUR"
  if [ -z "$bal" ]; then echo "  SKIP: could not read balance"; return 0; fi
  if ! echo "$bal" | rg -q '^-'; then echo "  already positive — no action"; return 0; fi

  # Credit a salary payment from the same-bank savings account (EUR -> EUR). The direct
  # balance endpoints need roles this user does not hold, so the realistic credit is primary.
  local body resp txid
  body=$(jq -nc --arg from "$SAVINGS_SOURCE" --arg to "$MAIN_ACCOUNT" --arg amt "$SALARY_AMOUNT" \
    '{from_account_id:$from, to_account_id:$to,
      value:{currency:"EUR", amount:$amt},
      description:"Salary — June", posted:"2026-06-05T09:00:00Z", completed:"2026-06-05T09:00:00Z",
      type:"SANDBOX_TAN", charge_policy:"SHARED"}')
  resp=$(obp_api POST "/obp/v4.0.0/banks/ac.bank.uk/management/historical/transactions" "$body")
  txid=$(echo "$resp" | jq -r '.transaction_id // empty')
  if [ -z "$txid" ]; then
    echo "  FAIL salary credit: $(echo "$resp" | jq -rc '.message // .' | head -c 140)"; return 1
  fi
  echo "  credited $SALARY_AMOUNT EUR (txn $txid)"
  obp_api POST "/obp/v4.0.0/banks/ac.bank.uk/accounts/$MAIN_ACCOUNT/transactions/$txid/attribute" \
    "$(jq -nc '{name:"TXN_TYPE", type:"STRING", value:"SAL"}')" >/dev/null
  echo "  tagged TXN_TYPE=SAL"
  echo "  new balance: $(balance_of ac.bank.uk "$MAIN_ACCOUNT") EUR"
}

assign_types() {
  echo "== Standard account types (all accounts) =="
  local done_products=""
  for entry in "${TYPE_MAP[@]}"; do
    local acc="${entry%%|*}" typ="${entry#*|}"
    local bank lbl
    bank=$(bank_of "$acc")
    if [ -z "$bank" ]; then echo "  SKIP $acc: not in /my/accounts"; continue; fi
    if [ "$(type_of "$acc")" = "$typ" ]; then echo "  ok   $acc @ $bank already $typ"; continue; fi
    # Standard products exist on ac.bank.uk; create on other banks once per (bank,type).
    if [ "$bank" != "ac.bank.uk" ] && ! grep -qw "$bank/$typ" <<<"$done_products"; then
      ensure_product "$bank" "$typ"
      done_products="$done_products $bank/$typ"
    fi
    lbl=$(label_of "$acc")
    [ "$acc" = "$MAIN_ACCOUNT" ] && lbl="$MAIN_LABEL"
    update_account "$bank" "$acc" "$lbl" "$typ" || true
  done
}

verify() {
  echo "== Verify (/my/accounts after changes) =="
  load_accounts
  echo "$MY_ACCOUNTS" | jq -r '.accounts[]? | "  " + .id + " | " + .label + " | " + (.account_type // "-")'
}

main() {
  obp_login
  load_accounts
  echo "== Main Checking rename + type =="
  update_account ac.bank.uk "$MAIN_ACCOUNT" "$MAIN_LABEL" "CURRENT" || true
  fix_main_checking_balance
  assign_types
  verify
}

main "$@"
