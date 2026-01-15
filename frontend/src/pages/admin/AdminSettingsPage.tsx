// src/pages/admin/AdminSettingsPage.tsx
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Globe, Mail } from "lucide-react";

import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";

import {
  getOrganizationSettings,
  updateOrganizationSettings,
} from "./adminSettingsApi";

import type { UpdateOrganizationSettingsRequest } from "./adminSettingsApi";

type EmailTemplateType = 
  | "invite" 
  | "documentSubmitted" 
  | "documentRequested" 
  | "documentReview" 
  | "stageUpdate";

const EMAIL_TEMPLATES: Record<EmailTemplateType, { label: string; variables: string }> = {
  invite: {
    label: "Invite / Password Setup",
    variables: "{{name}}",
  },
  documentSubmitted: {
    label: "Document Submitted",
    variables: "{{uploaderName}}, {{documentName}}, {{documentType}}, {{transactionId}}",
  },
  documentRequested: {
    label: "Document Requested",
    variables: "{{clientName}}, {{brokerName}}, {{documentName}}, {{documentType}}",
  },
  documentReview: {
    label: "Document Review",
    variables: "{{brokerName}}, {{documentName}}, {{documentType}}, {{transactionId}}, {{status}}, {{brokerNotes}}",
  },
  stageUpdate: {
    label: "Stage Update",
    variables: "{{clientName}}, {{brokerName}}, {{transactionAddress}}, {{newStage}}",
  },
};

export function AdminSettingsPage() {
  const { t } = useTranslation("admin");

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [form, setForm] = useState<UpdateOrganizationSettingsRequest | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<EmailTemplateType>("invite");

  // Load settings
  useEffect(() => {
    async function load() {
      try {
        setIsLoading(true);
        const data = await getOrganizationSettings();

        setForm({
          defaultLanguage: data.defaultLanguage,
          inviteSubjectEn: data.inviteSubjectEn,
          inviteBodyEn: data.inviteBodyEn,
          inviteSubjectFr: data.inviteSubjectFr,
          inviteBodyFr: data.inviteBodyFr,
          documentSubmittedSubjectEn: data.documentSubmittedSubjectEn,
          documentSubmittedBodyEn: data.documentSubmittedBodyEn,
          documentSubmittedSubjectFr: data.documentSubmittedSubjectFr,
          documentSubmittedBodyFr: data.documentSubmittedBodyFr,
          documentRequestedSubjectEn: data.documentRequestedSubjectEn,
          documentRequestedBodyEn: data.documentRequestedBodyEn,
          documentRequestedSubjectFr: data.documentRequestedSubjectFr,
          documentRequestedBodyFr: data.documentRequestedBodyFr,
          documentReviewSubjectEn: data.documentReviewSubjectEn,
          documentReviewBodyEn: data.documentReviewBodyEn,
          documentReviewSubjectFr: data.documentReviewSubjectFr,
          documentReviewBodyFr: data.documentReviewBodyFr,
          stageUpdateSubjectEn: data.stageUpdateSubjectEn,
          stageUpdateBodyEn: data.stageUpdateBodyEn,
          stageUpdateSubjectFr: data.stageUpdateSubjectFr,
          stageUpdateBodyFr: data.stageUpdateBodyFr,
        });
      } catch {
        toast.error(t("settings.errors.loadFailed"));
        setError(t("settings.errors.loadFailed"));
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, [t]);

  // Handle form changes
  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    const field = e.target.name;
    const value = e.target.value;

    setForm((prev) => {
      if (!prev) return prev;
      return { ...prev, [field]: value };
    });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;

    try {
      setIsSaving(true);
      setError(null);
      setSaveMessage(null);

      const updated = await updateOrganizationSettings(form);

      setForm({
        defaultLanguage: updated.defaultLanguage,
        inviteSubjectEn: updated.inviteSubjectEn,
        inviteBodyEn: updated.inviteBodyEn,
        inviteSubjectFr: updated.inviteSubjectFr,
        inviteBodyFr: updated.inviteBodyFr,
        documentSubmittedSubjectEn: updated.documentSubmittedSubjectEn,
        documentSubmittedBodyEn: updated.documentSubmittedBodyEn,
        documentSubmittedSubjectFr: updated.documentSubmittedSubjectFr,
        documentSubmittedBodyFr: updated.documentSubmittedBodyFr,
        documentRequestedSubjectEn: updated.documentRequestedSubjectEn,
        documentRequestedBodyEn: updated.documentRequestedBodyEn,
        documentRequestedSubjectFr: updated.documentRequestedSubjectFr,
        documentRequestedBodyFr: updated.documentRequestedBodyFr,
        documentReviewSubjectEn: updated.documentReviewSubjectEn,
        documentReviewBodyEn: updated.documentReviewBodyEn,
        documentReviewSubjectFr: updated.documentReviewSubjectFr,
        documentReviewBodyFr: updated.documentReviewBodyFr,
        stageUpdateSubjectEn: updated.stageUpdateSubjectEn,
        stageUpdateBodyEn: updated.stageUpdateBodyEn,
        stageUpdateSubjectFr: updated.stageUpdateSubjectFr,
        stageUpdateBodyFr: updated.stageUpdateBodyFr,
      });

      setSaveMessage(t("settings.messages.saved"));
    } catch {
      toast.error(t("settings.errors.saveFailed"));
      setError(t("settings.errors.saveFailed"));
    } finally {
      setIsSaving(false);
    }
  }

  const isDisabled = isSaving || isLoading || !form;

  const getSubjectFieldName = (type: EmailTemplateType, lang: "En" | "Fr"): keyof UpdateOrganizationSettingsRequest => {
    const fieldMap: Record<EmailTemplateType, Record<"En" | "Fr", keyof UpdateOrganizationSettingsRequest>> = {
      invite: { En: "inviteSubjectEn", Fr: "inviteSubjectFr" },
      documentSubmitted: { En: "documentSubmittedSubjectEn", Fr: "documentSubmittedSubjectFr" },
      documentRequested: { En: "documentRequestedSubjectEn", Fr: "documentRequestedSubjectFr" },
      documentReview: { En: "documentReviewSubjectEn", Fr: "documentReviewSubjectFr" },
      stageUpdate: { En: "stageUpdateSubjectEn", Fr: "stageUpdateSubjectFr" },
    };
    return fieldMap[type][lang];
  };

  const getBodyFieldName = (type: EmailTemplateType, lang: "En" | "Fr"): keyof UpdateOrganizationSettingsRequest => {
    const fieldMap: Record<EmailTemplateType, Record<"En" | "Fr", keyof UpdateOrganizationSettingsRequest>> = {
      invite: { En: "inviteBodyEn", Fr: "inviteBodyFr" },
      documentSubmitted: { En: "documentSubmittedBodyEn", Fr: "documentSubmittedBodyFr" },
      documentRequested: { En: "documentRequestedBodyEn", Fr: "documentRequestedBodyFr" },
      documentReview: { En: "documentReviewBodyEn", Fr: "documentReviewBodyFr" },
      stageUpdate: { En: "stageUpdateBodyEn", Fr: "stageUpdateBodyFr" },
    };
    return fieldMap[type][lang];
  };

  const renderTemplateForm = () => {
    const subjectEnField = getSubjectFieldName(selectedTemplate, "En");
    const bodyEnField = getBodyFieldName(selectedTemplate, "En");
    const subjectFrField = getSubjectFieldName(selectedTemplate, "Fr");
    const bodyFrField = getBodyFieldName(selectedTemplate, "Fr");

    // Convert plain text to HTML for preview
    const convertTextToHtml = (text: string) => {
      if (!text) return "";
      // Escape HTML
      let escaped = text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
      
      // Convert double newlines to paragraph breaks
      let html = escaped.replace(/\n\n/g, "</p><p>");
      
      // Convert single newlines to line breaks
      html = html.replace(/\n/g, "<br>");
      
      // Wrap in paragraph tags
      return "<p>" + html + "</p>";
    };

    const renderPreview = (subjectField: string, bodyField: string) => {
      const subject = (form?.[subjectField] as string) ?? "";
      const body = (form?.[bodyField] as string) ?? "";
      const htmlBody = convertTextToHtml(body);

      return (
        <div className="border border-border rounded-lg p-6 bg-white dark:bg-slate-950 space-y-4">
          {/* Subject Preview */}
          <div>
            <h3 className="text-2xl font-bold text-foreground mb-4">{subject || "(No subject)"}</h3>
          </div>

          {/* Body Preview */}
          <div 
            className="text-sm text-foreground/80 leading-relaxed max-h-96 overflow-y-auto prose prose-sm dark:prose-invert max-w-none"
            dangerouslySetInnerHTML={{
              __html: htmlBody || "<p style='color: #999;'>(Empty body)</p>",
            }}
          />

          {/* Footer Preview */}
          <div className="border-t border-gray-300 pt-4 mt-4">
            <hr style={{ border: "none", borderTop: "1px solid #e0e0e0", margin: "0" }} />
            <p style={{ color: "#666", fontSize: "12px", lineHeight: "1.6", marginTop: "15px" }}>
              Merci,<br />
              Cordialement,<br />
              <strong>Équipe CourtierPro</strong>
            </p>
          </div>
        </div>
      );
    };

    return (
      <>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left: Write Section (2 columns) */}
          <div className="lg:col-span-2 space-y-4">
            {/* English */}
            <div className="bg-muted/50 border border-border/50 border-dashed rounded-xl p-4 space-y-4">
              <h3 className="text-sm font-semibold text-foreground">
                {t("settings.templatesCard.englishTitle")}
              </h3>

              <div className="space-y-2">
                <label className="text-sm font-medium leading-none">
                  {t("settings.templatesCard.subject")}
                </label>
                <Input
                  type="text"
                  name={subjectEnField}
                  disabled={isDisabled}
                  value={(form?.[subjectEnField] as string) ?? ""}
                  onChange={handleChange}
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium leading-none">
                  {t("settings.templatesCard.body")}
                </label>
                <Textarea
                  name={bodyEnField}
                  className="min-h-[200px] resize-none"
                  disabled={isDisabled}
                  value={(form?.[bodyEnField] as string) ?? ""}
                  onChange={handleChange}
                  placeholder="Use \n\n for paragraphs, \n for line breaks"
                />
              </div>
            </div>

            {/* French */}
            <div className="bg-muted/50 border border-border/50 border-dashed rounded-xl p-4 space-y-4">
              <h3 className="text-sm font-semibold text-foreground">
                {t("settings.templatesCard.frenchTitle")}
              </h3>

              <div className="space-y-2">
                <label className="text-sm font-medium leading-none">
                  {t("settings.templatesCard.subject")}
                </label>
                <Input
                  type="text"
                  name={subjectFrField}
                  disabled={isDisabled}
                  value={(form?.[subjectFrField] as string) ?? ""}
                  onChange={handleChange}
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium leading-none">
                  {t("settings.templatesCard.body")}
                </label>
                <Textarea
                  name={bodyFrField}
                  className="min-h-[200px] resize-none"
                  disabled={isDisabled}
                  value={(form?.[bodyFrField] as string) ?? ""}
                  onChange={handleChange}
                  placeholder="Utilisez \n\n pour les paragraphes, \n pour les retours à la ligne"
                />
              </div>
            </div>
          </div>

          {/* Right: Preview Section */}
          <div className="space-y-4">
            <div className="sticky top-6 space-y-4">
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 dark:bg-amber-950/30 dark:border-amber-900">
                <p className="text-xs text-amber-900 dark:text-amber-200 font-semibold">
                  📧 PREVIEW (English)
                </p>
              </div>
              {renderPreview(subjectEnField, bodyEnField)}

              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 dark:bg-amber-950/30 dark:border-amber-900 mt-8">
                <p className="text-xs text-amber-900 dark:text-amber-200 font-semibold">
                  📧 APERÇU (Français)
                </p>
              </div>
              {renderPreview(subjectFrField, bodyFrField)}
            </div>
          </div>
        </div>
      </>
    );
  };

  return (
    <>
      <div className="min-h-screen bg-muted/40 p-12">
        <div className="mx-auto max-w-6xl space-y-8">
          {/* HEADER */}
          <div className="space-y-2">
            <h1 className="text-3xl font-bold tracking-tight text-foreground">{t("settings.title")}</h1>
            <p className="text-muted-foreground">
              {t("settings.subtitle")}
            </p>
          </div>

          {/* ALERTS */}
          {error && (
            <div className="rounded-xl border border-destructive/20 bg-destructive/10 p-4 text-destructive text-sm">
              {error}
            </div>
          )}

          {saveMessage && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-700 text-sm dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-400">
              {saveMessage}
            </div>
          )}

          {/* LAYOUT */}
          <div className="grid gap-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* DEFAULT LANGUAGE CARD */}
            <div className="rounded-xl border border-border bg-card p-8 shadow-sm transition-all hover:border-primary/50 hover:shadow-md">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-card-foreground mb-1">
                <Globe className="h-5 w-5 text-primary" />
                {t("settings.defaultLanguageCard.title")}
              </h2>

              <p className="text-sm text-muted-foreground mb-6">
                {t("settings.defaultLanguageCard.description")}
              </p>

              <form>
                <div className="space-y-2 max-w-md">
                  <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                    {t("settings.defaultLanguageCard.label")}
                  </label>

                  <Select
                    value={form?.defaultLanguage ?? "en"}
                    onValueChange={(value) => {
                      setForm((prev) => prev ? { ...prev, defaultLanguage: value } : prev);
                    }}
                    disabled={isDisabled}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="en">{t("settings.languages.en")}</SelectItem>
                      <SelectItem value="fr">{t("settings.languages.fr")}</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </form>
            </div>

            {/* EMAIL TEMPLATE CARD */}
            <div className="rounded-xl border border-border bg-card p-8 shadow-sm transition-all hover:border-primary/50 hover:shadow-md delay-75 animate-in fade-in slide-in-from-bottom-4 fill-mode-both">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-card-foreground mb-1">
                <Mail className="h-5 w-5 text-primary" />
                {t("settings.templatesCard.title")}
              </h2>

              <p className="text-sm text-muted-foreground mb-6">
                {t("settings.templatesCard.description")}
              </p>

              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Email Type Selector */}
                <div className="max-w-md">
                  <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 block mb-2">
                    {t("settings.templatesCard.selectType")}
                  </label>
                  <Select
                    value={selectedTemplate}
                    onValueChange={(value) => setSelectedTemplate(value as EmailTemplateType)}
                    disabled={isDisabled}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {(Object.entries(EMAIL_TEMPLATES) as [EmailTemplateType, typeof EMAIL_TEMPLATES["invite"]][]).map(
                        ([key, template]) => (
                          <SelectItem key={key} value={key}>
                            {template.label}
                          </SelectItem>
                        )
                      )}
                    </SelectContent>
                  </Select>
                </div>

                {/* Available Variables Display */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 dark:bg-blue-950/30 dark:border-blue-900">
                  <p className="text-sm text-blue-900 dark:text-blue-200">
                    <span className="font-semibold">Available variables: </span>
                    {EMAIL_TEMPLATES[selectedTemplate].variables}
                  </p>
                </div>

                {/* Template Form */}
                {renderTemplateForm()}

                <div className="flex justify-end pt-4">
                  <Button
                    type="submit"
                    disabled={isDisabled}
                    className="px-8"
                  >
                    {t("settings.saveButton")}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      {/* Invite modal (only appears for admin user page, but kept here for compatibility) */}
      <InviteUserModal
        open={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </>
  );
}
