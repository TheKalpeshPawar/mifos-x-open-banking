#!/usr/bin/env bash
# WS3 — Seed the card↔transaction link on the sandbox.
#
# OBP has no native card→transaction relationship, so we attach it as data:
#   1. Ensure a SECOND card exists on the demo account (two cards, one account — the
#      case that proves per-card separation).
#   2. Tag the account's existing transactions with two STRING transaction-attributes:
#        CARD_ID  = the owning card's number (bank_card_number — card_id is null in payloads)
#        TXN_TYPE = a standard short code (POS/ECOM/ATM/TFR/DD/SO/...)
#      Transactions are split deterministically across the two cards.
#
# Idempotent: a transaction already carrying a CARD_ID attribute is skipped; the extra
# card is created only if its number is not already present.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=obp-common.sh
source "$HERE/obp-common.sh"

ACCOUNT_ID="${OBP_DEMO_ACCOUNT:-ac.checking.001}"
CARD_B_NUMBER="${OBP_DEMO_CARD_B:-4000123456789028}"
TXN_LIMIT="${OBP_SEED_TXN_LIMIT:-20}"
# Card-instrument-leaning rotation so most seeded txns read as real card activity.
TXN_TYPE_CYCLE=(POS ECOM ATM POS TFR POS DD POS ECOM SO)

resolve_card_a() {
  obp_api GET "/obp/v7.0.0/cards" \
    | jq -r --arg acc "$ACCOUNT_ID" \
      '[.cards[]? | select((.account.id // .account.account_id) == $acc)][0].bank_card_number // empty'
}

ensure_card_b() {
  local existing
  existing=$(obp_api GET "/obp/v7.0.0/cards" | jq -r '[.cards[]?.bank_card_number] | join(" ")')
  if grep -qw "$CARD_B_NUMBER" <<<"$existing"; then
    echo "  card B $CARD_B_NUMBER already present"; return 0
  fi
  local customer_id body resp
  customer_id=$(obp_api GET "/obp/v5.1.0/banks/$OBP_BANK_ID/customers" 2>/dev/null \
    | jq -r '.customers[0].customer_id // empty')
  body=$(jq -nc --arg acc "$ACCOUNT_ID" --arg num "$CARD_B_NUMBER" --arg cust "$customer_id" \
    '{account_id:$acc, card_number:$num, card_type:"Debit", name_on_card:"A Customer",
      issue_number:"1", serial_number:"0002", valid_from_date:"2024-01-01T00:00:00Z",
      expires_date:"2030-01-01T00:00:00Z", enabled:true, technology:"chip",
      networks:["VISA"], allows:["credit","debit"], brand:"Visa",
      customer_id:$cust, replacement:null, pin_reset:[], collected:null, posted:null}')
  resp=$(obp_api POST "/obp/v5.0.0/management/banks/$OBP_BANK_ID/cards" "$body")
  if echo "$resp" | jq -e '.card_number // empty' >/dev/null 2>&1; then
    echo "  created card B $CARD_B_NUMBER"
  else
    echo "  FAIL create card B: $(echo "$resp" | jq -rc '.message // .' | head -c 160)"; return 1
  fi
}

txn_has_card_id() {
  # $1 = transaction_id ; returns 0 if a CARD_ID attribute already exists
  obp_api GET "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/transactions/$1/attributes" \
    | jq -e '[.transaction_attributes[]?.name] | index("CARD_ID")' >/dev/null 2>&1
}

post_attr() {
  # $1 txn_id  $2 name  $3 value
  obp_api POST "/obp/v4.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/transactions/$1/attribute" \
    "$(jq -nc --arg n "$2" --arg v "$3" '{name:$n, type:"STRING", value:$v}')" >/dev/null
}

seed_attributes() {
  local card_a card_b="$CARD_B_NUMBER"
  card_a=$(resolve_card_a)
  [ -n "$card_a" ] || { echo "ERROR: no existing card on $ACCOUNT_ID"; exit 1; }
  echo "  card A=$card_a  card B=$card_b"
  local ids
  ids=$(obp_api GET "/obp/v6.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/owner/transactions?limit=$TXN_LIMIT" \
    | jq -r '.transactions[]?.transaction_id // empty')
  local i=0 tagged=0 skipped=0
  while IFS= read -r tid; do
    [ -n "$tid" ] || continue
    if txn_has_card_id "$tid"; then skipped=$((skipped+1)); i=$((i+1)); continue; fi
    local card type
    card=$([ $((i % 2)) -eq 0 ] && echo "$card_a" || echo "$card_b")
    type=${TXN_TYPE_CYCLE[$((i % ${#TXN_TYPE_CYCLE[@]}))]}
    post_attr "$tid" CARD_ID "$card"
    post_attr "$tid" TXN_TYPE "$type"
    tagged=$((tagged+1)); i=$((i+1))
  done <<<"$ids"
  echo "  tagged=$tagged skipped(existing)=$skipped"
}

verify() {
  echo "== Verify (per-card counts via v6 inline attributes) =="
  obp_api GET "/obp/v6.0.0/banks/$OBP_BANK_ID/accounts/$ACCOUNT_ID/owner/transactions?limit=$TXN_LIMIT" \
    | jq -r '
      [ .transactions[]? as $t
        | { card: ([ $t.transaction_attributes[]? | select(.name=="CARD_ID") | .value ][0] // "untagged"),
            type: ([ $t.transaction_attributes[]? | select(.name=="TXN_TYPE") | .value ][0] // "-") } ]
      | group_by(.card)[] | "  card " + .[0].card + ": " + (length|tostring)
        + " txns (types: " + ([.[].type]|unique|join(","))+")"'
}

main() {
  obp_login
  echo "Account: $ACCOUNT_ID  Bank: $OBP_BANK_ID"
  echo "== Ensure second card =="
  ensure_card_b
  echo "== Tag transactions =="
  seed_attributes
  verify
}

main "$@"
