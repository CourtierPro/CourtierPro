#!/bin/sh
set -e

# Escape special characters in VITE_API_URL to prevent XSS/injection
# We escape backslashes first, then double quotes.
SANITIZED_URL=$(printf '%s' "$VITE_API_URL" | sed 's/\\/\\\\/g; s/"/\\"/g')

# Generate config.js from environment variables
cat <<EOF > /usr/share/nginx/html/config.js
window.env = {
  VITE_API_URL: "${SANITIZED_URL}"
};
EOF

# Start Nginx
exec nginx -g "daemon off;"
