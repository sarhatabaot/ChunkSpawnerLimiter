#!/usr/bin/env bash
set -euo pipefail

ROOT=${WATCHWOLF_ROOT:-"$(pwd)/runtime"}
BRANCH_FLAG=${WATCHWOLF_BRANCH_FLAG:---dev}
THREADS=${WATCHWOLF_BUILD_THREADS:-2}

for cmd in docker curl git; do
  command -v "$cmd" >/dev/null || { echo "Missing dependency: $cmd" >&2; exit 1; }
done

docker info >/dev/null
mkdir -p "$ROOT"
ROOT=$(cd "$ROOT" && pwd)

setup=$(mktemp)
trap 'rm -f "$setup"' EXIT
curl -fsSL https://raw.githubusercontent.com/watch-wolf/WatchWolf/dev/WatchWolfSetup.sh -o "$setup"

# This is the upstream artifact/bootstrap stage. Compose replaces upstream --install/--run.
# It intentionally does NOT use --skip-spigot-build, because unmodified ServersManager
# still expects its local server-types cache when it creates Minecraft child containers.
bash "$setup" --build "$BRANCH_FLAG" --threads "$THREADS" --path "$ROOT"

machine_ip=$(hostname -I 2>/dev/null | awk '{print $1}')
if [[ -z "$machine_ip" ]]; then
  echo "Could not determine MACHINE_IP; set it manually in .env" >&2
  machine_ip=127.0.0.1
fi
public_ip=$(curl -fsS --connect-timeout 10 --max-time 30 https://ifconfig.me/ip || true)
if [[ -z "$public_ip" ]]; then
  echo "Could not determine PUBLIC_IP; set it manually in .env" >&2
  public_ip="$machine_ip"
fi

cat > .env <<ENV
WATCHWOLF_ROOT=$ROOT
MACHINE_IP=$machine_ip
PUBLIC_IP=$public_ip
ENV

mkdir -p \
  "$ROOT/ServersManager/ci/release/logs" \
  "$ROOT/ServersManager/ci/release/tmp"

echo "Bootstrap complete. Start WatchWolf with: docker compose up -d --build"
