import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";
import { useTranslation } from "react-i18next";
import { type Appointment, getAppointmentTimeRange, getAppointmentDate } from "../types";
import { format } from "date-fns";
import { fr, enUS } from 'date-fns/locale';
import { Calendar, Clock, MapPin, User, FileText } from "lucide-react";
import { Badge } from "@/shared/components/ui/badge";
import { getStatusBadgeVariant } from "../enums";
import { useAuth0 } from "@auth0/auth0-react";
import { Section } from "@/shared/components/branded/Section";

interface AppointmentDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    appointment: Appointment | null;
}

export function AppointmentDetailModal({ isOpen, onClose, appointment }: AppointmentDetailModalProps) {
    const { t, i18n } = useTranslation('appointments');
    const { user } = useAuth0();

    if (!appointment) return null;

    const userRoles = (user?.['https://courtierpro.dev/roles'] as string[]) || [];
    const isBroker = userRoles.includes('BROKER');

    // Logic to determine other party name
    // If I am broker, show client name. If I am client, show broker name.
    const otherPartyName = isBroker ? appointment.clientName : appointment.brokerName;
    const otherPartyLabel = isBroker ? t('client') : t('broker');

    return (
        <Dialog open={isOpen} onOpenChange={(val) => !val && onClose()}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <div className="flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-primary/10">
                            <Calendar className="w-6 h-6 text-primary" />
                        </div>
                        <DialogTitle>{t(appointment.title.toLowerCase(), appointment.title)}</DialogTitle>
                    </div>
                </DialogHeader>

                <div className="space-y-6 pt-4">
                    {/* Status Badge */}
                    <div className="flex justify-between items-center">
                        <span className="text-sm text-muted-foreground">{t('statusLabel', 'Status')}</span>
                        <Badge variant={getStatusBadgeVariant(appointment.status)}>
                            {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                        </Badge>
                    </div>

                    {/* Date and Time */}
                    <Section className="p-4 space-y-3 bg-muted/30">
                        <div className="flex items-center gap-3">
                            <Calendar className="w-4 h-4 text-muted-foreground" />
                            <span className="font-medium">
                                {format(getAppointmentDate(appointment), 'EEEE, d MMMM yyyy', { locale: i18n.language === 'fr' ? fr : enUS })}
                            </span>
                        </div>
                        <div className="flex items-center gap-3">
                            <Clock className="w-4 h-4 text-muted-foreground" />
                            <span>{getAppointmentTimeRange(appointment)}</span>
                        </div>
                    </Section>

                    {/* Participants */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                {otherPartyLabel}
                            </p>
                            <div className="flex items-center gap-2">
                                <User className="w-4 h-4 text-primary" />
                                <span className="font-medium">{otherPartyName}</span>
                            </div>
                        </div>
                        {/* Location */}
                        {appointment.location && (
                            <div className="space-y-1">
                                <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                    {t('location', 'Location')}
                                </p>
                                <div className="flex items-center gap-2">
                                    <MapPin className="w-4 h-4 text-primary" />
                                    <span className="font-medium truncate" title={appointment.location}>
                                        {appointment.location}
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Transaction Reference (if any) */}
                    {appointment.transactionId && (
                        <div className="space-y-1 pt-2 border-t border-border/50">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider">
                                {t('transaction')}
                            </p>
                            <p className="text-sm text-muted-foreground font-mono">
                                {appointment.transactionId}
                            </p>
                        </div>
                    )}

                    {/* Notes/Message */}
                    {appointment.notes && (
                        <div className="space-y-2 pt-2 border-t border-border/50">
                            <p className="text-xs text-muted-foreground uppercase font-semibold tracking-wider flex items-center gap-2">
                                <FileText className="w-3 h-3" />
                                {t('notes', 'Notes')}
                            </p>
                            <p className="text-sm text-foreground/80 bg-muted p-3 rounded-lg italic">
                                "{appointment.notes}"
                            </p>
                        </div>
                    )}
                </div>
            </DialogContent>
        </Dialog>
    );
}
