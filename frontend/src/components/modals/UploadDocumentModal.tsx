interface UploadDocumentModalProps {
  open: boolean;
  onClose: () => void;
}

export function UploadDocumentModal({
  open,
  onClose,
}: UploadDocumentModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">Upload Document</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for uploading documents to a transaction (placeholder only).
        </p>
        {/* TODO: Implement document upload UI and behavior */}
        <button
          className="mt-4 text-sm text-orange-600 hover:underline"
          onClick={onClose}
        >
          Close
        </button>
      </div>
    </div>
  );
}
