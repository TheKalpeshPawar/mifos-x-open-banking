#!/usr/bin/env bash
# Shared helpers for OBP sandbox provisioning/seeding scripts.
#
# Credentials are read at runtime from a markdown file (never committed). Point
# OBP_CREDENTIALS_FILE at it, or rely on the default below. The file format is the
# key: value layout produced by the OBP "create app" flow (consumer_key, username,
# password lines).
#
# Config via environment:
#   OBP_BASE_URL          default https://apisandbox.openbankproject.com
#   OBP_BANK_ID           default ac.bank.uk
#   OBP_CREDENTIALS_FILE  default ~/OpenSource/Mifos/my_openbankproject_api_credentials.md
set -euo pipefail

OBP_BASE_URL="${OBP_BASE_URL:-https://apisandbox.openbankproject.com}"
OBP_BANK_ID="${OBP_BANK_ID:-ac.bank.uk}"
OBP_CREDENTIALS_FILE="${OBP_CREDENTIALS_FILE:-$HOME/OpenSource/Mifos/my_openbankproject_api_credentials.md}"

OBP_TOKEN=""
OBP_USER_ID=""

# Read a "key: value" line from the credentials file without echoing it.
_cred() { sed -n "s/^$1: //p" "$OBP_CREDENTIALS_FILE" | head -1; }

obp_login() {
  [ -f "$OBP_CREDENTIALS_FILE" ] || { echo "ERROR: creds file not found: $OBP_CREDENTIALS_FILE" >&2; exit 1; }
  local u p ck
  u=$(_cred username); p=$(_cred password); ck=$(_cred consumer_key)
  [ -n "$u" ] && [ -n "$p" ] && [ -n "$ck" ] || { echo "ERROR: username/password/consumer_key missing in creds file" >&2; exit 1; }
  local resp
  resp=$(curl -fsS -X POST "$OBP_BASE_URL/my/logins/direct" \
    -H "Authorization: DirectLogin username=\"$u\",password=\"$p\",consumer_key=\"$ck\"" \
    -H "Content-Type: application/json")
  OBP_TOKEN=$(echo "$resp" | jq -r '.token // empty')
  [ -n "$OBP_TOKEN" ] || { echo "ERROR: DirectLogin failed: $resp" >&2; exit 1; }
  OBP_USER_ID=$(obp_api GET "/obp/v4.0.0/users/current" | jq -r '.user_id // empty')
  [ -n "$OBP_USER_ID" ] || { echo "ERROR: could not resolve current user_id" >&2; exit 1; }
  echo "Logged in. user_id=$OBP_USER_ID token=${OBP_TOKEN:0:6}…"
}

# obp_api METHOD PATH [JSON_BODY]  -> prints response body (does not fail on HTTP error;
# the caller inspects the JSON). Use obp_api_strict for fail-on-error.
obp_api() {
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -sS -X "$method" "$OBP_BASE_URL$path" \
      -H "Authorization: DirectLogin token=\"$OBP_TOKEN\"" \
      -H "Content-Type: application/json" -d "$body"
  else
    curl -sS -X "$method" "$OBP_BASE_URL$path" \
      -H "Authorization: DirectLogin token=\"$OBP_TOKEN\"" \
      -H "Content-Type: application/json"
  fi
}

uuid() { cat /proc/sys/kernel/random/uuid; }

# True if a JSON response represents an OBP error (has a non-empty "code"/"message" error shape).
is_obp_error() { echo "$1" | jq -e 'has("code") and (.code >= 400)' >/dev/null 2>&1; }
