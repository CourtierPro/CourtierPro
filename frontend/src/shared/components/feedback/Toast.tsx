import { useEffect } from 'react';
import { CheckCircle, X } from 'lucide-react';

interface ToastProps {
  message: string;
  onClose: () => void;
  duration?: number;
}

export function Toast({ message, onClose, duration = 3000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  return (
    <div
      className="fixed top-4 right-4 z-50 flex items-center gap-3 rounded-lg border-2 border-emerald-500 bg-white p-4 shadow-lg"
      role="alert"
      aria-live="polite"
      aria-atomic="true"
    >
      <CheckCircle className="h-5 w-5 shrink-0 text-emerald-500" />
      <p className="text-sm text-slate-800">{message}</p>
      <button
        onClick={onClose}
        className="rounded-lg p-1 text-slate-600 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
        aria-label="Close notification"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
