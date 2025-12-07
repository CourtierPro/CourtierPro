import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Badge } from "@/shared/components/ui/badge";
import { usePasswordResetAudit } from "@/features/admin/hooks/usePasswordResetAudit";
import { formatDistanceToNow } from "date-fns";

export function PasswordResetAuditPage() {
    const { t } = useTranslation("admin");
    const { data: events, isLoading, error } = usePasswordResetAudit();

    if (isLoading) {
        return <LoadingState />;
    }

    if (error) {
        return (
            <ErrorState
                title={t("errorLoadingAudit")}
                message={t("couldNotLoadPasswordResetAudit")}
            />
        );
    }

    return (
        <div className="space-y-6">
            <PageHeader
                title="Password Reset Audit"
                subtitle="Track all password reset requests and completions"
            />

            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Email
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Event Type
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Timestamp
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                IP Address
                            </th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {events && events.length > 0 ? (
                            events.map((event) => (
                                <tr key={event.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                        {event.email}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        <Badge
                                            variant={
                                                event.eventType === "COMPLETED"
                                                    ? "default"
                                                    : "secondary"
                                            }
                                        >
                                            {event.eventType}
                                        </Badge>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {formatDistanceToNow(new Date(event.timestamp), {
                                            addSuffix: true,
                                        })}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {event.ipAddress || "N/A"}
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td
                                    colSpan={4}
                                    className="px-6 py-12 text-center text-gray-500"
                                >
                                    No password reset events found
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
