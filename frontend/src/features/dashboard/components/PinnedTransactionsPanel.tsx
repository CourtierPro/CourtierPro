import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { Pin, ArrowRight, Home, ShoppingCart } from "lucide-react";
import { usePinnedTransactions, type PinnedTransaction } from "@/features/dashboard/api/queries";
import { cn } from "@/shared/utils/utils";

interface PinnedTransactionsPanelProps {
    className?: string;
}

export function PinnedTransactionsPanel({ className }: PinnedTransactionsPanelProps) {
    const { t } = useTranslation("dashboard");
    const navigate = useNavigate();
    const { data: pinned, isLoading, error } = usePinnedTransactions();

    const getSideIcon = (side: PinnedTransaction['side']) => {
        return side === 'BUY_SIDE'
            ? <ShoppingCart className="h-4 w-4" />
            : <Home className="h-4 w-4" />;
    };

    const getSideLabel = (side: PinnedTransaction['side']) => {
        return side === 'BUY_SIDE'
            ? t("broker.pinnedTransactions.buying")
            : t("broker.pinnedTransactions.selling");
    };

    const formatStage = (stage: string) => {
        if (!stage) return "";
        return stage
            .replace(/_/g, " ")
            .toLowerCase()
            .replace(/\b\w/g, (c) => c.toUpperCase());
    };

    const handleTransactionClick = (tx: PinnedTransaction) => {
        navigate(`/transactions/${tx.transactionId}`);
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.pinnedTransactions.title")}
                className={className}
            >
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {[1, 2, 3].map((i) => (
                        <Skeleton key={i} className="h-24 w-full" />
                    ))}
                </div>
            </Section>
        );
    }

    if (error) {
        return (
            <Section
                title={t("broker.pinnedTransactions.title")}
                className={className}
            >
                <div className="text-sm text-destructive">
                    {t("broker.priorityCards.error")}
                </div>
            </Section>
        );
    }

    if (!pinned || pinned.length === 0) {
        return null;
    }

    return (
        <Section
            title={t("broker.pinnedTransactions.title")}
            description={t("broker.pinnedTransactions.description")}
            className={className}
            action={
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate("/transactions?pinned=true")}
                >
                    {t("broker.priorityCards.viewAll")}
                    <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
            }
        >
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {pinned.map((tx) => (
                    <button
                        key={tx.transactionId}
                        onClick={() => handleTransactionClick(tx)}
                        className={cn(
                            "flex flex-col p-4 rounded-lg border",
                            "bg-gradient-to-br from-primary/5 to-transparent",
                            "hover:from-primary/10 hover:border-primary/30",
                            "transition-all duration-200",
                            "text-left focus:outline-none focus:ring-2 focus:ring-primary"
                        )}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                                <Pin className="h-3.5 w-3.5 text-primary" />
                                <span className="text-xs text-muted-foreground flex items-center gap-1">
                                    {getSideIcon(tx.side)}
                                    {getSideLabel(tx.side)}
                                </span>
                            </div>
                            <Badge
                                variant={tx.status === 'ACTIVE' ? 'success' : 'secondary'}
                                className="text-xs px-2 py-0.5"
                            >
                                {tx.status}
                            </Badge>
                        </div>

                        <div className="font-medium text-sm truncate mb-1">
                            {tx.propertyAddress || t("broker.priorityCards.noAddress")}
                        </div>

                        <div className="text-xs text-muted-foreground truncate">
                            {tx.clientName}
                        </div>

                        {tx.currentStage && (
                            <div className="mt-2 pt-2 border-t border-border/50">
                                <span className="text-xs text-muted-foreground">
                                    {formatStage(tx.currentStage)}
                                </span>
                            </div>
                        )}
                    </button>
                ))}
            </div>
        </Section>
    );
}
