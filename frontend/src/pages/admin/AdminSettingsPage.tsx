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

export function AdminSettingsPage() {
  const { t } = useTranslation("admin");

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [form, setForm] = useState<UpdateOrganizationSettingsRequest | null>(
    null
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

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

      return {
        defaultLanguage:
          field === "defaultLanguage" ? value : prev.defaultLanguage,
        inviteSubjectEn:
          field === "inviteSubjectEn" ? value : prev.inviteSubjectEn,
        inviteBodyEn: field === "inviteBodyEn" ? value : prev.inviteBodyEn,
        inviteSubjectFr:
          field === "inviteSubjectFr" ? value : prev.inviteSubjectFr,
        inviteBodyFr: field === "inviteBodyFr" ? value : prev.inviteBodyFr,
      };
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
                {t("settings.templatesCard.description")}{" "}
                <span className="inline-flex items-center rounded-md border border-orange-200 bg-orange-50 px-2 py-0.5 text-xs font-medium text-orange-800 dark:border-orange-900 dark:bg-orange-950 dark:text-orange-300">
                  {"{{name}}"}
                </span>
              </p>

              <form onSubmit={handleSubmit}>
                {/* 2 columns */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* English column */}
                  <div className="bg-muted/50 border border-border/50 dashed border rounded-xl p-4 space-y-4">
                    <h3 className="text-sm font-semibold text-foreground">
                      {t("settings.templatesCard.englishTitle")}
                    </h3>

                    <div className="space-y-2">
                      <label className="text-sm font-medium leading-none">
                        {t("settings.templatesCard.subject")}
                      </label>
                      <Input
                        type="text"
                        name="inviteSubjectEn"
                        disabled={isDisabled}
                        value={form?.inviteSubjectEn ?? ""}
                        onChange={handleChange}
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-medium leading-none">
                        {t("settings.templatesCard.body")}
                      </label>
                      <Textarea
                        name="inviteBodyEn"
                        className="min-h-[120px] resize-y"
                        disabled={isDisabled}
                        value={form?.inviteBodyEn ?? ""}
                        onChange={handleChange}
                      />
                    </div>
                  </div>

                  {/* French column */}
                  <div className="bg-muted/50 border border-border/50 dashed border rounded-xl p-4 space-y-4">
                    <h3 className="text-sm font-semibold text-foreground">
                      {t("settings.templatesCard.frenchTitle")}
                    </h3>

                    <div className="space-y-2">
                      <label className="text-sm font-medium leading-none">
                        {t("settings.templatesCard.subjectFr")}
                      </label>
                      <Input
                        type="text"
                        name="inviteSubjectFr"
                        disabled={isDisabled}
                        value={form?.inviteSubjectFr ?? ""}
                        onChange={handleChange}
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-medium leading-none">
                        {t("settings.templatesCard.bodyFr")}
                      </label>
                      <Textarea
                        name="inviteBodyFr"
                        className="min-h-[120px] resize-y"
                        disabled={isDisabled}
                        value={form?.inviteBodyFr ?? ""}
                        onChange={handleChange}
                      />
                    </div>
                  </div>
                </div>

                <div className="flex justify-end mt-6">
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
