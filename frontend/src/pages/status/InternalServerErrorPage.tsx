import { Link } from 'react-router-dom';

export function InternalServerErrorPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center space-y-3">
      <h1 className="text-3xl font-semibold tracking-tight">
        500 – Internal server error
      </h1>
      <p className="max-w-md text-sm text-muted-foreground">
        This is the page shown when an unexpected error occurs on the server.
      </p>
      <Link
        to="/"
        className="mt-2 text-sm font-medium text-orange-600 hover:underline"
      >
        Go back home
      </Link>
    </div>
  );
}
