import React from "react";
import type { ReactNode, ErrorInfo } from "react";
import { AlertTriangle } from "lucide-react";

import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { ScrollArea } from "@/shared/components/ui/scroll-area";
import { logError } from "@/shared/utils/error-utils";

interface ErrorBoundaryProps {
  children: ReactNode;
  /**
   * Optional custom fallback to render when an error occurs.
   * If provided, this completely replaces the default UI.
   */
  fallback?: ReactNode;
  /**
   * Optional callback invoked when an error is caught.
   * Useful to report errors to an external service.
   */
  onError?(error: Error, info: ErrorInfo): void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

const isDev = import.meta.env.DEV;

/**
 * ErrorBoundary catches render-time errors in its child component tree
 * and renders a safe fallback UI instead of crashing the entire app.
 *
 * Use this around large sections (dashboards, complex widgets) and/or
 * at the app-shell level for global protection.
 */
export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public state: ErrorBoundaryState = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, info: ErrorInfo): void {
    // Log via our centralized logger
    logError(error);

    // Allow callers to hook into error reporting
    if (this.props.onError) {
      this.props.onError(error, info);
    }
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  private handleGoBack = () => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      this.handleReset();
    }
  };

  public render(): ReactNode {
    if (!this.state.hasError) {
      return this.props.children;
    }

    // Custom fallback completely overrides the default UI
    if (this.props.fallback) {
      return this.props.fallback;
    }

    const { error } = this.state;

    return (
      <div className="flex min-h-[50vh] items-center justify-center px-4 py-8">
        <Card className="max-w-xl w-full">
          <CardHeader className="flex flex-row items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10">
              <AlertTriangle className="h-5 w-5 text-destructive" />
            </div>
            <div>
              <CardTitle>Something went wrong</CardTitle>
              <CardDescription>
                An unexpected error occurred while rendering this part of the app.
              </CardDescription>
            </div>
          </CardHeader>

          {isDev && error && (
            <CardContent>
              <div className="mb-2 text-sm font-medium text-muted-foreground">
                Developer details (visible in development only):
              </div>
              <ScrollArea className="h-[200px] w-full rounded-md border bg-muted p-4">
                <pre className="text-xs font-mono break-words whitespace-pre-wrap">
                  {error.name}: {error.message}
                  {error.stack ? `\n\n${error.stack}` : null}
                </pre>
              </ScrollArea>
            </CardContent>
          )}

          <CardFooter className="flex flex-wrap items-center justify-between gap-2">
            <div className="text-xs text-muted-foreground">
              You can try again, go back, or refresh the page. If the problem continues, please contact support.
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={this.handleGoBack}>
                Go back
              </Button>
              <Button size="sm" onClick={this.handleReset}>
                Try again
              </Button>
            </div>
          </CardFooter>
        </Card>
      </div>
    );
  }
}
