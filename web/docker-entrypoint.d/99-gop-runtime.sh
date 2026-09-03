#!/bin/sh
set -e
API_URL="${GOP_API_URL:-}"
cat > /usr/share/nginx/html/runtime-config.js <<EOF
window.__GOP_CONFIG__ = { apiBaseUrl: "${API_URL}" };
EOF
