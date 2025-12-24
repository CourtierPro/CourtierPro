import { useEffect } from "react";
import type { AxiosError } from "axios";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

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
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/shared/components/ui/form";

import axiosInstance from "@/shared/api/axiosInstance";
import { useInviteUser } from "@/features/admin/api/mutations";
import { inviteUserSchema, type InviteUserFormValues } from "@/shared/schemas";

interface InviteUserModalProps {
  open: boolean;
  onClose: () => void;
  onUserCreated?: (user: AdminUserResponse) => void;
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

type Language = "en" | "fr";

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
  const inviteUser = useInviteUser();

  const form = useForm<InviteUserFormValues>({
    resolver: zodResolver(inviteUserSchema),
    defaultValues: {
      email: "",
      firstName: "",
      lastName: "",
      role: "BROKER",
      preferredLanguage: "en",
    },
  });

  const { setValue } = form;

  useEffect(() => {
    axiosInstance
      .get("/api/admin/settings")
      .then((res) => {
        const lang = res.data?.defaultLanguage?.toLowerCase();
        if (lang === "en" || lang === "fr") {
          setValue("preferredLanguage", lang as Language);
        }
      })
      .catch(() => {
        toast.error(t("inviteUser_settingsLoadError"));
      });
  }, [t, setValue]);

  const onSubmit = async (data: InviteUserFormValues) => {
    try {
      const createdUser = await inviteUser.mutateAsync({
        email: data.email,
        firstName: data.firstName,
        lastName: data.lastName,
        role: data.role,
        preferredLanguage: data.preferredLanguage,
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

      toast.error(errorMessage);
    }
  };

  return (
    <>
      <DialogHeader>
        <DialogTitle>{t("inviteUserTitle")}</DialogTitle>
        <DialogDescription>{t("inviteUserDesc")}</DialogDescription>
      </DialogHeader>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          {/* Email */}
          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t("inviteUser_emailLabel")} *</FormLabel>
                <FormControl>
                  <Input {...field} type="email" />
                </FormControl>
                <FormMessage>{form.formState.errors.email?.message && t(form.formState.errors.email?.message)}</FormMessage>
              </FormItem>
            )}
          />

          {/* First / last name */}
          <div className="grid grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="firstName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("inviteUser_firstNameLabel")} *</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage>{form.formState.errors.firstName?.message && t(form.formState.errors.firstName?.message)}</FormMessage>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("inviteUser_lastNameLabel")} *</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage>{form.formState.errors.lastName?.message && t(form.formState.errors.lastName?.message)}</FormMessage>
                </FormItem>
              )}
            />
          </div>

          {/* Role / language */}
          <div className="grid grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("inviteUser_roleLabel")}</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
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
                  <FormMessage>{form.formState.errors.role?.message && t(form.formState.errors.role?.message)}</FormMessage>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="preferredLanguage"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("inviteUser_languageLabel")}</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="en">
                        {t("inviteUser_language_en")}
                      </SelectItem>
                      <SelectItem value="fr">
                        {t("inviteUser_language_fr")}
                      </SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage>{form.formState.errors.preferredLanguage?.message && t(form.formState.errors.preferredLanguage?.message)}</FormMessage>
                </FormItem>
              )}
            />
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
      </Form>
    </>
  );
}
