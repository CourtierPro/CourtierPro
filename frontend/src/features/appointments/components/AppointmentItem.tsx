import { Section } from "@/shared/components/branded/Section";
import { Badge } from "@/shared/components/ui/badge";
import { type Appointment, getAppointmentTimeRange } from "../types";
import { getStatusBadgeVariant } from "../enums";
import { Clock, MapPin, User } from "lucide-react";
import { useTranslation } from "react-i18next";

interface AppointmentItemProps {
    appointment: Appointment;
}

export function AppointmentItem({ appointment }: AppointmentItemProps) {
    const { t } = useTranslation('appointments');

    return (
        <Section className="p-4 hover:bg-accent/50 transition-colors cursor-pointer">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="space-y-1.5 flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                        <h3 className="font-semibold truncate">{appointment.title}</h3>
                        <Badge variant={getStatusBadgeVariant(appointment.status)} className="shrink-0">
                            {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                        </Badge>
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
                        <span className="flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5" />
                            {getAppointmentTimeRange(appointment)}
                        </span>

                        <span className="flex items-center gap-1">
                            <User className="h-3.5 w-3.5" />
                            {appointment.clientName}
                        </span>

                        {appointment.location && (
                            <span className="flex items-center gap-1 truncate max-w-[200px]">
                                <MapPin className="h-3.5 w-3.5 shrink-0" />
                                <span className="truncate">{appointment.location}</span>
                            </span>
                        )}
                    </div>
                </div>
            </div>
        </Section>
    );
}
