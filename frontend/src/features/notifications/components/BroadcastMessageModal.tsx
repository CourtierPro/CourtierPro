
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { Loader2, Megaphone } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/shared/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/shared/components/ui/form";
import { Input } from "@/shared/components/ui/input";
import { Textarea } from "@/shared/components/ui/textarea";

import { useSendBroadcast } from "../api/notificationsApi";

interface BroadcastFormValues {
    title: string;
    message: string;
}

interface BroadcastMessageModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function BroadcastMessageModal({
    open,
    onOpenChange,
}: BroadcastMessageModalProps) {
    const { t } = useTranslation("notifications");
    const { mutate: sendBroadcast, isPending } = useSendBroadcast();

    const form = useForm<BroadcastFormValues>({
        defaultValues: {
            title: "",
            message: "",
        },
    });

    const onSubmit = (values: BroadcastFormValues) => {
        sendBroadcast(values, {
            onSuccess: () => {
                toast.success(t("broadcast.success", "Broadcast sent successfully"));
                form.reset();
                onOpenChange(false);
            },
            onError: (error) => {
                toast.error(t("broadcast.error", "Failed to send broadcast"));
                console.error("Broadcast error:", error);
            },
        });
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Megaphone className="h-5 w-5" />
                        {t("broadcast.title", "Send Broadcast Message")}
                    </DialogTitle>
                    <DialogDescription>
                        {t(
                            "broadcast.description",
                            "This message will be sent to all users in the system. Use carefully."
                        )}
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <FormField
                            control={form.control}
                            name="title"
                            rules={{
                                required: t("broadcast.form.titleRequired", "Title is required"),
                                maxLength: {
                                    value: 100,
                                    message: t("broadcast.form.titleMaxLength", "Max 100 characters"),
                                },
                            }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>{t("broadcast.form.title", "Title")}</FormLabel>
                                    <FormControl>
                                        <Input placeholder="System Maintenance" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="message"
                            rules={{
                                required: t("broadcast.form.messageRequired", "Message is required"),
                                maxLength: {
                                    value: 500,
                                    message: t("broadcast.form.messageMaxLength", "Max 500 characters"),
                                },
                            }}
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>{t("broadcast.form.message", "Message")}</FormLabel>
                                    <FormControl>
                                        <Textarea
                                            placeholder="The system will be undergoing maintenance..."
                                            className="min-h-[120px]"
                                            {...field}
                                        />
                                    </FormControl>
                                    <FormDescription>
                                        {(field.value || "").length}/500
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => onOpenChange(false)}
                                disabled={isPending}
                            >
                                {t("common.cancel", "Cancel")}
                            </Button>
                            <Button type="submit" disabled={isPending}>
                                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                {t("broadcast.send", "Send Broadcast")}
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
