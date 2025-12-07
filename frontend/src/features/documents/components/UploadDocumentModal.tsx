import { useState } from 'react';
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { useSubmitDocument } from "../api/mutations";
import { Loader2, Upload, X } from "lucide-react";
import { toast } from "sonner";
import { useErrorHandler } from "@/shared/hooks/useErrorHandler";

interface UploadDocumentModalProps {
  open: boolean;
  onClose: () => void;
  requestId: string;
  transactionId: string;
  documentTitle: string;
  onSuccess: () => void;
}

export function UploadDocumentModal({
  open,
  onClose,
  requestId,
  transactionId,
  documentTitle,
  onSuccess,
}: UploadDocumentModalProps) {
  const { t } = useTranslation("documents");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submitDocument = useSubmitDocument();
  const isUploading = submitDocument.isPending;
  const { handleError } = useErrorHandler();

  if (!open) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      if (selectedFile.size > 25 * 1024 * 1024) {
        setError(t("errors.fileTooLarge"));
        setFile(null);
      } else {
        setError(null);
        setFile(selectedFile);
      }
    }
  };

  const handleSubmit = async () => {
    if (!file) return;

    setError(null);

    try {
      await submitDocument.mutateAsync({ transactionId, requestId, file });
      toast.success(t("success.documentUploaded"));
      onSuccess();
    } catch (err) {
      handleError(err);
      setError(t("errors.uploadFailed"));
      toast.error(t("errors.uploadFailed"));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">{t("modals.uploadDocument")}</h2>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
          </Button>
        </div>

        <p className="text-sm text-gray-600 mb-4">
          {t("uploadingFor")}: <span className="font-medium">{documentTitle}</span>
        </p>

        <div className="space-y-4">
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:bg-gray-50 transition-colors">
            <input
              type="file"
              id="file-upload"
              className="hidden"
              onChange={handleFileChange}
              accept=".pdf,.jpg,.jpeg,.png"
            />
            <label
              htmlFor="file-upload"
              className="cursor-pointer flex flex-col items-center gap-2"
            >
              <Upload className="w-8 h-8 text-gray-400" />
              <span className="text-sm font-medium text-gray-700">
                {file ? file.name : t("clickToSelectFile")}
              </span>
              <span className="text-xs text-gray-500">
                {t("maxSize", { size: "25MB" })}
              </span>
            </label>
          </div>

          {error && (
            <div className="text-sm text-red-500 bg-red-50 p-2 rounded">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3">
            <Button variant="outline" onClick={onClose} disabled={isUploading}>
              {t("actions.cancel")}
            </Button>
            <Button onClick={handleSubmit} disabled={!file || isUploading}>
              {isUploading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              {t("actions.upload")}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
