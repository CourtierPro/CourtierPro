import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { Calendar, ArrowRight, Clock, MapPin } from "lucide-react";
import { useAppointments, type Appointment } from "@/features/appointments/api/queries";
import { cn } from "@/shared/utils/utils";
import { format, parseISO, isToday, isTomorrow, addDays, isBefore } from "date-fns";

interface UpcomingAppointmentsCardProps {
    className?: string;
    maxItems?: number;
    daysAhead?: number;
}

export function UpcomingAppointmentsCard({
    className,
    maxItems = 5,
    daysAhead = 2
}: UpcomingAppointmentsCardProps) {
    const { t } = useTranslation("dashboard");
    const navigate = useNavigate();
    const { data: appointments, isLoading, error } = useAppointments();

    // Filter to upcoming appointments only (within daysAhead)
    const cutoffDate = addDays(new Date(), daysAhead);
    const upcomingAppointments = (appointments ?? [])
        .filter((apt) => {
            try {
                const aptDate = parseISO(apt.date);
                return !isBefore(aptDate, new Date()) && isBefore(aptDate, cutoffDate);
            } catch {
                return false;
            }
        })
        .sort((a, b) => {
            const dateA = parseISO(a.date);
            const dateB = parseISO(b.date);
            return dateA.getTime() - dateB.getTime();
        })
        .slice(0, maxItems);

    const getAppointmentTypeIcon = (type: Appointment['type']) => {
        const icons: Record<Appointment['type'], string> = {
            inspection: "🔍",
            notary: "📝",
            showing: "🏠",
            consultation: "💬",
            walkthrough: "🚶",
            meeting: "🤝",
        };
        return icons[type] || "📅";
    };

    const formatDateLabel = (dateString: string) => {
        try {
            const date = parseISO(dateString);
            if (isToday(date)) return t("broker.priorityCards.upcomingAppointments.today");
            if (isTomorrow(date)) return t("broker.priorityCards.upcomingAppointments.tomorrow");
            return format(date, "EEE, MMM d");
        } catch {
            return dateString;
        }
    };

    const handleAppointmentClick = (apt: Appointment) => {
        navigate(`/appointments?id=${apt.id}`);
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.priorityCards.upcomingAppointments.title")}
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
                title={t("broker.priorityCards.upcomingAppointments.title")}
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
            title={t("broker.priorityCards.upcomingAppointments.title")}
            description={t("broker.priorityCards.upcomingAppointments.description")}
            className={className}
            action={
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate("/appointments")}
                >
                    {t("broker.priorityCards.viewAll")}
                    <ArrowRight className="ml-1 h-4 w-4" />
                </Button>
            }
        >
            {upcomingAppointments.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                    <Calendar className="h-10 w-10 text-muted-foreground/50 mb-2" />
                    <p className="text-sm text-muted-foreground">
                        {t("broker.priorityCards.upcomingAppointments.empty")}
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {upcomingAppointments.map((apt) => (
                        <button
                            key={apt.id}
                            onClick={() => handleAppointmentClick(apt)}
                            className={cn(
                                "w-full flex items-center justify-between p-3 rounded-lg",
                                "bg-muted/50 hover:bg-muted transition-colors",
                                "text-left focus:outline-none focus:ring-2 focus:ring-primary"
                            )}
                        >
                            <div className="flex items-center gap-3 flex-1 min-w-0">
                                <div className="text-xl flex-shrink-0">
                                    {getAppointmentTypeIcon(apt.type)}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="font-medium text-sm capitalize">
                                        {apt.type}
                                    </div>
                                    <div className="text-xs text-muted-foreground truncate">
                                        {apt.clientName}
                                        {apt.transactionAddress && (
                                            <span className="flex items-center gap-1 mt-0.5">
                                                <MapPin className="h-3 w-3" />
                                                {apt.transactionAddress}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div className="flex flex-col items-end gap-1 ml-2 flex-shrink-0">
                                <Badge
                                    variant={isToday(parseISO(apt.date)) ? "warning" : "secondary"}
                                    className="text-xs"
                                >
                                    {formatDateLabel(apt.date)}
                                </Badge>
                                <span className="text-xs text-muted-foreground flex items-center gap-1">
                                    <Clock className="h-3 w-3" />
                                    {apt.time}
                                </span>
                            </div>
                        </button>
                    ))}
                </div>
            )}
        </Section>
    );
}
