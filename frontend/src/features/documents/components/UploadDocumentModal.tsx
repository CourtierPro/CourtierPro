import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from "react-i18next";
import { useDropzone, type FileRejection } from 'react-dropzone';
import { Button } from "@/shared/components/ui/button";
import { Progress } from "@/shared/components/ui/progress";
import { useSubmitDocument } from "@/features/documents/api/mutations";
import { Loader2, Upload, X, FileText } from "lucide-react";
import { toast } from "sonner";
import { useErrorHandler } from "@/shared/hooks/useErrorHandler";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";

interface UploadDocumentModalProps {
  open: boolean;
  onClose: () => void;
  requestId: string;
  transactionId: string;
  documentTitle: string;
  docType?: string;
  initialFile?: File | null;
  onSuccess: () => void;
}

export function UploadDocumentModal({
  open,
  onClose,
  requestId,
  transactionId,
  documentTitle,
  docType,
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

  // Get translated document type or use the title
  const displayTitle = docType ? t(`types.${docType}`) : documentTitle;

  // Simulate progress when uploading starts
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (isUploading) {
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
    onDropRejected: (fileRejections: FileRejection[]) => {
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

  const handleSubmit = async () => {
    if (!file) return;

    setError(null);
    setUploadProgress(0);

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

  const handleCloseAttempt = () => {
    if (!isUploading) {
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={(val) => !val && handleCloseAttempt()}>
      <DialogContent className="sm:max-w-md [&>button]:hidden">
        <DialogHeader className="flex-row items-center justify-between space-y-0 text-left mb-4">
          <DialogTitle className="text-xl font-semibold text-foreground">
            {t("modals.uploadDocument")}
          </DialogTitle>
          <Button variant="ghost" size="icon" onClick={handleCloseAttempt} disabled={isUploading}>
            <X className="w-5 h-5" />
          </Button>
        </DialogHeader>

        <p className="text-sm text-muted-foreground mb-4">
          {t("uploadingFor")}: <span className="font-medium text-foreground">{displayTitle}</span>
        </p>

        <div className="space-y-4">
          {!file ? (
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors cursor-pointer flex flex-col items-center gap-3
                ${isDragReject ? 'border-destructive bg-destructive/10' : isDragActive ? 'border-primary bg-primary/10' : 'border-border hover:bg-muted/50'}`}
            >
              <input {...getInputProps()} />
              <div className={`p-3 rounded-full ${isDragReject ? 'bg-destructive/10' : 'bg-muted'}`}>
                {isDragReject ? (
                  <X className="w-6 h-6 text-destructive" />
                ) : (
                  <Upload className="w-6 h-6 text-muted-foreground" />
                )}
              </div>
              <div>
                <span className={`text-sm font-medium block ${isDragReject ? 'text-destructive' : 'text-foreground'}`}>
                  {isDragReject ? t("errors.invalidFileType", "Invalid file type") : (isDragActive ? t("dropHere") : t("dragAndDropOrClick"))}
                </span>
                <span className="text-xs text-muted-foreground">
                  {t("supportedFormats")} (PDF, JPG, PNG) • {t("maxSize", { size: "25MB" })}
                </span>
              </div>
            </div>
          ) : (
            <div className="border border-border rounded-lg p-4 relative">
              {!isUploading && (
                <button
                  onClick={removeFile}
                  className="absolute top-2 right-2 p-1 hover:bg-muted rounded-full text-muted-foreground"
                >
                  <X className="w-4 h-4" />
                </button>
              )}

              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary/10 rounded-lg">
                  <FileText className="w-8 h-8 text-primary" />
                </div>
                <div className="flex-1 overflow-hidden">
                  <p className="text-sm font-medium text-foreground truncate">{file.name}</p>
                  <p className="text-xs text-muted-foreground">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                </div>
              </div>

              {isUploading && (
                <div className="mt-4 space-y-1">
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>{t("uploading")}...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <Progress value={uploadProgress} className="h-2" />
                </div>
              )}
            </div>
          )}

          {error && (
            <div className="text-sm text-destructive bg-destructive/10 p-2 rounded flex items-center gap-2">
              <span className="block w-1.5 h-1.5 rounded-full bg-destructive" />
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={handleCloseAttempt} disabled={isUploading}>
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
      </DialogContent>
    </Dialog>
  );
}
