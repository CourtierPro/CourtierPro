import { useEffect } from 'react';
import { CheckCircle, X } from 'lucide-react';
import { Button } from "@/shared/components/ui/button";

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
      <Button
        variant="ghost"
        size="icon"
        onClick={onClose}
        aria-label="Close notification"
        className="text-slate-600"
      >
        <X className="h-4 w-4" />
      </Button>
    </div>
  );
}
