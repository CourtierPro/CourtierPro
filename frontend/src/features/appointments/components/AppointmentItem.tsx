import { Section } from "@/shared/components/branded/Section";
import { Badge } from "@/shared/components/ui/badge";
import { type Appointment } from "@/features/appointments/api/queries";

interface AppointmentItemProps {
    appointment: Appointment;
}

export function AppointmentItem({ appointment }: AppointmentItemProps) {
    return (
        <Section className="flex justify-between items-center p-4">
            <div>
                <h3 className="font-semibold">{appointment.type}</h3>
                <p className="text-sm text-muted-foreground">{appointment.date} at {appointment.time}</p>
                <p className="text-sm">{appointment.clientName}</p>
            </div>
            <div className="text-right">
                <Badge variant="secondary">
                    {appointment.status}
                </Badge>
            </div>
        </Section>
    );
}
