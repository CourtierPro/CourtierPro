import { Section } from "@/shared/components/branded/Section";
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
                <span className="inline-block px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                    {appointment.status}
                </span>
            </div>
        </Section>
    );
}
