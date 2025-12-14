import { useState } from "react";
import { useTranslation } from "react-i18next";
import { MessageSquare, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/components/ui/dialog";
import { Button } from "@/shared/components/ui/button";
import { Textarea } from "@/shared/components/ui/textarea";
import { Label } from "@/shared/components/ui/label";
import { Checkbox } from "@/shared/components/ui/checkbox";
import { Alert, AlertDescription } from "@/shared/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import { submitFeedback } from "@/shared/api/feedbackApi";
import type { FeedbackType } from "@/shared/api/feedbackApi";

interface FeedbackModalProps {
  trigger?: React.ReactNode;
}

export function FeedbackModal({ trigger }: FeedbackModalProps) {
  const { t } = useTranslation("feedback");

  const [open, setOpen] = useState(false);
  const [type, setType] = useState<FeedbackType | "">("");
  const [message, setMessage] = useState("");
  const [anonymous, setAnonymous] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<{ type?: string; message?: string }>({});

  const validate = (): boolean => {
    const newErrors: { type?: string; message?: string } = {};

    if (!type) {
      newErrors.type = t("validation.typeRequired");
    }

    if (!message.trim()) {
      newErrors.message = t("validation.messageRequired");
    } else if (message.trim().length < 10) {
      newErrors.message = t("validation.messageMinLength");
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await submitFeedback({
        type: type as FeedbackType,
        message: message.trim(),
        anonymous,
      });

      toast.success(t("success.title"), {
        description: t("success.description"),
      });

      // Reset form and close modal
      setType("");
      setMessage("");
      setAnonymous(false);
      setErrors({});
      setOpen(false);
    } catch {
      toast.error(t("error.title"), {
        description: t("error.description"),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      // Reset form when closing
      setType("");
      setMessage("");
      setAnonymous(false);
      setErrors({});
    }
    setOpen(newOpen);
  };

  const defaultTrigger = (
    <Button variant="ghost" size="sm" className="gap-2">
      <MessageSquare className="h-4 w-4" />
      <span>{t("button")}</span>
    </Button>
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{trigger ?? defaultTrigger}</DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{t("title")}</DialogTitle>
          <DialogDescription>{t("description")}</DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          {/* Feedback Type Select */}
          <div className="grid gap-2">
            <Label htmlFor="feedback-type">{t("type.label")}</Label>
            <Select
              value={type}
              onValueChange={(value) => {
                setType(value as FeedbackType);
                if (errors.type) setErrors((prev) => ({ ...prev, type: undefined }));
              }}
            >
              <SelectTrigger id="feedback-type" className={errors.type ? "border-destructive" : ""}>
                <SelectValue placeholder={t("type.placeholder")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="bug">{t("type.bug")}</SelectItem>
                <SelectItem value="feature">{t("type.feature")}</SelectItem>
              </SelectContent>
            </Select>
            {errors.type && <p className="text-sm text-destructive">{errors.type}</p>}
          </div>

          {/* Message Textarea */}
          <div className="grid gap-2">
            <Label htmlFor="feedback-message">{t("message.label")}</Label>
            <Textarea
              id="feedback-message"
              placeholder={t("message.placeholder")}
              value={message}
              onChange={(e) => {
                setMessage(e.target.value);
                if (errors.message) setErrors((prev) => ({ ...prev, message: undefined }));
              }}
              className={`min-h-[120px] ${errors.message ? "border-destructive" : ""}`}
            />
            {errors.message && <p className="text-sm text-destructive">{errors.message}</p>}
          </div>

          {/* Privacy Notice */}
          <Alert variant="default" className="border-amber-500/50 bg-amber-500/10 dark:border-amber-400/30 dark:bg-amber-400/10">
            <AlertTriangle className="h-4 w-4 text-amber-600 dark:text-amber-400" />
            <AlertDescription className="text-amber-800 dark:text-amber-200 text-sm">
              {t("privacy.notice")}
            </AlertDescription>
          </Alert>

          {/* Anonymous Checkbox */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="anonymous"
              checked={anonymous}
              onCheckedChange={(checked) => setAnonymous(checked === true)}
            />
            <Label htmlFor="anonymous" className="text-sm font-normal cursor-pointer">
              {t("privacy.anonymous")}
            </Label>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={isSubmitting}>
            {t("cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? t("submitting") : t("submit")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
