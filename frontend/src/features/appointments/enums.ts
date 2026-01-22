/**
 * Appointment status enum matching backend.
 */
export type AppointmentStatus = 'PROPOSED' | 'CONFIRMED' | 'DECLINED' | 'CANCELLED';

/**
 * Appointment initiator type enum matching backend.
 */
export type InitiatorType = 'BROKER' | 'CLIENT';

/**
 * Get badge variant for appointment status.
 */
export function getStatusBadgeVariant(status: AppointmentStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
        case 'CONFIRMED':
            return 'default';
        case 'PROPOSED':
            return 'secondary';
        case 'DECLINED':
        case 'CANCELLED':
            return 'destructive';
        default:
            return 'outline';
    }
}

/**
 * Get color class for appointment status (for calendar dots, etc.).
 */
export function getStatusColorClass(status: AppointmentStatus): string {
    switch (status) {
        case 'CONFIRMED':
            return 'bg-green-500';
        case 'PROPOSED':
            return 'bg-gray-500';
        case 'DECLINED':
            return 'bg-red-500';
        case 'CANCELLED':
            return 'bg-gray-400';
        default:
            return 'bg-blue-500';
    }
}
