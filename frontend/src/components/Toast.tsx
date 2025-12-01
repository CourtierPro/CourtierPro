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
      className="fixed top-4 right-4 z-50 flex items-center gap-3 p-4 rounded-lg shadow-lg animate-slide-in-right"
      style={{ backgroundColor: '#FFFFFF', border: '2px solid #10b981' }}
      role="alert"
      aria-live="polite"
      aria-atomic="true"
    >
      <CheckCircle className="w-5 h-5 flex-shrink-0" style={{ color: '#10b981' }} />
      <p style={{ color: '#353535' }}>{message}</p>
      <button
        onClick={onClose}
        className="p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#10b981] transition-colors"
        aria-label="Close notification"
      >
        <X className="w-4 h-4" style={{ color: '#353535' }} />
      </button>
    </div>
  );
}
