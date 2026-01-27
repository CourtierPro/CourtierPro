import { useGetOutstandingDocuments } from "@/features/documents/api/queries";
import { useSendDocumentReminder } from "@/features/documents/api/mutations";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { Bell, AlertCircle, CheckCircle } from "lucide-react";
import { useTranslation } from "react-i18next";
import { format, parseISO, isPast } from "date-fns";
import { toast } from "sonner";
import { type OutstandingDocumentDTO } from "../api/documentsApi";
import { cn } from "@/shared/utils/utils";

export function OutstandingDocumentsDashboard() {
    const { t } = useTranslation('documents');
    const { data: outstandingDocs, isLoading } = useGetOutstandingDocuments();
    const { mutate: remind, isPending: isReminding } = useSendDocumentReminder();

    const hasDocuments = outstandingDocs && outstandingDocs.length > 0;

    if (isLoading) return null; // Or a skeleton

    // Group by transaction
    const groupedDocs = (outstandingDocs || []).reduce((acc: Record<string, OutstandingDocumentDTO[]>, doc: OutstandingDocumentDTO) => {
        const key = doc.transactionAddress;
        if (!acc[key]) {
            acc[key] = [];
        }
        acc[key].push(doc);
        return acc;
    }, {} as Record<string, OutstandingDocumentDTO[]>);

    const handleRemind = (docId: string) => {
        remind(docId, {
            onSuccess: () => {
                toast.success(t('reminderSent', 'Reminder sent successfully'));
            },
            onError: () => {
                toast.error(t('reminderFailed', 'Failed to send reminder'));
            }
        });
    };

    // Sort documents within groups by urgency (dueDate asc, then daysOutstanding desc)
    Object.keys(groupedDocs).forEach(key => {
        groupedDocs[key].sort((a: OutstandingDocumentDTO, b: OutstandingDocumentDTO) => {
            // If both have due dates, sort by due date (earliest/overdue first)
            if (a.dueDate && b.dueDate) {
                return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
            }
            // If only one has due date, it comes first
            if (a.dueDate) return -1;
            if (b.dueDate) return 1;

            // Fallback: days outstanding (descending - older requests first)
            return (b.daysOutstanding || 0) - (a.daysOutstanding || 0);
        });
    });

    // Sort groups by the urgency of their most urgent document
    const sortedGroups = Object.entries(groupedDocs).sort(([, docsA], [, docsB]) => {
        const firstA = docsA[0]; // Guaranteed to be most urgent due to sort above
        const firstB = docsB[0];

        // Similar comparison logic for groups
        if (firstA.dueDate && firstB.dueDate) {
            return new Date(firstA.dueDate).getTime() - new Date(firstB.dueDate).getTime();
        }
        if (firstA.dueDate) return -1;
        if (firstB.dueDate) return 1;

        return (firstB.daysOutstanding || 0) - (firstA.daysOutstanding || 0);
    });

    return (
        <Card className={cn("h-full", hasDocuments && "border-orange-200 bg-orange-50/30 dark:bg-orange-950/10")}>
            <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                    <AlertCircle className={cn("w-5 h-5", hasDocuments ? "text-orange-600" : "text-muted-foreground")} />
                    <CardTitle className={cn("text-lg font-semibold", hasDocuments ? "text-orange-900 dark:text-orange-100" : "")}>
                        {t('outstandingDocuments', 'Outstanding Documents')}
                    </CardTitle>
                </div>
                <CardDescription>
                    {t('outstandingDocumentsDesc', 'Review and remind clients about overdue documents.')}
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                {(!outstandingDocs || outstandingDocs.length === 0) ? (
                    <div className="flex flex-col items-center justify-center py-8 text-center">
                        <CheckCircle className="h-10 w-10 text-green-500/50 mb-2" />
                        <p className="text-sm text-muted-foreground">
                            {t('noOutstandingDocuments', "No outstanding documents. You're all caught up!")}
                        </p>
                    </div>
                ) : (
                    sortedGroups.map(([address, docs]) => (
                        <div key={address} className="space-y-2">
                            <h4 className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                                <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
                                {address}
                            </h4>
                            <div className="space-y-2 pl-3 border-l-2 border-slate-100 dark:border-slate-800">
                                {docs.map((doc: OutstandingDocumentDTO) => {
                                    const isOverdue = doc.dueDate && isPast(parseISO(doc.dueDate));
                                    const isDueToday = doc.daysOutstanding === 0;

                                    return (
                                        <div key={doc.id} className="flex items-center justify-between p-3 bg-white dark:bg-card border rounded-lg shadow-sm">
                                            <div className="flex items-center gap-3">
                                                <Badge
                                                    variant={isOverdue ? "destructive" : "secondary"}
                                                    className="h-6"
                                                >
                                                    {isDueToday
                                                        ? t('dueToday', 'Due Today')
                                                        : `${doc.daysOutstanding} ${t('days', 'days')}`
                                                    }
                                                </Badge>
                                                <div>
                                                    <p className="font-medium text-sm">
                                                        {t(`types.${doc.title}`, { defaultValue: doc.title })}
                                                    </p>
                                                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                                        <span>{doc.clientName}</span>
                                                        {doc.dueDate && (
                                                            <>
                                                                <span>•</span>
                                                                <span className={isOverdue ? "text-destructive dark:text-red-400" : ""}>
                                                                    {t('due', 'Due')}: {format(parseISO(doc.dueDate), 'MMM d')}
                                                                </span>
                                                            </>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                            <Button
                                                size="sm"
                                                variant="ghost"
                                                className="gap-2 text-muted-foreground hover:text-primary"
                                                onClick={() => handleRemind(doc.id)}
                                                disabled={isReminding}
                                            >
                                                <Bell className="w-4 h-4" />
                                                {t('remind', 'Remind')}
                                            </Button>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))
                )}
            </CardContent>
        </Card>
    );
}
