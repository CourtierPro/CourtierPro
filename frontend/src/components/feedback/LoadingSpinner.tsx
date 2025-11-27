interface LoadingSpinnerProps {
  /** Optional message shown under the spinner, e.g. "Loading transactions..." */
  message?: string;
  /** If true, covers the screen with a centered spinner */
  fullscreen?: boolean;
  /** Spinner size */
  size?: 'sm' | 'md' | 'lg';
}

export function LoadingSpinner({
  message,
  fullscreen = false,
  size = 'md',
}: LoadingSpinnerProps) {
  const sizeClasses =
    size === 'sm'
      ? 'h-4 w-4 border-2'
      : size === 'lg'
        ? 'h-12 w-12 border-4'
        : 'h-8 w-8 border-[3px]';

  const content = (
    <div className="flex flex-col items-center justify-center gap-2" role="status">
      <div
        className={`animate-spin rounded-full border-solid border-orange-500 border-t-transparent ${sizeClasses}`}
      />
      {message ? (
        <p className="text-sm text-muted-foreground">{message}</p>
      ) : (
        <span className="sr-only">Loading…</span>
      )}
    </div>
  );

  if (fullscreen) {
    return (
      <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/20">
        {content}
      </div>
    );
  }

  return content;
}
