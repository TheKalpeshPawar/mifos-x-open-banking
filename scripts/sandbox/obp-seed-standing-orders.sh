#!/usr/bin/env bash
# Seed realistic recurring-payment history on the sandbox.
#
# OBP exposes no read endpoint for standing orders (POST-create only), so the app derives
# the Standing Orders screen from transaction history: outgoing transactions tagged with a
# TXN_TYPE attribute, grouped by description into recurring series.
#
# Real-world semantics are respected:
#   TXN_TYPE=SO (standing order — payer-pushed, FIXED amount):
#     Rent — Amani Apartments        450.00 EUR  monthly x3 (active)
#     Monthly savings transfer       200.00 EUR  monthly x3 (active; to own savings)
#     Gym Membership                 12.50 EUR  weekly x2, stopped ~3 weeks ago (Paused)
#
# NOTE on the window: the sandbox caps every transactions endpoint at the 50 NEWEST rows
# (limit/offset/sort_direction/to_date and obp-* headers are all ignored — probed
# 2026-06-05 on v3/v4/v5.1/v6). The app derives from that window, so series need >=1
# recent payment to surface; a paused MONTHLY series (>45d-old last payment) can never
# be visible on this crowded account — hence the weekly Gym cadence for the Paused demo.
#   TXN_TYPE=DD (direct debit — merchant-pulled, VARIABLE amount allowed):
#     British Gas — energy           82.40 / 76.15 / 68.90 EUR  monthly x3
#     EE Mobile — airtime            25.00 EUR  monthly x3
#
# Also migrates earlier mis-tagged data (idempotent): Netflix moves SO -> DD
# (subscriptions are merchant-pulled in the real world); Uber/Pret -> POS; probes -> TFR.
# A compensating salary credit keeps the balance positive.
#
# Idempotent: a series whose description already appears in history is skipped; retags
# only touch rows whose TXN_TYPE differs from the target.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=obp-common.sh
source "$HERE/obp-common.sh"

ACCOUNT_ID="${OBP_DEMO_ACCOUNT:-ac.checking.001}"
SAVINGS_ACCOUNT="${OBP_SO_COUNTERPARTY:-ac.savings.001}"
SALARY_DESC="Salary — May"
SALARY_AMOUNT="2500.00"

# description|type|to_account|amounts (comma list, one per date)|dates (comma list)
SERIES=(
  "Rent — Amani Apartments|SO|ac.savings.001|450.00,450.00,450.00|2026-04-01,2026-05-01,2026-06-01"
  "Monthly savings transfer|SO|ac.savings.001|200.00,200.00,200.00|2026-04-05,2026-05-05,2026-06-05"
  "Gym Membership|SO|ac.savings.001|12.50,12.50|2026-05-13,2026-05-20"
  "British Gas — energy|DD|ac.savings.001|82.40,76.15,68.90|2026-03-20,2026-04-20,2026-05-20"
  "EE Mobile — airtime|DD|ac.savings.001|25.00,25.00,25.00|2026-03-28,2026-04-28,2026-05-28"
)

# description|target TXN_TYPE — migrate earlier mis-tagged rows to real-world semantics.
RETAGS=(
  "Netflix Subscription|DD"
  "Uber|POS"
  "Pret A Manger|POS"
  "Costa Coffee|POS"
  "probe OTP|TFR"
  "probe SEPA|TFR"
  "R5 COUNTERPARTY test|TFR"
  "R5 SANDBOX_TAN test|TFR"
  "AgentKit Pass-1 Option A cash-in probe|TFR"
)

fetch_txns() {
  obp_api GET "/obp/v6.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/owner/transactions?limit=50"
}

has_description() {
  # $1 = exact description against the cached window in $TXNS
  echo "$TXNS" | jq -e --arg d "$1" \
    '[.transactions[]? | select(.details.description == $d)] | length > 0' >/dev/null 2>&1
}

post_historical() {
  # $1 from  $2 to  $3 amount  $4 description  $5 date -> prints transaction_id or empty
  local body resp
  body=$(jq -nc --arg from "$1" --arg to "$2" --arg amt "$3" --arg desc "$4" --arg ts "${5}T09:00:00Z" \
    '{from_account_id:$from, to_account_id:$to,
      value:{currency:"EUR", amount:$amt},
      description:$desc, posted:$ts, completed:$ts,
      type:"SANDBOX_TAN", charge_policy:"SHARED"}')
  resp=$(obp_api POST "/obp/v4.0.0/banks/$OBP_BANK_ID/management/historical/transactions" "$body")
  echo "$resp" | jq -r '.transaction_id // empty'
}

tag_type() {
  # $1 = transaction_id  $2 = TXN_TYPE value
  obp_api POST "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/transactions/$1/attribute" \
    "$(jq -nc --arg v "$2" '{name:"TXN_TYPE", type:"STRING", value:$v}')" >/dev/null
}

seed_salary() {
  echo "== Compensating salary credit =="
  if has_description "$SALARY_DESC"; then echo "  already present — skip"; return 0; fi
  local txid
  txid=$(post_historical "$SAVINGS_ACCOUNT" "$ACCOUNT_ID" "$SALARY_AMOUNT" "$SALARY_DESC" "2026-05-25")
  if [ -z "$txid" ]; then echo "  FAIL salary credit"; return 1; fi
  tag_type "$txid" "SAL"
  echo "  credited $SALARY_AMOUNT EUR ($txid)"
}

seed_series() {
  echo "== Recurring series =="
  for entry in "${SERIES[@]}"; do
    local desc type to amounts dates
    IFS='|' read -r desc type to amounts dates <<<"$entry"
    if has_description "$desc"; then echo "  skip '$desc' (already seeded)"; continue; fi
    local n=0
    IFS=',' read -ra amount_list <<<"$amounts"
    IFS=',' read -ra date_list <<<"$dates"
    for i in "${!date_list[@]}"; do
      local txid
      txid=$(post_historical "$ACCOUNT_ID" "$to" "${amount_list[$i]}" "$desc" "${date_list[$i]}")
      if [ -z "$txid" ]; then echo "  FAIL '$desc' @ ${date_list[$i]}"; continue; fi
      tag_type "$txid" "$type"
      n=$((n+1))
    done
    echo "  seeded '$desc' [$type]: $n payments"
  done
}

retag_legacy() {
  echo "== Retag mis-typed legacy rows =="
  local fresh
  fresh=$(fetch_txns)
  for entry in "${RETAGS[@]}"; do
    local desc target
    IFS='|' read -r desc target <<<"$entry"
    echo "$fresh" | jq -r --arg d "$desc" --arg t "$target" '
        .transactions[]
        | select(.details.description == $d)
        | select(([.transaction_attributes[]? | select(.name=="TXN_TYPE")] | length) > 0)
        | select(([.transaction_attributes[]? | select(.name=="TXN_TYPE" and .value==$t)] | length) == 0)
        | .transaction_id' |
    while read -r tid; do
      [ -n "$tid" ] || continue
      local attr_id resp
      attr_id=$(obp_api GET "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/transactions/$tid/attributes" \
        | jq -r '.transaction_attributes[] | select(.name=="TXN_TYPE") | .transaction_attribute_id // .id // empty' | head -1)
      [ -n "$attr_id" ] || continue
      resp=$(obp_api PUT "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/transactions/$tid/attributes/$attr_id" \
        "$(jq -nc --arg v "$target" '{name:"TXN_TYPE", type:"STRING", value:$v}')")
      echo "  '$desc' $tid -> $(echo "$resp" | jq -r '.value // .message')"
    done
  done
}

verify() {
  echo "== Verify (recurring series in the 50-txn window) =="
  fetch_txns | jq -r '
    [ .transactions[]?
      | { d: .details.description,
          t: ([.transaction_attributes[]? | select(.name=="TXN_TYPE") | .value][0] // "-"),
          out: ((.details.value.amount | tonumber) < 0) }
      | select(.out and (.t == "SO" or .t == "DD")) ]
    | group_by(.t + "|" + .d)[]
    | "  [" + .[0].t + "] " + .[0].d + ": " + (length|tostring) + " payments"'
  echo "  balance: $(obp_api GET "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/owner/account" | jq -r .balance.amount) EUR"
}

main() {
  obp_login
  echo "Account: $ACCOUNT_ID  Bank: $OBP_BANK_ID"
  TXNS=$(fetch_txns)
  seed_salary
  seed_series
  retag_legacy
  verify
}

main "$@"
