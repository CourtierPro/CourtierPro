import { AppointmentItem } from "@/features/appointments/components/AppointmentItem";
import { type Appointment } from "@/features/appointments/api/queries";

interface AppointmentListProps {
    appointments: Appointment[];
}

export function AppointmentList({ appointments }: AppointmentListProps) {
    return (
        <div className="grid gap-4">
            {appointments.map((apt) => (
                <AppointmentItem key={apt.id} appointment={apt} />
            ))}
        </div>
    );
}
