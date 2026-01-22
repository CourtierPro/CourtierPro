import { AppointmentItem } from "./AppointmentItem";
import { type Appointment } from "../types";
import { format, parseISO } from "date-fns";

interface AppointmentListProps {
    groupedAppointments: Map<string, Appointment[]>;
}

import { useState } from "react";
import { AppointmentDetailModal } from "./AppointmentDetailModal";

import { useAuth0 } from "@auth0/auth0-react";

export function AppointmentList({ groupedAppointments }: AppointmentListProps) {
    const { user } = useAuth0();
    const isBroker = user?.['https://courtierpro.dev/roles']?.includes('BROKER') ?? false;
    const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

    // Sort dates chronologically
    const sortedDates = Array.from(groupedAppointments.keys()).sort();

    if (sortedDates.length === 0) {
        return null;
    }

    return (
        <div className="space-y-6">
            {sortedDates.map((dateKey) => {
                const appointments = groupedAppointments.get(dateKey) || [];
                const date = parseISO(dateKey);

                return (
                    <div key={dateKey} className="space-y-3">
                        <h3 className="font-semibold text-lg sticky top-0 bg-background py-2">
                            {format(date, 'EEEE, MMMM d, yyyy')}
                        </h3>
                        <div className="grid gap-3">
                            {appointments.map((apt) => (
                                <AppointmentItem
                                    key={apt.appointmentId}
                                    appointment={apt}
                                    onClick={setSelectedAppointment}
                                    isBroker={isBroker}
                                />
                            ))}
                        </div>
                    </div>
                );
            })}

            <AppointmentDetailModal
                isOpen={!!selectedAppointment}
                onClose={() => setSelectedAppointment(null)}
                appointment={selectedAppointment}
            />
        </div>
    );
}
