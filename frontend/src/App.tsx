import { useAuth0 } from "@auth0/auth0-react";
import { AppRoutes } from "@/app/routes/AppRoutes";

// Check if we're in the middle of an Auth0 callback (URL has code and state params)
function isAuth0Callback(): boolean {
  const params = new URLSearchParams(window.location.search);
  return params.has("code") && params.has("state");
}

export default function App() {
  const { isLoading } = useAuth0();
  const isProcessingCallback = isAuth0Callback();

  if (isLoading || isProcessingCallback) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return <AppRoutes />;
}

