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
        <div className={`flex justify-between ${compact ? 'gap-2' : 'gap-3'}`}>
            {/* Left Content: Title, Details */}
            <div className="flex flex-col gap-1 overflow-hidden min-w-0 flex-1">
                {/* Title */}
                <h3 className={`font-semibold text-foreground truncate ${compact ? 'text-sm' : 'text-base'}`}>
                    {t(appointment.title.toLowerCase(), appointment.title)}
                </h3>

                {/* Line 1: Time | Proposed By */}
                <div className={`flex flex-wrap items-center gap-x-2 gap-y-1 text-muted-foreground ${compact ? 'text-[10px]' : 'text-sm'}`}>
                    <div className="flex items-center gap-1.5 shrink-0">
                        <Clock className="h-3.5 w-3.5 shrink-0" />
                        <span className="truncate">{getAppointmentTimeRange(appointment)}</span>
                    </div>

                    <div className="hidden sm:inline-block w-px h-3 bg-border/60"></div>

                    <div className={`flex items-center gap-1.5 truncate ${isMe ? "" : "text-primary font-medium"}`}>
                        {isMe ? <ArrowUpRight className="h-3.5 w-3.5 shrink-0" /> : <ArrowDownLeft className="h-3.5 w-3.5 shrink-0" />}
                        <span className="truncate">{label}</span>
                    </div>
                </div>

                {/* Line 2: Name | Location */}
                <div className={`flex flex-wrap items-center gap-x-2 gap-y-1 text-muted-foreground ${compact ? 'text-[10px]' : 'text-sm'}`}>
                    <div className="flex items-center gap-1.5 shrink-0">
                        <User className="h-3.5 w-3.5 shrink-0" />
                        <span className="truncate">{isBrokerEffective ? appointment.clientName : appointment.brokerName}</span>
                    </div>

                    {appointment.location && (
                        <>
                            <div className="hidden sm:inline-block w-px h-3 bg-border/60"></div>
                            <div className="flex items-center gap-1.5 shrink-0 max-w-[150px]">
                                <MapPin className="h-3.5 w-3.5 shrink-0" />
                                <span className="truncate">{appointment.location}</span>
                            </div>
                        </>
                    )}
                </div>
            </div>

            {/* Right Content: Status, Notes, Actions */}
            <div className="flex flex-col items-end gap-1 shrink-0">
                <div className="flex items-center gap-2">
                    {appointment.notes && (
                        <Badge
                            variant="outline"
                            className="text-[10px] bg-amber-50 text-amber-700 border-amber-200 px-1.5 py-0 h-5 gap-1"
                        >
                            <StickyNote className="h-3 w-3" />
                            {!compact && t('note', 'Note')}
                        </Badge>
                    )}

                    <Badge
                        variant="outline"
                        className={`border-0 text-white ${getStatusColorClass(appointment.status)} ${compact ? 'text-[10px] px-1.5 py-0 h-5' : 'text-xs px-2.5 py-0.5'}`}
                    >
                        {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                    </Badge>
                </div>

                {canReview && (
                    <Button
                        size="sm"
                        className={`h-6 bg-primary text-primary-foreground hover:bg-primary/90 rounded-full ${compact ? 'text-[10px] px-2 h-5' : 'text-xs px-3'}`}
                        onClick={(e) => {
                            e.stopPropagation();
                            onClick?.(appointment);
                        }}
                    >
                        <Check className="w-3 h-3 mr-1" />
                        {t('review', 'Review')}
                    </Button>
                )}
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
