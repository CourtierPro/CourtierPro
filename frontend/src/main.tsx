// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Auth0Provider } from "@auth0/auth0-react";

import App from "./App";
import "./index.css";
import "./i18n/i18n";

// ⚠️ mets bien TES valeurs ici (celles qu’on a déjà utilisées)
const domain = "dev-y7mhv7ttykx4kz4f.us.auth0.com";
const clientId = "JJIaYMFFtbZqGwP1XKyOx1XcFTPO9Qlr";
const audience = "https://api.courtierpro.dev";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
        <BrowserRouter>
            <Auth0Provider
                domain={domain}
                clientId={clientId}
                authorizationParams={{
                    audience,
                    redirect_uri: window.location.origin,
                }}
            >
                <App />
            </Auth0Provider>
        </BrowserRouter>
    </React.StrictMode>
);
