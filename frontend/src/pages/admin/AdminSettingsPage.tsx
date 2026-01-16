// src/pages/admin/AdminSettingsPage.tsx
import { useEffect, useState, useRef, useMemo } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Globe, Mail, Pencil, Eye, Heading2, Square, Lightbulb, Bold as BoldIcon, Italic as ItalicIcon, Highlighter, Minus } from "lucide-react";

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

const EMAIL_TEMPLATE_VARIABLES: Record<EmailTemplateType, string> = {
  invite: "{{name}}",
  documentSubmitted: "{{uploaderName}}, {{documentName}}, {{documentType}}, {{transactionId}}",
  documentRequested: "{{clientName}}, {{brokerName}}, {{documentName}}, {{documentType}}",
  documentReview: "{{brokerName}}, {{documentName}}, {{documentType}}, {{transactionId}}, {{status}}, {{brokerNotes}}",
  stageUpdate: "{{clientName}}, {{brokerName}}, {{transactionAddress}}, {{newStage}}",
};

export function AdminSettingsPage() {
  const { t } = useTranslation("admin");
  const hasLoadedRef = useRef(false);

  // Memoized email templates with translated labels
  const emailTemplates = useMemo(() => {
    const getLabel = (type: EmailTemplateType): string => {
      return t(`settings.templatesCard.emailTemplateTypes.${type}`);
    };

    return {
      invite: {
        label: getLabel("invite"),
        variables: EMAIL_TEMPLATE_VARIABLES.invite,
      },
      documentSubmitted: {
        label: getLabel("documentSubmitted"),
        variables: EMAIL_TEMPLATE_VARIABLES.documentSubmitted,
      },
      documentRequested: {
        label: getLabel("documentRequested"),
        variables: EMAIL_TEMPLATE_VARIABLES.documentRequested,
      },
      documentReview: {
        label: getLabel("documentReview"),
        variables: EMAIL_TEMPLATE_VARIABLES.documentReview,
      },
      stageUpdate: {
        label: getLabel("stageUpdate"),
        variables: EMAIL_TEMPLATE_VARIABLES.stageUpdate,
      },
    };
  }, [t]);

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [form, setForm] = useState<UpdateOrganizationSettingsRequest | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<EmailTemplateType>("invite");
  const [showPreview, setShowPreview] = useState(false);

  // Load settings (only once on mount)
  useEffect(() => {
    if (hasLoadedRef.current) return;
    hasLoadedRef.current = true;

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
  }, []);

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

      toast.success(t("settings.messages.saved"));
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

  const insertTemplate = (fieldName: keyof UpdateOrganizationSettingsRequest, template: string) => {
    setForm((prev) => {
      if (!prev) return prev;
      const currentValue = (prev[fieldName] as string) ?? "";
      const newValue = currentValue + (currentValue ? "\n\n" : "") + template;
      return { ...prev, [fieldName]: newValue };
    });
  };

  // Memoize field names so they don't change on every render
  const fieldNames = useMemo(() => {
    const subjectEnField = getSubjectFieldName(selectedTemplate, "En");
    const bodyEnField = getBodyFieldName(selectedTemplate, "En");
    const subjectFrField = getSubjectFieldName(selectedTemplate, "Fr");
    const bodyFrField = getBodyFieldName(selectedTemplate, "Fr");
    return { subjectEnField, bodyEnField, subjectFrField, bodyFrField };
  }, [selectedTemplate]);

  const convertTextToHtml = (text: string) => {
    if (!text) return "";
    
    // Escape HTML
    let escaped = text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
    
    // Handle [HEADING]...[/HEADING]
    escaped = escaped.replace(
      /\[HEADING\](.*?)\[\/HEADING\]/gs,
      '<h3 style="font-size: 1.5em; font-weight: bold; margin: 15px 0 10px 0;">$1</h3>'
    );
    
    // Handle [BOX]...[/BOX]
    escaped = escaped.replace(
      /\[BOX\](.*?)\[\/BOX\]/gs,
      '<div style="border-left: 4px solid #3b82f6; background-color: #eff6ff; padding: 15px; margin: 15px 0; border-radius: 4px; max-width: 600px;"><p style="margin: 0; color: #1e3a8a; font-weight: 500;">$1</p></div>'
    );

    // Handle colored boxes
    escaped = escaped.replace(
      /\[BOX-RED\](.*?)\[\/BOX-RED\]/gs,
      '<div style="border-left: 4px solid #ef4444; background-color: #fee2e2; padding: 15px; margin: 15px 0; border-radius: 4px; max-width: 600px;"><p style="margin: 0; color: #7f1d1d; font-weight: 500;">$1</p></div>'
    );
    escaped = escaped.replace(
      /\[BOX-BLUE\](.*?)\[\/BOX-BLUE\]/gs,
      '<div style="border-left: 4px solid #3b82f6; background-color: #eff6ff; padding: 15px; margin: 15px 0; border-radius: 4px; max-width: 600px;"><p style="margin: 0; color: #1e3a8a; font-weight: 500;">$1</p></div>'
    );
    escaped = escaped.replace(
      /\[BOX-GREEN\](.*?)\[\/BOX-GREEN\]/gs,
      '<div style="border-left: 4px solid #22c55e; background-color: #f0fdf4; padding: 15px; margin: 15px 0; border-radius: 4px; max-width: 600px;"><p style="margin: 0; color: #166534; font-weight: 500;">$1</p></div>'
    );
    escaped = escaped.replace(
      /\[BOX-YELLOW\](.*?)\[\/BOX-YELLOW\]/gs,
      '<div style="border-left: 4px solid #eab308; background-color: #fefce8; padding: 15px; margin: 15px 0; border-radius: 4px; max-width: 600px;"><p style="margin: 0; color: #713f12; font-weight: 500;">$1</p></div>'
    );
    
    // Handle [BOLD]...[/BOLD]
    escaped = escaped.replace(/\[BOLD\](.*?)\[\/BOLD\]/gs, '<strong>$1</strong>');

    // Handle [ITALIC]...[/ITALIC]
    escaped = escaped.replace(/\[ITALIC\](.*?)\[\/ITALIC\]/gs, '<em>$1</em>');

    // Handle [HIGHLIGHT]...[/HIGHLIGHT]
    escaped = escaped.replace(
      /\[HIGHLIGHT\](.*?)\[\/HIGHLIGHT\]/gs,
      '<span style="background-color: #fef08a; padding: 0 4px; border-radius: 3px;">$1</span>'
    );

    // Handle [SEPARATOR]
    escaped = escaped.replace(/\[SEPARATOR\]/g, '<hr style="border: none; border-top: 1px solid #e5e7eb; margin: 16px 0;" />');
    
    // Convert double newlines to paragraph breaks
    let html = escaped.replace(/\n\n/g, "</p><p>");
    
    // Convert single newlines to line breaks
    html = html.replace(/\n/g, "<br>");
    
    // Wrap in paragraph tags
    return "<p>" + html + "</p>";
  };

  const renderPreview = (subjectField: keyof UpdateOrganizationSettingsRequest, bodyField: keyof UpdateOrganizationSettingsRequest) => {
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

  const renderTemplateForm = () => {
    const { subjectEnField, bodyEnField, subjectFrField, bodyFrField } = fieldNames;

    const renderWriteSection = (
      subjectEnField: keyof UpdateOrganizationSettingsRequest,
      bodyEnField: keyof UpdateOrganizationSettingsRequest,
      subjectFrField: keyof UpdateOrganizationSettingsRequest,
      bodyFrField: keyof UpdateOrganizationSettingsRequest
    ) => (
      <div className="space-y-6">
        {/* Formatting Guide */}
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 dark:bg-green-950/30 dark:border-green-900">
          <p className="text-sm text-green-900 dark:text-green-200 font-semibold mb-3 flex items-center gap-2">
            <Lightbulb className="h-4 w-4" />
            {t("settings.templatesCard.formattingGuide")}
          </p>
          <ul className="text-xs text-green-800 dark:text-green-300 space-y-2">
            <li><span className="font-mono font-semibold">[HEADING]Your Text[/HEADING]</span> - {t("settings.templatesCard.formattingGuideHeading")}</li>
            <li><span className="font-mono font-semibold">[BOX]Your Text[/BOX]</span> - {t("settings.templatesCard.formattingGuideBox")}</li>
            <li><span className="font-mono font-semibold">[BOLD]Text[/BOLD]</span> - {t("settings.templatesCard.formattingGuideBold")}</li>
            <li><span className="font-mono font-semibold">[ITALIC]Text[/ITALIC]</span> - {t("settings.templatesCard.formattingGuideItalic")}</li>
            <li><span className="font-mono font-semibold">[HIGHLIGHT]Text[/HIGHLIGHT]</span> - {t("settings.templatesCard.formattingGuideHighlight")}</li>
            <li><span className="font-mono font-semibold">[SEPARATOR]</span> - {t("settings.templatesCard.formattingGuideSeparator")}</li>
            <li><span className="font-mono font-semibold">[BOX-RED], [BOX-BLUE], [BOX-GREEN], [BOX-YELLOW]</span> - {t("settings.templatesCard.formattingGuideBoxRed")} / {t("settings.templatesCard.formattingGuideBoxBlue")} / {t("settings.templatesCard.formattingGuideBoxGreen")} / {t("settings.templatesCard.formattingGuideBoxYellow")}</li>
            <li>{t("settings.templatesCard.formattingGuideParagraphs")}</li>
          </ul>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* English column */}
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
              <label className="text-sm font-medium leading-none flex items-center justify-between">
                <span>{t("settings.templatesCard.body")}</span>
              </label>
              
              {/* Toolbar */}
              <div className="flex gap-2 mb-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[HEADING]Your heading here[/HEADING]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Heading2 className="h-4 w-4" />
                  {t("settings.templatesCard.headingButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOX]Your important content here[/BOX]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Square className="h-4 w-4" />
                  {t("settings.templatesCard.boxButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOLD]Bold text[/BOLD]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <BoldIcon className="h-4 w-4" />
                  {t("settings.templatesCard.boldButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[ITALIC]Italic text[/ITALIC]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <ItalicIcon className="h-4 w-4" />
                  {t("settings.templatesCard.italicButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT]Highlighted text[/HIGHLIGHT]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Highlighter className="h-4 w-4" />
                  {t("settings.templatesCard.highlightButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[SEPARATOR]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Minus className="h-4 w-4" />
                  {t("settings.templatesCard.separatorButton")}
                </button>
              </div>

              {/* Colored Box Toolbar */}
              <div className="flex gap-2 mb-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOX-RED]Your error message[/BOX-RED]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-red-200 hover:bg-red-300 dark:bg-red-900/40 dark:hover:bg-red-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-red-400"
                >
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                  {t("settings.templatesCard.boxRedButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOX-BLUE]Your info message[/BOX-BLUE]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-blue-200 hover:bg-blue-300 dark:bg-blue-900/40 dark:hover:bg-blue-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-blue-400"
                >
                  <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                  {t("settings.templatesCard.boxBlueButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOX-GREEN]Your success message[/BOX-GREEN]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-green-200 hover:bg-green-300 dark:bg-green-900/40 dark:hover:bg-green-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-green-400"
                >
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  {t("settings.templatesCard.boxGreenButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyEnField, "[BOX-YELLOW]Your warning message[/BOX-YELLOW]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-yellow-200 hover:bg-yellow-300 dark:bg-yellow-900/40 dark:hover:bg-yellow-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-yellow-400"
                >
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                  {t("settings.templatesCard.boxYellowButton")}
                </button>
              </div>

              <Textarea
                name={bodyEnField}
                className="min-h-[200px] resize-none font-mono text-xs"
                disabled={isDisabled}
                value={(form?.[bodyEnField] as string) ?? ""}
                onChange={handleChange}
                placeholder="Use the formatting buttons; press Enter twice for paragraphs."
              />
            </div>
          </div>

          {/* French column */}
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
              <label className="text-sm font-medium leading-none flex items-center justify-between">
                <span>{t("settings.templatesCard.body")}</span>
              </label>
              
              {/* Toolbar */}
              <div className="flex gap-2 mb-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[HEADING]Votre titre ici[/HEADING]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Heading2 className="h-4 w-4" />
                  {t("settings.templatesCard.headingButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOX]Votre contenu important ici[/BOX]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Square className="h-4 w-4" />
                  {t("settings.templatesCard.boxButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOLD]Texte en gras[/BOLD]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <BoldIcon className="h-4 w-4" />
                  {t("settings.templatesCard.boldButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[ITALIC]Texte en italique[/ITALIC]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <ItalicIcon className="h-4 w-4" />
                  {t("settings.templatesCard.italicButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT]Texte surligné[/HIGHLIGHT]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Highlighter className="h-4 w-4" />
                  {t("settings.templatesCard.highlightButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[SEPARATOR]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Minus className="h-4 w-4" />
                  {t("settings.templatesCard.separatorButton")}
                </button>
              </div>

              {/* Colored Box Toolbar */}
              <div className="flex gap-2 mb-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOX-RED]Votre message d'erreur[/BOX-RED]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-red-200 hover:bg-red-300 dark:bg-red-900/40 dark:hover:bg-red-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-red-400"
                >
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                  {t("settings.templatesCard.boxRedButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOX-BLUE]Votre message d'information[/BOX-BLUE]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-blue-200 hover:bg-blue-300 dark:bg-blue-900/40 dark:hover:bg-blue-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-blue-400"
                >
                  <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                  {t("settings.templatesCard.boxBlueButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOX-GREEN]Votre message de succès[/BOX-GREEN]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-green-200 hover:bg-green-300 dark:bg-green-900/40 dark:hover:bg-green-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-green-400"
                >
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  {t("settings.templatesCard.boxGreenButton")}
                </button>
                <button
                  type="button"
                  onClick={() => insertTemplate(bodyFrField, "[BOX-YELLOW]Votre message d'avertissement[/BOX-YELLOW]")}
                  disabled={isDisabled}
                  className="flex items-center gap-1 px-2 py-1.5 bg-yellow-200 hover:bg-yellow-300 dark:bg-yellow-900/40 dark:hover:bg-yellow-900/60 rounded text-xs font-medium transition-colors disabled:opacity-50 border border-yellow-400"
                >
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                  {t("settings.templatesCard.boxYellowButton")}
                </button>
              </div>

              <Textarea
                name={bodyFrField}
                className="min-h-[200px] resize-none font-mono text-xs"
                disabled={isDisabled}
                value={(form?.[bodyFrField] as string) ?? ""}
                onChange={handleChange}
                placeholder="Utilisez les boutons de formatage; appuyez deux fois sur Entrée pour séparer les paragraphes."
              />
            </div>
          </div>
        </div>
      </div>
    );

    const renderPreviewSection = () => (
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* English Preview */}
        <div className="space-y-2">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 dark:bg-blue-950/30 dark:border-blue-900">
            <p className="text-xs text-blue-900 dark:text-blue-200 font-semibold flex items-center gap-2">
              <Mail className="h-3 w-3" />
              PREVIEW (English)
            </p>
          </div>
          {renderPreview(subjectEnField, bodyEnField)}
        </div>

        {/* French Preview */}
        <div className="space-y-2">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 dark:bg-blue-950/30 dark:border-blue-900">
            <p className="text-xs text-blue-900 dark:text-blue-200 font-semibold flex items-center gap-2">
              <Mail className="h-3 w-3" />
              APERÇU (Français)
            </p>
          </div>
          {renderPreview(subjectFrField, bodyFrField)}
        </div>
      </div>
    );

    return (
      <>
        {/* Toggle Buttons */}
        <div className="flex gap-2 mb-6">
          <button
            type="button"
            onClick={() => setShowPreview(false)}
            className={`px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 ${
              !showPreview
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            <Pencil className="h-4 w-4" />
            {t("settings.templatesCard.write")}
          </button>
          <button
            type="button"
            onClick={() => setShowPreview(true)}
            className={`px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 ${
              showPreview
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            <Eye className="h-4 w-4" />
            {t("settings.templatesCard.preview")}
          </button>
        </div>

        {/* Content */}
        {showPreview ? renderPreviewSection() : renderWriteSection(subjectEnField, bodyEnField, subjectFrField, bodyFrField)}
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
                      {(Object.entries(emailTemplates) as [EmailTemplateType, typeof emailTemplates["invite"]][]).map(
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
                    <span className="font-semibold">{t("settings.templatesCard.availableVariables")} </span>
                    {emailTemplates[selectedTemplate].variables}
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
