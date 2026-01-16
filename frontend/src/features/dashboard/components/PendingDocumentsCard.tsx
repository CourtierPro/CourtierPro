import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { FileText, ArrowRight, CheckCircle } from "lucide-react";
import { usePendingDocuments, type PendingDocument } from "@/features/dashboard/api/queries";
import { cn } from "@/shared/utils/utils";
import { formatDistanceToNow } from "date-fns";

interface PendingDocumentsCardProps {
    className?: string;
    maxItems?: number;
}

export function PendingDocumentsCard({ className, maxItems = 5 }: PendingDocumentsCardProps) {
    const { t } = useTranslation("dashboard");
    const navigate = useNavigate();
    const { data: documents, isLoading, error } = usePendingDocuments();

    const displayDocuments = documents?.slice(0, maxItems) ?? [];

    const formatDocumentType = (type: string) => {
        return type
            .replace(/_/g, " ")
            .toLowerCase()
            .replace(/\b\w/g, (c) => c.toUpperCase());
    };

    const getTimeAgo = (dateString: string | null) => {
        if (!dateString) return "";
        try {
            return formatDistanceToNow(new Date(dateString), { addSuffix: true });
        } catch {
            return "";
        }
    };

    const handleDocumentClick = (doc: PendingDocument) => {
        // Navigate to transaction details with documents tab and focus on this document
        navigate(`/transactions/${doc.transactionId}?tab=documents&focus=${doc.requestId}`);
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.priorityCards.pendingDocuments.title")}
                className={className}
            >
                <div className="space-y-3">
                    {[1, 2, 3].map((i) => (
                        <Skeleton key={i} className="h-16 w-full" />
                    ))}
                </div>
            </Section>
        );
    }

    if (error) {
        return (
            <Section
                title={t("broker.priorityCards.pendingDocuments.title")}
                className={className}
            >
                <div className="text-sm text-destructive">
                    {t("broker.priorityCards.error")}
                </div>
            </Section>
        );
    }

    return (
        <Section
            title={t("broker.priorityCards.pendingDocuments.title")}
            description={t("broker.priorityCards.pendingDocuments.description")}
            className={className}
            action={
                documents && documents.length > maxItems && (
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => navigate("/documents?status=SUBMITTED")}
                    >
                        {t("broker.priorityCards.viewAll")}
                        <ArrowRight className="ml-1 h-4 w-4" />
                    </Button>
                )
            }
        >
            {displayDocuments.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                    <CheckCircle className="h-10 w-10 text-green-500/50 mb-2" />
                    <p className="text-sm text-muted-foreground">
                        {t("broker.priorityCards.pendingDocuments.empty")}
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {displayDocuments.map((doc) => (
                        <button
                            key={doc.requestId}
                            onClick={() => handleDocumentClick(doc)}
                            className={cn(
                                "w-full flex items-center justify-between p-3 rounded-lg",
                                "bg-muted/50 hover:bg-muted transition-colors",
                                "text-left focus:outline-none focus:ring-2 focus:ring-primary"
                            )}
                        >
                            <div className="flex items-center gap-3 flex-1 min-w-0">
                                <div className="p-2 rounded-md bg-primary/10 flex-shrink-0">
                                    <FileText className="h-4 w-4 text-primary" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="font-medium text-sm truncate">
                                        {doc.customTitle || formatDocumentType(doc.documentType)}
                                    </div>
                                    <div className="text-xs text-muted-foreground truncate">
                                        {doc.clientName}
                                        {doc.propertyAddress && (
                                            <span> • {doc.propertyAddress}</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                                <Badge variant="info" className="text-xs">
                                    {t("broker.priorityCards.pendingDocuments.review")}
                                </Badge>
                                {doc.submittedAt && (
                                    <span className="text-xs text-muted-foreground">
                                        {getTimeAgo(doc.submittedAt)}
                                    </span>
                                )}
                            </div>
                        </button>
                    ))}
                </div>
            )}
        </Section>
    );
}
