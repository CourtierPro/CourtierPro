#!/bin/sh
set -e

# Escape special characters to prevent XSS/injection
SANITIZED_API_URL=$(printf '%s' "$VITE_API_URL" | sed 's/\\/\\\\/g; s/"/\\"/g')
SANITIZED_AUTH0_DOMAIN=$(printf '%s' "$VITE_AUTH0_DOMAIN" | sed 's/\\/\\\\/g; s/"/\\"/g')
SANITIZED_AUTH0_CLIENT_ID=$(printf '%s' "$VITE_AUTH0_CLIENT_ID" | sed 's/\\/\\\\/g; s/"/\\"/g')
SANITIZED_AUTH0_AUDIENCE=$(printf '%s' "$VITE_AUTH0_AUDIENCE" | sed 's/\\/\\\\/g; s/"/\\"/g')

# Generate config.js from environment variables
cat <<EOF > /usr/share/nginx/html/config.js
window.env = {
  VITE_API_URL: "${SANITIZED_API_URL}",
  VITE_AUTH0_DOMAIN: "${SANITIZED_AUTH0_DOMAIN}",
  VITE_AUTH0_CLIENT_ID: "${SANITIZED_AUTH0_CLIENT_ID}",
  VITE_AUTH0_AUDIENCE: "${SANITIZED_AUTH0_AUDIENCE}"
};
EOF

# Start Nginx
exec nginx -g "daemon off;"