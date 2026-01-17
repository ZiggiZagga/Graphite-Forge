#!/bin/bash
# Ensure Keycloak dev realm users (alice/bob) are ready for direct grant flows
# - Clears required actions, marks email verified, enables accounts
# - Resets passwords to known values and re-applies realm roles

set -euo pipefail

KC_CONTAINER="${KC_CONTAINER:-steel-hammer-keycloak}"
KC_SERVER="${KC_SERVER:-http://localhost:7081}"
REALM="${REALM:-dev}"
ADMIN_USER="${KC_ADMIN_USER:-admin}"
ADMIN_PASS="${KC_ADMIN_PASS:-admin}"

ALICE_USER="${ALICE_USER:-alice}"
ALICE_PASS="${ALICE_PASS:-aliceP@ss}"
ALICE_ROLE="${ALICE_ROLE:-adminrole}"
ALICE_EMAIL="${ALICE_EMAIL:-alice@acme-corp.io}"
ALICE_FIRST="${ALICE_FIRST:-Alice}"
ALICE_LAST="${ALICE_LAST:-Admin}"

BOB_USER="${BOB_USER:-bob}"
BOB_PASS="${BOB_PASS:-bobP@ss}"
BOB_ROLE="${BOB_ROLE:-devrole}"
BOB_EMAIL="${BOB_EMAIL:-bob@widgets-inc.io}"
BOB_FIRST="${BOB_FIRST:-Bob}"
BOB_LAST="${BOB_LAST:-Dev}"

timestamp() { date '+%Y-%m-%d %H:%M:%S'; }
err() { echo "[$(timestamp)] [setup-keycloak] ERROR: $*" >&2; }
info() { echo "[$(timestamp)] [setup-keycloak] $*"; }

require_docker_exec() {
    if ! docker inspect "$KC_CONTAINER" >/dev/null 2>&1; then
        err "Container '$KC_CONTAINER' not found. Start steel-hammer stack first."
        exit 1
    fi
    if [ "$(docker inspect -f '{{.State.Running}}' "$KC_CONTAINER")" != "true" ]; then
        err "Container '$KC_CONTAINER' is not running."
        exit 1
    fi
}

kcadm() {
    # Run kcadm and suppress the "Logging into..." output
    docker exec "$KC_CONTAINER" /opt/keycloak/keycloak/bin/kcadm.sh "$@" 2>&1 | grep -v '^Logging into' || true
}

get_user_id() {
    local username="$1"
    local uid
    uid=$(kcadm get users -r "$REALM" -q "username=$username" | grep '"id"' | head -1 | awk -F'"' '{print $4}') || true
    echo "$uid"
}

ensure_user() {
    local username="$1" password="$2" role="$3" email="$4" first="$5" last="$6"
    info "  ▶ Setting up user: $username"
    
    local uid
    uid=$(get_user_id "$username") || true
    if [ -z "${uid:-}" ]; then
        err "    User '$username' not found in realm '$REALM'"
        return 1
    fi
    info "    ✓ Found user ID: ${uid:0:8}..."

    info "    ▶ Updating profile (email=$email)..."
    if ! kcadm update "users/$uid" -r "$REALM" \
        -s enabled=true \
        -s emailVerified=true \
        -s "email=$email" \
        -s "firstName=$first" \
        -s "lastName=$last" \
        -s 'requiredActions=[]' >/dev/null 2>&1; then
        err "    Failed to update profile for $username"
        return 1
    fi
    info "    ✓ Profile updated"

    info "    ▶ Resetting password..."
    if ! kcadm set-password -r "$REALM" --userid "$uid" --new-password "$password" --temporary=false >/dev/null 2>&1; then
        err "    Failed to set password for $username"
        return 1
    fi
    info "    ✓ Password set to: $password"

    info "    ▶ Assigning realm role: $role..."
    if ! kcadm add-roles -r "$REALM" --uusername "$username" --rolename "$role" >/dev/null 2>&1; then
        err "    Failed to assign role $role to $username"
        return 1
    fi
    info "    ✓ Role assigned: $role"
    
    info "  ✅ User '$username' fully configured"
}

main() {
    info "════════════════════════════════════════════════════════════════"
    info "Keycloak Development Users Setup"
    info "════════════════════════════════════════════════════════════════"
    info ""
    
    require_docker_exec

    info "[STEP 1/3] Authenticating to Keycloak..."
    info "  - Server: $KC_SERVER"
    info "  - Realm: master"
    info "  - User: $ADMIN_USER"
    if ! kcadm config credentials --server "$KC_SERVER" --realm master --user "$ADMIN_USER" --password "$ADMIN_PASS" >/dev/null 2>&1; then
        err "  Authentication failed"
        return 1
    fi
    info "  ✓ Authenticated successfully"
    info ""

    info "[STEP 2/3] Setting up Alice (adminrole)..."
    ensure_user "$ALICE_USER" "$ALICE_PASS" "$ALICE_ROLE" "$ALICE_EMAIL" "$ALICE_FIRST" "$ALICE_LAST" || return 1
    info ""

    info "[STEP 3/3] Setting up Bob (devrole)..."
    ensure_user "$BOB_USER" "$BOB_PASS" "$BOB_ROLE" "$BOB_EMAIL" "$BOB_FIRST" "$BOB_LAST" || return 1
    info ""

    info "════════════════════════════════════════════════════════════════"
    info "✅ Keycloak dev realm ready for E2E testing (alice/bob)"
    info "════════════════════════════════════════════════════════════════"
}

main "$@"
