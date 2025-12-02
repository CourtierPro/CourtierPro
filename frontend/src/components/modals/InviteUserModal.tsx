// src/components/modals/InviteUserModal.tsx
import React, { type FormEvent, useEffect, useState } from "react";
import type { AxiosError } from "axios";

import axiosInstance from "@/api/axiosInstance";

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

export function InviteUserModal({open, onClose, onUserCreated,}: InviteUserModalProps) {
    const [form, setForm] = useState<InviteFormState>(defaultFormState);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!open) {
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
                    preferredLanguage: form.preferredLanguage,
                }
            );

            const createdUser = response.data;

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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="w-full max-w-xl rounded-lg bg-white p-6 shadow-lg">
                <h2 className="mb-2 text-xl font-semibold">Invite User</h2>
                <p className="mb-4 text-sm text-muted-foreground">
                    Invite a new broker or client to your organization. An email will be
                    sent with access instructions.
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
                            Email *
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
                                First name *
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
                                Last name *
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
                                Role
                            </label>
                            <select
                                id="role"
                                name="role"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.role}
                                onChange={handleChange}
                            >
                                <option value="BROKER">Broker</option>
                                <option value="CLIENT">Client</option>
                                <option value="ADMIN">Admin</option>
                            </select>
                        </div>

                        <div className="space-y-1">
                            <label
                                htmlFor="preferredLanguage"
                                className="text-sm font-medium"
                            >
                                Language
                            </label>
                            <select
                                id="preferredLanguage"
                                name="preferredLanguage"
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                value={form.preferredLanguage}
                                onChange={handleChange}
                            >
                                <option value="en">English</option>
                                <option value="fr">Français</option>
                            </select>
                        </div>
                    </div>

                    {/* Boutons */}
                    <div className="mt-4 flex justify-between">
                        <button
                            type="button"
                            className="text-sm text-orange-600 hover:underline"
                            onClick={onClose}
                            disabled={isSubmitting}
                        >
                            Close
                        </button>

                        <button
                            type="submit"
                            className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-60"
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
