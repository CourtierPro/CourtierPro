import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from "react-i18next";
import { useDropzone } from 'react-dropzone';
import { Button } from "@/shared/components/ui/button";
import { Progress } from "@/shared/components/ui/progress";
import { useSubmitDocument } from "@/features/documents/api/mutations";
import { Loader2, Upload, X, FileText } from "lucide-react";
import { toast } from "sonner";
import { useErrorHandler } from "@/shared/hooks/useErrorHandler";

interface UploadDocumentModalProps {
  open: boolean;
  onClose: () => void;
  requestId: string;
  transactionId: string;
  documentTitle: string;
  initialFile?: File | null;
  onSuccess: () => void;
}

export function UploadDocumentModal({
  open,
  onClose,
  requestId,
  transactionId,
  documentTitle,
  initialFile,
  onSuccess,
}: UploadDocumentModalProps) {
  const { t } = useTranslation("documents");
  const [file, setFile] = useState<File | null>(initialFile || null);
  const [error, setError] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);

  const submitDocument = useSubmitDocument();
  const isUploading = submitDocument.isPending;
  const { handleError } = useErrorHandler();

  // Simulate progress when uploading starts
  useEffect(() => {
    let interval: any;
    if (isUploading) {
      setUploadProgress(0);
      interval = setInterval(() => {
        setUploadProgress((prev) => (prev >= 90 ? 90 : prev + 10));
      }, 500);
    }
    return () => clearInterval(interval);
  }, [isUploading]);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const selectedFile = acceptedFiles[0];
    if (selectedFile) {
      if (selectedFile.size > 25 * 1024 * 1024) {
        setError(t("errors.fileTooLarge"));
        setFile(null);
      } else {
        setError(null);
        setFile(selectedFile);
      }
    }
  }, [t]);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    onDropRejected: (fileRejections) => {
      const rejection = fileRejections[0];
      if (rejection.errors[0].code === 'file-invalid-type') {
        setError(t("errors.invalidFileType", "Invalid file type. Please upload PDF, JPG, or PNG."));
      } else {
        setError(rejection.errors[0].message);
      }
    },
    accept: {
      'application/pdf': ['.pdf'],
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png']
    },
    maxFiles: 1,
    multiple: false
  });

  if (!open) return null;

  const handleSubmit = async () => {
    if (!file) return;

    setError(null);

    try {
      await submitDocument.mutateAsync({ transactionId, requestId, file });
      setUploadProgress(100);
      toast.success(t("success.documentUploaded"));

      // Delay closing to show 100%
      setTimeout(() => {
        onSuccess();
        setFile(null);
        setUploadProgress(0);
      }, 500);
    } catch (err) {
      handleError(err);
      setError(t("errors.uploadFailed"));
      toast.error(t("errors.uploadFailed"));
      setUploadProgress(0);
    }
  };

  const removeFile = () => {
    setFile(null);
    setError(null);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">{t("modals.uploadDocument")}</h2>
          <Button variant="ghost" size="icon" onClick={onClose} disabled={isUploading}>
            <X className="w-5 h-5" />
          </Button>
        </div>

        <p className="text-sm text-gray-600 mb-4">
          {t("uploadingFor")}: <span className="font-medium">{documentTitle}</span>
        </p>

        <div className="space-y-4">
          {!file ? (
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors cursor-pointer flex flex-col items-center gap-3
                ${isDragReject ? 'border-red-500 bg-red-50' : isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:bg-gray-50'}`}
            >
              <input {...getInputProps()} />
              <div className={`p-3 rounded-full ${isDragReject ? 'bg-red-100' : 'bg-gray-100'}`}>
                {isDragReject ? (
                  <X className="w-6 h-6 text-red-500" />
                ) : (
                  <Upload className="w-6 h-6 text-gray-500" />
                )}
              </div>
              <div>
                <span className={`text-sm font-medium block ${isDragReject ? 'text-red-700' : 'text-gray-700'}`}>
                  {isDragReject ? t("errors.invalidFileType", "Invalid file type") : (isDragActive ? t("dropHere") : t("dragAndDropOrClick"))}
                </span>
                <span className="text-xs text-gray-500">
                  {t("supportedFormats")} (PDF, JPG, PNG) • {t("maxSize", { size: "25MB" })}
                </span>
              </div>
            </div>
          ) : (
            <div className="border border-gray-200 rounded-lg p-4 relative">
              {!isUploading && (
                <button
                  onClick={removeFile}
                  className="absolute top-2 right-2 p-1 hover:bg-gray-100 rounded-full text-gray-500"
                >
                  <X className="w-4 h-4" />
                </button>
              )}

              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-50 rounded-lg">
                  <FileText className="w-8 h-8 text-blue-500" />
                </div>
                <div className="flex-1 overflow-hidden">
                  <p className="text-sm font-medium text-gray-900 truncate">{file.name}</p>
                  <p className="text-xs text-gray-500">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                </div>
              </div>

              {isUploading && (
                <div className="mt-4 space-y-1">
                  <div className="flex justify-between text-xs text-gray-500">
                    <span>{t("uploading")}...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <Progress value={uploadProgress} className="h-2" />
                </div>
              )}
            </div>
          )}

          {error && (
            <div className="text-sm text-red-500 bg-red-50 p-2 rounded flex items-center gap-2">
              <span className="block w-1.5 h-1.5 rounded-full bg-red-500" />
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={onClose} disabled={isUploading}>
              {t("actions.cancel")}
            </Button>
            <Button onClick={handleSubmit} disabled={!file || isUploading}>
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  {t("actions.uploading")}
                </>
              ) : (
                t("actions.upload")
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
