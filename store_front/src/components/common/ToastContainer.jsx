import { X } from 'lucide-react';
import { useToast } from '../../context/ToastContext';

function toastStyles(type) {
  switch (type) {
    case 'success':
      return {
        wrapper: 'border-green-100 bg-green-50 text-green-900',
        title: 'text-green-900',
        message: 'text-green-800',
        bar: 'bg-green-500',
      };
    case 'error':
      return {
        wrapper: 'border-red-100 bg-red-50 text-red-900',
        title: 'text-red-900',
        message: 'text-red-800',
        bar: 'bg-red-500',
      };
    case 'info':
    default:
      return {
        wrapper: 'border-blue-100 bg-blue-50 text-blue-900',
        title: 'text-blue-900',
        message: 'text-blue-800',
        bar: 'bg-blue-500',
      };
  }
}

function ToastContainer() {
  const { toasts, removeToast } = useToast();

  if (!toasts || toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-[9999] w-[calc(100%-2rem)] max-w-sm space-y-3">
      {toasts.map((t) => {
        const s = toastStyles(t.type);
        return (
          <div
            key={t.id}
            className={`relative overflow-hidden border rounded-xl shadow-sm p-4 ${s.wrapper}`}
            role="status"
            aria-live="polite"
          >
            <div className="flex items-start gap-3">
              <div className="flex-1 min-w-0">
                <p className={`font-semibold ${s.title}`}>{t.title}</p>
                {t.message && <p className={`text-sm mt-1 ${s.message}`}>{t.message}</p>}
              </div>
              <button
                onClick={() => removeToast(t.id)}
                className="p-1 rounded-md hover:bg-white/50 transition-colors"
                aria-label="Close toast"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className={`absolute bottom-0 left-0 h-1 w-full ${s.bar} opacity-70`} />
          </div>
        );
      })}
    </div>
  );
}

export default ToastContainer;