import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { AlertTriangle, Clock, ArrowRight } from "lucide-react";
import { useExpiringOffers, type ExpiringOffer } from "@/features/dashboard/api/queries";
import { cn } from "@/shared/utils/utils";

interface ExpiringOffersCardProps {
    className?: string;
    maxItems?: number;
}

export function ExpiringOffersCard({ className, maxItems = 5 }: ExpiringOffersCardProps) {
    const { t } = useTranslation("dashboard");
    const navigate = useNavigate();
    const { data: offers, isLoading, error } = useExpiringOffers();

    const displayOffers = offers?.slice(0, maxItems) ?? [];

    const getUrgencyColor = (days: number) => {
        if (days <= 1) return "destructive";
        if (days <= 3) return "warning";
        return "secondary";
    };

    const formatCurrency = (amount: number) => {
        return new Intl.NumberFormat("en-CA", {
            style: "currency",
            currency: "CAD",
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const handleOfferClick = (offer: ExpiringOffer) => {
        navigate(`/transactions/${offer.transactionId}?tab=offers`);
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.priorityCards.expiringOffers.title")}
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
                title={t("broker.priorityCards.expiringOffers.title")}
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
            title={t("broker.priorityCards.expiringOffers.title")}
            description={t("broker.priorityCards.expiringOffers.description")}
            className={className}
            action={
                offers && offers.length > maxItems && (
                    <Button variant="ghost" size="sm" onClick={() => navigate("/transactions")}>
                        {t("broker.priorityCards.viewAll")}
                        <ArrowRight className="ml-1 h-4 w-4" />
                    </Button>
                )
            }
        >
            {displayOffers.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                    <Clock className="h-10 w-10 text-muted-foreground/50 mb-2" />
                    <p className="text-sm text-muted-foreground">
                        {t("broker.priorityCards.expiringOffers.empty")}
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {displayOffers.map((offer) => (
                        <button
                            key={offer.offerId}
                            onClick={() => handleOfferClick(offer)}
                            className={cn(
                                "w-full flex items-center justify-between p-3 rounded-lg",
                                "bg-muted/50 hover:bg-muted transition-colors",
                                "text-left focus:outline-none focus:ring-2 focus:ring-primary"
                            )}
                        >
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                    {offer.daysUntilExpiry <= 1 && (
                                        <AlertTriangle className="h-4 w-4 text-destructive flex-shrink-0" />
                                    )}
                                    <span className="font-medium text-sm truncate">
                                        {offer.propertyAddress || t("broker.priorityCards.noAddress")}
                                    </span>
                                </div>
                                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                    <span>{offer.clientName}</span>
                                    <span>•</span>
                                    <span>
                                        {formatCurrency(offer.offerAmount)}
                                    </span>
                                </div>
                            </div>
                            <Badge
                                variant={getUrgencyColor(offer.daysUntilExpiry)}
                                className="ml-2 flex-shrink-0"
                            >
                                {offer.daysUntilExpiry === 0
                                    ? t("broker.priorityCards.expiringOffers.today")
                                    : offer.daysUntilExpiry === 1
                                        ? t("broker.priorityCards.expiringOffers.tomorrow")
                                        : t("broker.priorityCards.expiringOffers.daysLeft", { days: offer.daysUntilExpiry })
                                }
                            </Badge>
                        </button>
                    ))}
                </div>
            )}
        </Section>
    );
}
