// ...existing imports...

// src/pages/admin/AdminSettingsPage.tsx
import { useEffect, useState, useRef, useMemo } from "react";

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
  | "stageUpdate"
  | "propertyOfferMade"
  | "propertyOfferStatus"
  | "offerReceived"
  | "offerStatus";

const EMAIL_TEMPLATE_VARIABLES: Record<EmailTemplateType, string> = {
  invite: "{{name}}",
  documentSubmitted: "{{uploaderName}}, {{documentName}}, {{documentType}}, {{transactionId}}",
  documentRequested: "{{clientName}}, {{brokerName}}, {{documentName}}, {{documentType}}",
  documentReview: "{{brokerName}}, {{documentName}}, {{documentType}}, {{transactionId}}, {{status}}, {{brokerNotes}}",
  stageUpdate: "{{clientName}}, {{brokerName}}, {{transactionAddress}}, {{newStage}}",
  propertyOfferMade: "{{clientName}}, {{brokerName}}, {{propertyAddress}}, {{offerAmount}}, {{offerRound}}",
  propertyOfferStatus: "{{clientName}}, {{brokerName}}, {{propertyAddress}}, {{previousStatus}}, {{newStatus}}, {{counterpartyResponse}}",
  offerReceived: "{{clientName}}, {{brokerName}}, {{buyerName}}, {{offerAmount}}",
  offerStatus: "{{clientName}}, {{brokerName}}, {{buyerName}}, {{previousStatus}}, {{newStatus}}",
};

export function AdminSettingsPage() {

  const { t } = useTranslation("admin");
  const hasLoadedRef = useRef(false);
  // Use number | null for browser setTimeout compatibility
  const hideTimeoutEnRef = useRef<number | null>(null);
  const hideTimeoutFrRef = useRef<number | null>(null);
  const hideHighlightTimeoutEnRef = useRef<number | null>(null);
  const hideHighlightTimeoutFrRef = useRef<number | null>(null);

  // Color picker state hooks (must be at the top)
  const [showBoxColorPickerEn, setShowBoxColorPickerEn] = useState(false);
  const [showHighlightColorPickerEn, setShowHighlightColorPickerEn] = useState(false);
  const [showBoxColorPickerFr, setShowBoxColorPickerFr] = useState(false);
  const [showHighlightColorPickerFr, setShowHighlightColorPickerFr] = useState(false);

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
      propertyOfferMade: {
        label: getLabel("propertyOfferMade"),
        variables: EMAIL_TEMPLATE_VARIABLES.propertyOfferMade,
      },
      propertyOfferStatus: {
        label: getLabel("propertyOfferStatus"),
        variables: EMAIL_TEMPLATE_VARIABLES.propertyOfferStatus,
      },
      offerReceived: {
        label: getLabel("offerReceived"),
        variables: EMAIL_TEMPLATE_VARIABLES.offerReceived,
      },
      offerStatus: {
        label: getLabel("offerStatus"),
        variables: EMAIL_TEMPLATE_VARIABLES.offerStatus,
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
  const [showFormattingGuide, setShowFormattingGuide] = useState(true);
  // Simplified color picker state management
  // (supprimé car non utilisé)

  // Disable form controls if loading or saving
  const isDisabled = isLoading || isSaving;

  // Get subject field name for a template and language
  function getSubjectFieldName(type: EmailTemplateType, lang: "En" | "Fr"): keyof UpdateOrganizationSettingsRequest {
    const fieldMap: Record<EmailTemplateType, Record<"En" | "Fr", keyof UpdateOrganizationSettingsRequest>> = {
      invite: { En: "inviteSubjectEn", Fr: "inviteSubjectFr" },
      documentSubmitted: { En: "documentSubmittedSubjectEn", Fr: "documentSubmittedSubjectFr" },
      documentRequested: { En: "documentRequestedSubjectEn", Fr: "documentRequestedSubjectFr" },
      documentReview: { En: "documentReviewSubjectEn", Fr: "documentReviewSubjectFr" },
      stageUpdate: { En: "stageUpdateSubjectEn", Fr: "stageUpdateSubjectFr" },
      propertyOfferMade: { En: "propertyOfferMadeSubjectEn", Fr: "propertyOfferMadeSubjectFr" },
      propertyOfferStatus: { En: "propertyOfferStatusSubjectEn", Fr: "propertyOfferStatusSubjectFr" },
      offerReceived: { En: "offerReceivedSubjectEn", Fr: "offerReceivedSubjectFr" },
      offerStatus: { En: "offerStatusSubjectEn", Fr: "offerStatusSubjectFr" },
    };
    return fieldMap[type][lang];
  }

  // Handle input changes for all fields
  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const { name, value } = e.target;
    setForm((prev) => prev ? { ...prev, [name]: value } : prev);
  }

  // Handle form submit
  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form) return;
    setIsSaving(true);
    setError(null);
    try {
      await updateOrganizationSettings(form);
      toast.success(t("settings.success.save"));
    } catch {
      toast.error(t("settings.errors.saveFailed"));
      setError(t("settings.errors.saveFailed"));
    } finally {
      setIsSaving(false);
    }
  }

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
          propertyOfferMadeSubjectEn: data.propertyOfferMadeSubjectEn,
          propertyOfferMadeBodyEn: data.propertyOfferMadeBodyEn,
          propertyOfferMadeSubjectFr: data.propertyOfferMadeSubjectFr,
          propertyOfferMadeBodyFr: data.propertyOfferMadeBodyFr,
          propertyOfferStatusSubjectEn: data.propertyOfferStatusSubjectEn,
          propertyOfferStatusBodyEn: data.propertyOfferStatusBodyEn,
          propertyOfferStatusSubjectFr: data.propertyOfferStatusSubjectFr,
          propertyOfferStatusBodyFr: data.propertyOfferStatusBodyFr,
          offerReceivedSubjectEn: data.offerReceivedSubjectEn,
          offerReceivedBodyEn: data.offerReceivedBodyEn,
          offerReceivedSubjectFr: data.offerReceivedSubjectFr,
          offerReceivedBodyFr: data.offerReceivedBodyFr,
          offerStatusSubjectEn: data.offerStatusSubjectEn,
          offerStatusBodyEn: data.offerStatusBodyEn,
          offerStatusSubjectFr: data.offerStatusSubjectFr,
          offerStatusBodyFr: data.offerStatusBodyFr,
        });
      } catch {
        toast.error(t("settings.errors.loadFailed"));
        setError(t("settings.errors.loadFailed"));
      }
    }
    load();
  }, [t]);

  const getBodyFieldName = (type: EmailTemplateType, lang: "En" | "Fr"): keyof UpdateOrganizationSettingsRequest => {
    const fieldMap: Record<EmailTemplateType, Record<"En" | "Fr", keyof UpdateOrganizationSettingsRequest>> = {
      invite: { En: "inviteBodyEn", Fr: "inviteBodyFr" },
      documentSubmitted: { En: "documentSubmittedBodyEn", Fr: "documentSubmittedBodyFr" },
      documentRequested: { En: "documentRequestedBodyEn", Fr: "documentRequestedBodyFr" },
      documentReview: { En: "documentReviewBodyEn", Fr: "documentReviewBodyFr" },
      stageUpdate: { En: "stageUpdateBodyEn", Fr: "stageUpdateBodyFr" },
      propertyOfferMade: { En: "propertyOfferMadeBodyEn", Fr: "propertyOfferMadeBodyFr" },
      propertyOfferStatus: { En: "propertyOfferStatusBodyEn", Fr: "propertyOfferStatusBodyFr" },
      offerReceived: { En: "offerReceivedBodyEn", Fr: "offerReceivedBodyFr" },
      offerStatus: { En: "offerStatusBodyEn", Fr: "offerStatusBodyFr" },
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
    let escaped = text;
    // Handle [SEPARATOR]
    escaped = escaped.replace(/\[SEPARATOR\]/g, '<hr style="border: none; border-top: 1px solid #e5e7eb; margin: 16px 0;" />');
    // Convert double newlines to paragraph breaks
    let html = escaped.replace(/\n\n/g, "</p><p>");
    // Convert single newlines to line breaks
    html = html.replace(/\n/g, "<br>");
    // Wrap in paragraph tags and center within container for consistency with backend
    return "<div style=\"max-width: 600px; margin: 0 auto;\"><p>" + html + "</p></div>";
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

        {/* Password Link Section - Only for Invite Template */}
        {selectedTemplate === "invite" && (
        <div className="mt-4 pt-4 border-t border-gray-300">
          {/* Button */}
          <table role="presentation" style={{ margin: "30px 0" }}>
            <tbody>
              <tr>
                <td>
                  <a
                    href="#"
                    style={{
                      backgroundColor: "#4CAF50",
                      color: "white",
                      padding: "14px 28px",
                      textDecoration: "none",
                      borderRadius: "4px",
                      display: "inline-block",
                      fontWeight: "bold"
                    }}
                  >
                    {fieldNames.bodyEnField === "inviteBodyEn" 
                      ? "Set Your Password" 
                      : "Définir votre mot de passe"}
                  </a>
                </td>
              </tr>
            </tbody>
          </table>
          
          {/* Link instructions */}
          <p style={{ marginTop: "30px", color: "#666", fontSize: "14px" }}>
            {fieldNames.bodyEnField === "inviteBodyEn" 
              ? "Or copy and paste this URL into your browser:" 
              : "Ou copiez-collez ce lien dans votre navigateur :"}
          </p>
          <p style={{ color: "#666", fontSize: "12px", wordBreak: "break-all", marginTop: "10px" }}>
            https://example.com/reset-password?token=abc123def456
          </p>
        </div>
        )}

        {/* Footer Preview */}
        <div className="border-t border-gray-300 pt-4 mt-4">
          <hr style={{ border: "none", borderTop: "1px solid #e0e0e0", margin: "0" }} />
          <p style={{ color: "#666", fontSize: "12px", lineHeight: "1.6", marginTop: "15px" }}>
            {selectedTemplate === "invite" && fieldNames.bodyEnField === "inviteBodyEn" ? (
              <>
                Thanks,<br />
                Best regards,<br />
                <strong>CourtierPro Team</strong>
              </>
            ) : (
              <>
                Merci,<br />
                Cordialement,<br />
                <strong>Équipe CourtierPro</strong>
              </>
            )}
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
        {/* Formatting Guide - Collapsible */}
        <div className="bg-green-50 border border-green-200 rounded-lg dark:bg-green-950/30 dark:border-green-900">
          <button
            type="button"
            onClick={() => setShowFormattingGuide(!showFormattingGuide)}
            className="w-full px-4 py-3 flex items-center justify-between hover:bg-green-100 dark:hover:bg-green-900/50 transition-colors rounded-t-lg"
          >
            <p className="text-sm text-green-900 dark:text-green-200 font-semibold flex items-center gap-2">
              <Lightbulb className="h-4 w-4" />
              {t("settings.templatesCard.formattingGuide")}
            </p>
            <span className={`text-green-900 dark:text-green-200 transition-transform ${showFormattingGuide ? 'rotate-180' : ''}`}>
              ▼
            </span>
          </button>

          {showFormattingGuide && (
          <div className="px-4 pb-4 border-t border-green-200 dark:border-green-900">
            <ul className="text-xs text-green-800 dark:text-green-300 space-y-2 mt-3">
              <li><span className="font-mono font-semibold">[HEADING] / [HEADING-SM] / [HEADING-MD] / [HEADING-LG]</span> - {t("settings.templatesCard.formattingGuideHeadingSizes")}</li>
              <li><span className="font-mono font-semibold">[BOX]Your Text[/BOX]</span> - {t("settings.templatesCard.formattingGuideBox")}</li>
              <li><span className="font-mono font-semibold">[BOX-gray], [BOX-red], [BOX-green], [BOX-blue], [BOX-yellow], [BOX-orange], [BOX-white]</span> - Colored boxes</li>
              <li><span className="font-mono font-semibold">[BOLD]Text[/BOLD]</span> - {t("settings.templatesCard.formattingGuideBold")}</li>
              <li><span className="font-mono font-semibold">[ITALIC]Text[/ITALIC]</span> - {t("settings.templatesCard.formattingGuideItalic")}</li>
              <li><span className="font-mono font-semibold">[HIGHLIGHT]Text[/HIGHLIGHT]</span> - {t("settings.templatesCard.formattingGuideHighlight")}</li>
              <li><span className="font-mono font-semibold">[HIGHLIGHT-yellow], [HIGHLIGHT-pink], [HIGHLIGHT-blue], [HIGHLIGHT-green], [HIGHLIGHT-orange]</span> - Colored highlights</li>
              <li><span className="font-mono font-semibold">[SEPARATOR]</span> - {t("settings.templatesCard.formattingGuideSeparator")}</li>
              <li><span className="font-mono font-semibold">[IF-variableName]Content[/IF-variableName]</span> - {t("settings.templatesCard.formattingGuideConditional")}</li>
              <li>{t("settings.templatesCard.formattingGuideParagraphs")}</li>
            </ul>
          </div>
          )}
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

                {/* BOX button with color picker dropdown */}
                <div
                  className="relative"
                  onMouseEnter={() => {
                    clearTimeout(hideTimeoutEnRef.current === null ? undefined : hideTimeoutEnRef.current);
                    setShowBoxColorPickerEn(true);
                  }}
                  onMouseLeave={() => {
                    hideTimeoutEnRef.current = setTimeout(() => {
                      setShowBoxColorPickerEn(false);
                    }, 150);
                  }}
                >
                  <button
                    type="button"
                    disabled={isDisabled}
                    className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    <Square className="h-4 w-4" />
                    {t("settings.templatesCard.boxButton")}
                  </button>
                  
                  {/* Dropdown menu */}
                  {showBoxColorPickerEn && (
                  <div className="absolute left-0 top-full mt-1 bg-white dark:bg-slate-800 border border-border rounded shadow-lg z-50 p-2 space-y-1 w-48">
                    {/* Named colors */}
                    <div>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-gray]Your message[/BOX-gray]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-gray-100 dark:hover:bg-gray-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-gray-500 rounded-full"></div>
                        Gray
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-red]Your message[/BOX-red]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-red-100 dark:hover:bg-red-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                        Red
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-green]Your message[/BOX-green]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-green-100 dark:hover:bg-green-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                        Green
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-blue]Your message[/BOX-blue]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                        Blue
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-yellow]Your message[/BOX-yellow]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-yellow-100 dark:hover:bg-yellow-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                        Yellow
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-orange]Your message[/BOX-orange]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-orange-100 dark:hover:bg-orange-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
                        Orange
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[BOX-white]Your message[/BOX-white]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-slate-100 dark:hover:bg-slate-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-white rounded-full border border-gray-300"></div>
                        White
                      </button>
                    </div>
                  </div>
                  )}
                </div>

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
                <div
                  className="relative"
                  onMouseEnter={() => {
                    clearTimeout(hideHighlightTimeoutEnRef.current === null ? undefined : hideHighlightTimeoutEnRef.current);
                    setShowHighlightColorPickerEn(true);
                  }}
                  onMouseLeave={() => {
                    hideHighlightTimeoutEnRef.current = setTimeout(() => {
                      setShowHighlightColorPickerEn(false);
                    }, 150);
                  }}
                >
                  <button
                    type="button"
                    disabled={isDisabled}
                    className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    <Highlighter className="h-4 w-4" />
                    {t("settings.templatesCard.highlightButton")}
                  </button>

                  {/* Dropdown menu */}
                  {showHighlightColorPickerEn && (
                  <div className="absolute left-0 top-full mt-1 bg-white dark:bg-slate-800 border border-border rounded shadow-lg z-50 p-2 space-y-1 w-48">
                    {/* Highlight colors */}
                    <div>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT-yellow]Highlighted text[/HIGHLIGHT-yellow]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-yellow-100 dark:hover:bg-yellow-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-yellow-300 rounded-full"></div>
                        Yellow
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT-pink]Highlighted text[/HIGHLIGHT-pink]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-pink-100 dark:hover:bg-pink-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-pink-300 rounded-full"></div>
                        Pink
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT-blue]Highlighted text[/HIGHLIGHT-blue]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-blue-300 rounded-full"></div>
                        Blue
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT-green]Highlighted text[/HIGHLIGHT-green]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-green-100 dark:hover:bg-green-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-green-300 rounded-full"></div>
                        Green
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyEnField, "[HIGHLIGHT-orange]Highlighted text[/HIGHLIGHT-orange]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-orange-100 dark:hover:bg-orange-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-orange-300 rounded-full"></div>
                        Orange
                      </button>
                    </div>
                  </div>
                  )}
                </div>
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

                {/* BOX button with color picker dropdown - FR */}
                <div
                  className="relative"
                  onMouseEnter={() => {
                    clearTimeout(hideTimeoutFrRef.current === null ? undefined : hideTimeoutFrRef.current);
                    setShowBoxColorPickerFr(true);
                  }}
                  onMouseLeave={() => {
                    hideTimeoutFrRef.current = setTimeout(() => {
                      setShowBoxColorPickerFr(false);
                    }, 150);
                  }}
                >
                  <button
                    type="button"
                    disabled={isDisabled}
                    className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    <Square className="h-4 w-4" />
                    {t("settings.templatesCard.boxButton")}
                  </button>
                  
                  {/* Dropdown menu */}
                  {showBoxColorPickerFr && (
                  <div className="absolute left-0 top-full mt-1 bg-white dark:bg-slate-800 border border-border rounded shadow-lg z-50 p-2 space-y-1 w-48">
                    {/* Named colors */}
                    <div>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-gray]Votre message[/BOX-gray]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-gray-100 dark:hover:bg-gray-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-gray-500 rounded-full"></div>
                        Gris
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-red]Votre message[/BOX-red]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-red-100 dark:hover:bg-red-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                        Rouge
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-green]Votre message[/BOX-green]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-green-100 dark:hover:bg-green-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                        Vert
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-blue]Votre message[/BOX-blue]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                        Bleu
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-yellow]Votre message[/BOX-yellow]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-yellow-100 dark:hover:bg-yellow-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                        Jaune
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-orange]Votre message[/BOX-orange]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-orange-100 dark:hover:bg-orange-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
                        Orange
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[BOX-white]Votre message[/BOX-white]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-slate-100 dark:hover:bg-slate-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-white rounded-full border border-gray-300"></div>
                        Blanc
                      </button>
                    </div>
                  </div>
                  )}
                </div>

                <div
                  className="relative"
                  onMouseEnter={() => {
                    clearTimeout(hideHighlightTimeoutFrRef.current === null ? undefined : hideHighlightTimeoutFrRef.current);
                    setShowHighlightColorPickerFr(true);
                  }}
                  onMouseLeave={() => {
                    hideHighlightTimeoutFrRef.current = setTimeout(() => {
                      setShowHighlightColorPickerFr(false);
                    }, 150);
                  }}
                >
                  <button
                    type="button"
                    disabled={isDisabled}
                    className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    <Highlighter className="h-4 w-4" />
                    {t("settings.templatesCard.highlightButton")}
                  </button>

                  {/* Dropdown menu */}
                  {showHighlightColorPickerFr && (
                  <div className="absolute left-0 top-full mt-1 bg-white dark:bg-slate-800 border border-border rounded shadow-lg z-50 p-2 space-y-1 w-48">
                    {/* Highlight colors */}
                    <div>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT-yellow]Texte surligné[/HIGHLIGHT-yellow]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-yellow-100 dark:hover:bg-yellow-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-yellow-300 rounded-full"></div>
                        Jaune
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT-pink]Texte surligné[/HIGHLIGHT-pink]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-pink-100 dark:hover:bg-pink-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-pink-300 rounded-full"></div>
                        Rose
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT-blue]Texte surligné[/HIGHLIGHT-blue]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-blue-300 rounded-full"></div>
                        Bleu
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT-green]Texte surligné[/HIGHLIGHT-green]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-green-100 dark:hover:bg-green-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-green-300 rounded-full"></div>
                        Vert
                      </button>
                      <button
                        type="button"
                        onClick={() => insertTemplate(bodyFrField, "[HIGHLIGHT-orange]Texte surligné[/HIGHLIGHT-orange]")}
                        disabled={isDisabled}
                        className="w-full text-left px-2 py-1 hover:bg-orange-100 dark:hover:bg-orange-900/40 rounded text-xs transition-colors flex items-center gap-2"
                      >
                        <div className="w-3 h-3 bg-orange-300 rounded-full"></div>
                        Orange
                      </button>
                    </div>
                  </div>
                  )}
                </div>

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
                  onClick={() => insertTemplate(bodyFrField, "[SEPARATOR]")}
                  disabled={isDisabled}
                  className="flex items-center gap-2 px-3 py-1.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-sm font-medium transition-colors disabled:opacity-50"
                >
                  <Minus className="h-4 w-4" />
                  {t("settings.templatesCard.separatorButton")}
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

