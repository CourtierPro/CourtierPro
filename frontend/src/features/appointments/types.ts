import type { AppointmentStatus, InitiatorType } from './enums';

/**
 * Appointment response from the backend API.
 */
export interface Appointment {
    appointmentId: string;
    title: string;
    transactionId: string | null;
    brokerId: string;
    brokerName: string;
    clientId: string;
    clientName: string;
    fromDateTime: string; // ISO datetime string
    toDateTime: string; // ISO datetime string
    status: AppointmentStatus;
    initiatedBy: InitiatorType;
    location: string | null;
    latitude: number | null;
    longitude: number | null;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
}

/**
 * Filter parameters for fetching appointments.
 */
export interface AppointmentFilters {
    from?: string; // ISO datetime
    to?: string; // ISO datetime
    status?: AppointmentStatus;
}

/**
 * View mode for the appointments page.
 */
export type AppointmentViewMode = 'calendar' | 'list';

/**
 * Helper to get display-friendly date from Appointment.
 */
export function getAppointmentDate(appointment: Appointment): Date {
    return new Date(appointment.fromDateTime);
}

/**
 * Helper to get display-friendly time range.
 */
export function getAppointmentTimeRange(appointment: Appointment): string {
    const start = new Date(appointment.fromDateTime);
    const end = new Date(appointment.toDateTime);
    const formatTime = (d: Date) =>
        d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return `${formatTime(start)} - ${formatTime(end)}`;
}

/**
 * Group appointments by date for list view.
 */
export function groupAppointmentsByDate(
    appointments: Appointment[]
): Map<string, Appointment[]> {
    const groups = new Map<string, Appointment[]>();

    for (const apt of appointments) {
        const date = new Date(apt.fromDateTime);
        const dateKey = date.toISOString().split('T')[0]; // YYYY-MM-DD

        if (!groups.has(dateKey)) {
            groups.set(dateKey, []);
        }
        groups.get(dateKey)!.push(apt);
    }

    return groups;
}
