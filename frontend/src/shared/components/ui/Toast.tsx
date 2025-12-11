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
      className="fixed top-4 right-4 z-50 flex items-center gap-3 p-4 rounded-lg shadow-lg animate-slide-in-right bg-background border-2 border-emerald-500"
      role="alert"
      aria-live="polite"
      aria-atomic="true"
    >
      <CheckCircle className="w-5 h-5 flex-shrink-0 text-emerald-500" />
      <p className="text-foreground">{message}</p>
      <button
        onClick={onClose}
        className="p-1 rounded-lg hover:bg-muted focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors"
        aria-label="Close notification"
      >
        <X className="w-4 h-4 text-foreground" />
      </button>
    </div>
  );
}
