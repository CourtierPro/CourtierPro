#!/bin/sh

# Generate config.js from environment variables
cat <<EOF > /usr/share/nginx/html/config.js
window.env = {
  VITE_API_URL: "${VITE_API_URL}"
};
EOF

# Start Nginx
exec nginx -g "daemon off;"
