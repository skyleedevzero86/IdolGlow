#!/bin/sh
set -e

cd /app
node node_modules/next/dist/bin/next start -H "${HOSTNAME:-127.0.0.1}" -p "${PORT:-3000}" &
NEXT_PID=$!

trap 'kill "$NEXT_PID" 2>/dev/null; nginx -s quit 2>/dev/null' TERM INT

nginx -g 'daemon off;' &
NGINX_PID=$!

wait "$NEXT_PID" "$NGINX_PID"
