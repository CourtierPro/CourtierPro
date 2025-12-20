// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";

import App from "@/App";
import "./index.css";
import "@/shared/i18n/i18n";
import { AppProviders } from "@/app/providers/AppProviders";

// Check for runtime configuration
if (!window.env) {
    console.warn("Runtime configuration (window.env) is missing. Falling back to build-time configuration.");
}
const apiUrl = window.env?.VITE_API_URL || import.meta.env.VITE_API_URL;

if (!apiUrl) {
    console.error("Critical: API URL configuration is missing.");
    document.body.innerHTML = `
        <div style="display:flex;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;background-color:#f8d7da;color:#721c24;">
            <div style="text-align:center;">
                <h1>Configuration Error</h1>
                <p>The application could not load its configuration.</p>
                <small>Missing VITE_API_URL</small>
            </div>
        </div>
    `;
} else {
    ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
        <React.StrictMode>
            <AppProviders>
                <App />
            </AppProviders>
        </React.StrictMode>
    );
}
