// src/pages/admin/AdminSettingsPage.tsx
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";

import { InviteUserModal } from "@/features/admin/components/InviteUserModal";

import {
  getOrganizationSettings,
  updateOrganizationSettings,
} from "./adminSettingsApi";

import type { UpdateOrganizationSettingsRequest } from "./adminSettingsApi";

import { Globe, Mail } from "lucide-react";

import "./AdminSettingsPage.css";

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
      } catch (err) {
        console.error(err);
        setError(t("settings.errors.loadFailed"));
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, [t]);

  // Handle form changes
  function handleChange(e: any) {
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
    } catch (err) {
      console.error(err);
      setError(t("settings.errors.saveFailed"));
    } finally {
      setIsSaving(false);
    }
  }

  const isDisabled = isSaving || isLoading || !form;

  return (
    <>
      <div className="admin-settings-page">
        <div className="admin-settings-inner">
          {/* HEADER */}
          <div className="admin-settings-header">
            <h1 className="admin-settings-title">{t("settings.title")}</h1>
            <p className="admin-settings-subtitle">
              {t("settings.subtitle")}
            </p>
          </div>

          {/* ALERTS */}
          {error && (
            <div className="admin-settings-alert admin-settings-alert--error">
              {error}
            </div>
          )}

          {saveMessage && (
            <div className="admin-settings-alert admin-settings-alert--success">
              {saveMessage}
            </div>
          )}

          {/* LAYOUT */}
          <div className="admin-settings-layout">
            {/* DEFAULT LANGUAGE CARD */}
            <div className="admin-settings-card">
              <h2 className="admin-settings-card-title">
                <Globe />
                {t("settings.defaultLanguageCard.title")}
              </h2>

              <p className="admin-settings-card-description">
                {t("settings.defaultLanguageCard.description")}
              </p>

              <form>
                <div className="admin-settings-field-group">
                  <label className="admin-settings-field-label">
                    {t("settings.defaultLanguageCard.label")}
                  </label>

                  <select
                    name="defaultLanguage"
                    value={form?.defaultLanguage ?? ""}
                    className="admin-settings-select"
                    disabled={isDisabled}
                    onChange={handleChange}
                  >
                    <option value="en">{t("settings.languages.en")}</option>
                    <option value="fr">{t("settings.languages.fr")}</option>
                  </select>
                </div>
              </form>
            </div>

            {/* EMAIL TEMPLATE CARD */}
            <div className="admin-settings-card">
              <h2 className="admin-settings-card-title">
                <Mail />
                {t("settings.templatesCard.title")}
              </h2>

              <p className="admin-settings-card-description">
                {t("settings.templatesCard.description")}{" "}
                <span className="admin-settings-pill">
                  {"{{name}}"}
                </span>
              </p>

              <form onSubmit={handleSubmit}>
                {/* 2 columns */}
                <div className="admin-settings-templates-grid">
                  {/* English column */}
                  <div className="admin-settings-template-column">
                    <h3 className="admin-settings-template-title">
                      {t("settings.templatesCard.englishTitle")}
                    </h3>

                    <div className="admin-settings-field-group">
                      <label className="admin-settings-field-label">
                        {t("settings.templatesCard.subject")}
                      </label>
                      <input
                        type="text"
                        name="inviteSubjectEn"
                        className="admin-settings-input"
                        disabled={isDisabled}
                        value={form?.inviteSubjectEn ?? ""}
                        onChange={handleChange}
                      />
                    </div>

                    <div className="admin-settings-field-group">
                      <label className="admin-settings-field-label">
                        {t("settings.templatesCard.body")}
                      </label>
                      <textarea
                        name="inviteBodyEn"
                        className="admin-settings-textarea"
                        disabled={isDisabled}
                        value={form?.inviteBodyEn ?? ""}
                        onChange={handleChange}
                      />
                    </div>
                  </div>

                  {/* French column */}
                  <div className="admin-settings-template-column">
                    <h3 className="admin-settings-template-title">
                      {t("settings.templatesCard.frenchTitle")}
                    </h3>

                    <div className="admin-settings-field-group">
                      <label className="admin-settings-field-label">
                        {t("settings.templatesCard.subjectFr")}
                      </label>
                      <input
                        type="text"
                        name="inviteSubjectFr"
                        className="admin-settings-input"
                        disabled={isDisabled}
                        value={form?.inviteSubjectFr ?? ""}
                        onChange={handleChange}
                      />
                    </div>

                    <div className="admin-settings-field-group">
                      <label className="admin-settings-field-label">
                        {t("settings.templatesCard.bodyFr")}
                      </label>
                      <textarea
                        name="inviteBodyFr"
                        className="admin-settings-textarea"
                        disabled={isDisabled}
                        value={form?.inviteBodyFr ?? ""}
                        onChange={handleChange}
                      />
                    </div>
                  </div>
                </div>

                <div className="admin-settings-actions">
                  <button
                    type="submit"
                    disabled={isDisabled}
                    className="admin-settings-saveButton"
                  >
                    {t("settings.saveButton")}
                  </button>
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
