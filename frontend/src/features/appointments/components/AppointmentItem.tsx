import { Section } from "@/shared/components/branded/Section";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { type Appointment, getAppointmentTimeRange } from "../types";
import { getStatusColorClass } from "../enums";
import { Clock, MapPin, User, ArrowUpRight, ArrowDownLeft, StickyNote, Check } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth0 } from "@auth0/auth0-react";

interface AppointmentItemProps {
    appointment: Appointment;
    onClick?: (appointment: Appointment) => void;
    compact?: boolean;
    isBroker?: boolean; // Added as optional
}

export function AppointmentItem({ appointment, onClick, compact = false, isBroker }: AppointmentItemProps) {
    const { t } = useTranslation('appointments');
    const { user } = useAuth0();

    // Determine if the current user is a broker, if the prop 'isBroker' is not provided.
    const userRoles = (user?.['https://courtierpro.dev/roles'] as string[]) || [];
    const isCurrentUserBroker = userRoles.includes('BROKER');

    // Use the 'isBroker' prop if provided, otherwise fall back to the Auth0 user role check.
    const isBrokerEffective = isBroker !== undefined ? isBroker : isCurrentUserBroker;

    // Determine if "I" (the current user) initiated the appointment
    // If I am a broker, and initiatedBy is BROKER -> I initiated it.
    // If I am a client, and initiatedBy is CLIENT -> I initiated it.
    const isMe = (isBrokerEffective && appointment.initiatedBy === 'BROKER') ||
        (!isBrokerEffective && appointment.initiatedBy === 'CLIENT');

    const label = isMe
        ? t('proposedByYou')
        : isBrokerEffective
            ? t('proposedByClient', { name: appointment.clientName })
            : t('proposedByBroker', { name: appointment.brokerName });

    const canReview = appointment.status === 'PROPOSED' && !isMe;

    const content = (
        <div className={`flex ${compact ? 'flex-col gap-2' : 'flex-col sm:flex-row sm:items-center justify-between gap-3'}`}>
            <div className="space-y-1.5 flex-1 min-w-0">
                <div className="flex items-center gap-2 justify-between w-full">
                    <h3 className="font-semibold truncate">{t(appointment.title.toLowerCase(), appointment.title)}</h3>
                    <div className="flex flex-col items-end gap-2 shrink-0">
                        <div className="flex items-center gap-2">
                            {appointment.notes && (
                                <Badge
                                    variant="outline"
                                    className="text-[10px] bg-amber-50 text-amber-700 border-amber-200 px-1.5 py-0 h-5 gap-1"
                                >
                                    <StickyNote className="h-3 w-3" />
                                    {t('note', 'Note')}
                                </Badge>
                            )}
                            <Badge
                                variant="outline"
                                className={`text-xs border-0 text-white ${getStatusColorClass(appointment.status)}`}
                            >
                                {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                            </Badge>
                        </div>
                        {canReview && (
                            <Button
                                size="sm"
                                className="h-7 text-xs bg-primary text-primary-foreground border border-transparent hover:bg-white hover:text-primary hover:border-primary gap-1.5 px-3 rounded-full transition-all duration-200"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onClick?.(appointment);
                                }}
                            >
                                <Check className="w-3.5 h-3.5" />
                                {t('review', 'Review Request')}
                            </Button>
                        )}
                    </div>
                </div>

                <div className={`flex flex-wrap items-center ${compact ? 'gap-x-3 gap-y-1 text-xs' : 'gap-x-4 gap-y-1 text-sm'} text-muted-foreground`}>
                    <span className="flex items-center gap-1">
                        <Clock className="h-3.5 w-3.5" />
                        {getAppointmentTimeRange(appointment)}
                    </span>

                    <span className="flex items-center gap-1">
                        <User className="h-3.5 w-3.5" />
                        {isBrokerEffective ? appointment.clientName : appointment.brokerName}
                    </span>

                    {appointment.location && (
                        <span className="flex items-center gap-1 truncate max-w-[200px]">
                            <MapPin className="h-3.5 w-3.5 shrink-0" />
                            <span className="truncate">{appointment.location}</span>
                        </span>
                    )}

                    <span className={`flex items-center gap-1 font-medium text-primary`}>
                        {isMe ? <ArrowUpRight className="h-3.5 w-3.5" /> : <ArrowDownLeft className="h-3.5 w-3.5" />}
                        {label}
                    </span>
                </div>
            </div>
        </div>
    );

    const bgClass = !isMe ? "bg-orange-50/50 dark:bg-orange-900/10 border-orange-100 dark:border-orange-900/20" : "bg-card hover:bg-accent/50";

    if (compact) {
        return (
            <div
                className={`p-3 border rounded-lg transition-colors cursor-pointer text-card-foreground ${bgClass} ${!isMe ? 'hover:bg-orange-100/50 dark:hover:bg-orange-900/20' : ''}`}
                onClick={() => onClick?.(appointment)}
            >
                {content}
            </div>
        );
    }

    return (
        <Section
            className={`transition-colors cursor-pointer ${bgClass} ${!isMe ? 'hover:bg-orange-100/50 dark:hover:bg-orange-900/20' : ''}`}
            onClick={() => onClick?.(appointment)}
        >
            {content}
        </Section>
    );
}
