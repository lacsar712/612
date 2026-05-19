#!/bin/sh
set -e

API_BASE=${API_BASE:-/api-7f3b2}
cat <<CONFIG > /usr/share/nginx/html/config.js
window.__API_BASE__ = '${API_BASE}';
CONFIG

exec nginx -g 'daemon off;'
