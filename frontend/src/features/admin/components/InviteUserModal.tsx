/**
 * InviteUserModal Component
 * 
 * Modal for inviting new users (brokers, clients, admins).
 * Uses `useInviteUser` mutation to send invitations.
 */

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
            toast.success(t("inviteSent"));
            onClose();
        } catch (err) {
            const axiosErr = err as AxiosError;
            // Removed console.error

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
                        <Input
                            id="email"
                            name="email"
                            type="email"
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
                            <Input
                                id="firstName"
                                name="firstName"
                                value={form.firstName}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="space-y-1">
                            <label htmlFor="lastName" className="text-sm font-medium">
                                {t("lastName")} *
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

                    {/* Role / Language */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label htmlFor="role" className="text-sm font-medium">
                                {t("role")}
                            </label>
                            <Select
                                value={form.role}
                                onValueChange={(value) => setForm((prev) => ({ ...prev, role: value as UserRole }))}
                            >
                                <SelectTrigger id="role" className="w-full">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="BROKER">{t("broker")}</SelectItem>
                                    <SelectItem value="CLIENT">{t("client")}</SelectItem>
                                    <SelectItem value="ADMIN">{t("admin")}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-1">
                            <label
                                htmlFor="preferredLanguage"
                                className="text-sm font-medium"
                            >
                                {t("language")}
                            </label>
                            <Select
                                value={form.preferredLanguage}
                                onValueChange={(value) => setForm((prev) => ({ ...prev, preferredLanguage: value as Language }))}
                            >
                                <SelectTrigger id="preferredLanguage" className="w-full">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="en">{t("english")}</SelectItem>
                                    <SelectItem value="fr">{t("french")}</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    {/* Boutons */}
                    <div className="mt-4 flex justify-between">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={onClose}
                            disabled={inviteUser.isPending}
                        >
                            {t("close")}
                        </Button>

                        <Button
                            type="submit"
                            disabled={inviteUser.isPending}
                        >
                            {inviteUser.isPending ? t("sending") : t("sendInvite")}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}
