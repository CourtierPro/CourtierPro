/**
 * InviteUserModal Component
 * 
 * Modal for inviting new users (brokers, clients, admins).
 * Uses `useInviteUser` mutation to send invitations.
 */

import React, { type FormEvent, useEffect, useState } from "react";
import type { AxiosError } from "axios";
import { useTranslation } from "react-i18next";

import { useInviteUser } from '@/features/admin/api/mutations';

type UserRole = "BROKER" | "CLIENT" | "ADMIN";

type Language = "en" | "fr";

interface InviteUserModalProps {
    open: boolean; // true = rthe modal is shown
    onClose: () => void;
    onUserCreated?: (user: AdminUserResponse) => void; // optionnal callback
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

const defaultFormState: InviteFormState = {
    email: "",
    firstName: "",
    lastName: "",
    role: "BROKER",
    preferredLanguage: "en",
};

export function InviteUserModal({ open, onClose, onUserCreated, }: InviteUserModalProps) {
    const { t } = useTranslation("admin");
    const [form, setForm] = useState<InviteFormState>(defaultFormState);

    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!open) {
            setForm(defaultFormState);
            setError(null);
        }
    }, [open]);

    const inviteUser = useInviteUser();

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
            setError(t("fillAllFields"));
            return;
        }

        try {
            const createdUser = await inviteUser.mutateAsync({
                email: form.email,
                firstName: form.firstName,
                lastName: form.lastName,
                role: form.role,
            });

            onUserCreated?.(createdUser);
            onClose();
        } catch (err) {
            const axiosErr = err as AxiosError;

            console.error("InviteUserModal - error when creating user:", {
                message: axiosErr.message,
                status: axiosErr.response?.status,
                data: axiosErr.response?.data,
                headers: axiosErr.response?.headers,
            });

            const respData = axiosErr.response?.data;

            if (typeof respData === "string") {
                setError(respData);
            } else if (respData && typeof respData === "object") {
                const dataObj = respData as { message?: string; error?: string };
                setError(
                    dataObj.message ??
                    dataObj.error ??
                    t("inviteError")
                );
            } else {
                setError(t("inviteError"));
            }
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="w-full max-w-xl rounded-lg bg-white p-6 shadow-lg">
                <h2 className="mb-2 text-xl font-semibold">{t("inviteUserTitle")}</h2>
                <p className="mb-4 text-sm text-muted-foreground">
                    {t("inviteUserDesc")}
                </p>

                {error && (
                    <div className="mb-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-700">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-3">
                    {/* Email */}
                    <div className="space-y-1">
                        <label htmlFor="email" className="text-sm font-medium">
                            {t("email")} *
                        </label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            value={form.email}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    {/* First / Last name */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label htmlFor="firstName" className="text-sm font-medium">
                                {t("firstName")} *
                            </label>
                            <input
                                id="firstName"
                                name="firstName"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.firstName}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="space-y-1">
                            <label htmlFor="lastName" className="text-sm font-medium">
                                {t("lastName")} *
                            </label>
                            <input
                                id="lastName"
                                name="lastName"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.lastName}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    {/* Role / Language */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label htmlFor="role" className="text-sm font-medium">
                                {t("role")}
                            </label>
                            <select
                                id="role"
                                name="role"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.role}
                                onChange={handleChange}
                            >
                                <option value="BROKER">{t("broker")}</option>
                                <option value="CLIENT">{t("client")}</option>
                                <option value="ADMIN">{t("admin")}</option>
                            </select>
                        </div>

                        <div className="space-y-1">
                            <label
                                htmlFor="preferredLanguage"
                                className="text-sm font-medium"
                            >
                                {t("language")}
                            </label>
                            <select
                                id="preferredLanguage"
                                name="preferredLanguage"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.preferredLanguage}
                                onChange={handleChange}
                            >
                                <option value="en">{t("english")}</option>
                                <option value="fr">{t("french")}</option>
                            </select>
                        </div>
                    </div>

                    {/* Boutons */}
                    <div className="mt-4 flex justify-between">
                        <button
                            type="button"
                            className="text-sm text-orange-600 hover:underline"
                            onClick={onClose}
                            disabled={inviteUser.isPending}
                        >
                            {t("close")}
                        </button>

                        <button
                            type="submit"
                            className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-60"
                            disabled={inviteUser.isPending}
                        >
                            {inviteUser.isPending ? t("sending") : t("sendInvite")}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
