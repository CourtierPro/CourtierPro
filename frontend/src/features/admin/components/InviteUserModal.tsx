import React, { type FormEvent, useEffect, useState } from "react";
import type { AxiosError } from "axios";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";

import axiosInstance from "@/shared/api/axiosInstance";
import { useInviteUser } from "@/features/admin/api/mutations";

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

// Start with EN (backend/organization settings can still override if needed)
const defaultFormState: InviteFormState = {
  email: "",
  firstName: "",
  lastName: "",
  role: "BROKER",
  preferredLanguage: "en",
};

export function InviteUserModal(props: InviteUserModalProps) {
  return (
    <Dialog open={props.open} onOpenChange={(val) => !val && props.onClose()}>
      <DialogContent className="sm:max-w-xl">
        <InviteUserForm {...props} />
      </DialogContent>
    </Dialog>
  );
}

function InviteUserForm({ onClose, onUserCreated }: InviteUserModalProps) {
  const { t } = useTranslation("admin");
  const [form, setForm] = useState<InviteFormState>(defaultFormState);
  const [error, setError] = useState<string | null>(null);

  const inviteUser = useInviteUser();

  useEffect(() => {
    axiosInstance
      .get("/api/admin/settings")
      .then((res) => {
        const lang = res.data?.defaultLanguage?.toLowerCase();
        if (lang === "en" || lang === "fr") {
          setForm((prev) => ({
            ...prev,
            preferredLanguage: lang as Language,
          }));
        }
      })
      .catch(() => {
        toast.error(t("inviteUser_settingsLoadError"));
      });
  }, [t]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
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
      setError(t("inviteUser_fillAllFields"));
      return;
    }

    try {
      const createdUser = await inviteUser.mutateAsync({
        email: form.email,
        firstName: form.firstName,
        lastName: form.lastName,
        role: form.role,
        preferredLanguage: form.preferredLanguage,
      });

      onUserCreated?.(createdUser);
      toast.success(t("inviteUser_inviteSent"));
      onClose();
    } catch (err) {
      const axiosErr = err as AxiosError;
      const respData = axiosErr.response?.data;
      let errorMessage = t("inviteUser_inviteError");

      if (typeof respData === "string") {
        errorMessage = respData;
      } else if (respData && typeof respData === "object") {
        const dataObj = respData as { message?: string; error?: string };
        errorMessage =
          dataObj.message ??
          dataObj.error ??
          t("inviteUser_inviteError");
      }

      setError(errorMessage);
      toast.error(errorMessage);
    }
  };

  return (
    <>
      <DialogHeader>
        <DialogTitle>{t("inviteUserTitle")}</DialogTitle>
        <DialogDescription>{t("inviteUserDesc")}</DialogDescription>
      </DialogHeader>

      {error && (
        <div className="mb-3 rounded-md bg-destructive/10 px-3 py-2 text-xs text-destructive">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Email */}
        <div className="space-y-2">
          <label
            htmlFor="email"
            className="text-sm font-medium text-foreground"
          >
            {t("inviteUser_emailLabel")} *
          </label>
          <Input
            id="email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            required
          />
        </div>

        {/* First / last name */}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label
              htmlFor="firstName"
              className="text-sm font-medium text-foreground"
            >
              {t("inviteUser_firstNameLabel")} *
            </label>
            <Input
              id="firstName"
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              required
            />
          </div>

          <div className="space-y-2">
            <label
              htmlFor="lastName"
              className="text-sm font-medium text-foreground"
            >
              {t("inviteUser_lastNameLabel")} *
            </label>
            <Input
              id="lastName"
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        {/* Role / language */}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label
              htmlFor="role"
              className="text-sm font-medium text-foreground"
            >
              {t("inviteUser_roleLabel")}
            </label>
            <Select
              value={form.role}
              onValueChange={(value) =>
                setForm((prev) => ({ ...prev, role: value as UserRole }))
              }
            >
              <SelectTrigger id="role" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="BROKER">
                  {t("inviteUser_role_BROKER")}
                </SelectItem>
                <SelectItem value="CLIENT">
                  {t("inviteUser_role_CLIENT")}
                </SelectItem>
                <SelectItem value="ADMIN">
                  {t("inviteUser_role_ADMIN")}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label
              htmlFor="preferredLanguage"
              className="text-sm font-medium text-foreground"
            >
              {t("inviteUser_languageLabel")}
            </label>
            <Select
              value={form.preferredLanguage}
              onValueChange={(value) =>
                setForm((prev) => ({
                  ...prev,
                  preferredLanguage: value as Language,
                }))
              }
            >
              <SelectTrigger
                id="preferredLanguage"
                className="w-full"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="en">
                  {t("inviteUser_language_en")}
                </SelectItem>
                <SelectItem value="fr">
                  {t("inviteUser_language_fr")}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Actions */}
        <div className="mt-6 flex justify-between gap-3">
          <Button
            type="button"
            variant="ghost"
            onClick={onClose}
            disabled={inviteUser.isPending}
          >
            {t("inviteUser_close")}
          </Button>

          <Button type="submit" disabled={inviteUser.isPending}>
            {inviteUser.isPending
              ? t("inviteUser_sending")
              : t("inviteUser_sendInvite")}
          </Button>
        </div>
      </form>
    </>
  );
}
