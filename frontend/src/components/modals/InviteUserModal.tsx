// src/components/modals/InviteUserModal.tsx
import React, { type FormEvent, useEffect, useState } from "react";
import type { AxiosError } from "axios";

import axiosInstance from "@/api/axiosInstance";
import "./InviteUserModal.css";

type UserRole = "BROKER" | "CLIENT" | "ADMIN";
type Language = "en" | "fr";

interface InviteUserModalProps {
  open: boolean;
  onClose: () => void;
  onUserCreated?: (user: AdminUserResponse) => void;
}

interface InviteFormState {
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  preferredLanguage: Language;
}

export interface AdminUserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  preferredLanguage: string;
}

// Start with EN but will be overwritten by org settings
const defaultFormState: InviteFormState = {
  email: "",
  firstName: "",
  lastName: "",
  role: "BROKER",
  preferredLanguage: "en",
};

export function InviteUserModal({
  open,
  onClose,
  onUserCreated,
}: InviteUserModalProps) {
  const [form, setForm] = useState<InviteFormState>(defaultFormState);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load organization default language when modal opens
  useEffect(() => {
    if (open) {
      axiosInstance
        .get("/api/admin/settings")
        .then((res) => {
          const lang = res.data.defaultLanguage?.toLowerCase();
          if (lang === "fr" || lang === "en") {
            setForm((prev) => ({ ...prev, preferredLanguage: lang }));
          }
        })
        .catch((err) => console.error("Failed loading settings", err));
    } else {
      setForm(defaultFormState);
      setError(null);
    }
  }, [open]);

  if (!open) return null;

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!form.email || !form.firstName || !form.lastName) {
      setError("Please fill all required fields.");
      return;
    }

    try {
      setIsSubmitting(true);

      const response = await axiosInstance.post<AdminUserResponse>(
        "/api/admin/users",
        {
          email: form.email,
          firstName: form.firstName,
          lastName: form.lastName,
          role: form.role,
          preferredLanguage: form.preferredLanguage, // now always en or fr
        }
      );

      onUserCreated?.(response.data);
      onClose();
    } catch (err) {
      const axiosErr = err as AxiosError;

      console.error("InviteUserModal - error when creating user:", {
        message: axiosErr.message,
        status: axiosErr.response?.status,
        data: axiosErr.response?.data,
      });

      const respData = axiosErr.response?.data;

      if (typeof respData === "string") {
        setError(respData);
      } else if (respData && typeof respData === "object") {
        setError(
          (respData as { message?: string; error?: string }).message ??
            "An error occurred while sending the invitation."
        );
      } else {
        setError("An error occurred while sending the invitation.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="invite-modal-overlay">
      <div className="invite-modal">
        <header className="invite-modal-header">
          <h2 className="invite-modal-title">Invite User</h2>
          <p className="invite-modal-subtitle">
            Invite a new broker or client to your organization. An email will be
            sent with access instructions.
          </p>
        </header>

        {error && (
          <div className="invite-modal-alert invite-modal-alert--error">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="invite-modal-form">
          {/* Email */}
          <div className="invite-field-group">
            <label className="invite-field-label">Email *</label>
            <input
              name="email"
              type="email"
              className="invite-input"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>

          {/* First / Last */}
          <div className="invite-row invite-row--two-cols">
            <div className="invite-field-group">
              <label className="invite-field-label">First name *</label>
              <input
                name="firstName"
                className="invite-input"
                value={form.firstName}
                onChange={handleChange}
                required
              />
            </div>

            <div className="invite-field-group">
              <label className="invite-field-label">Last name *</label>
              <input
                name="lastName"
                className="invite-input"
                value={form.lastName}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          {/* Role / Language */}
          <div className="invite-row invite-row--two-cols">
            <div className="invite-field-group">
              <label className="invite-field-label">Role</label>
              <select
                name="role"
                className="invite-select"
                value={form.role}
                onChange={handleChange}
              >
                <option value="BROKER">Broker</option>
                <option value="CLIENT">Client</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <div className="invite-field-group">
              <label className="invite-field-label">Language</label>
              <select
                name="preferredLanguage"
                className="invite-select"
                value={form.preferredLanguage}
                onChange={handleChange}
              >
                <option value="en">English</option>
                <option value="fr">Français</option>
              </select>
            </div>
          </div>

          {/* Actions */}
          <div className="invite-modal-actions">
            <button
              type="button"
              className="invite-link-button"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Close
            </button>

            <button
              type="submit"
              className="admin-primary-btn invite-submit-btn"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Sending…" : "Send invite"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
