
import { useCurrentUser } from '@/features/auth/api/useCurrentUser';
import { useTransactionParticipants, useTransaction } from '@/features/transactions/api/queries';
import { type ParticipantPermission } from '@/shared/api/types';

export function useParticipantPermissions(transactionId: string) {
    const { data: user } = useCurrentUser();
    const { data: participants } = useTransactionParticipants(transactionId);
    const { data: transaction } = useTransaction(transactionId);

    if (!user || !participants) {
        return {
            permissions: [],
            checkPermission: () => false,
            role: null
        };
    }

    const currentParticipant = participants.find(p => p.email === user.email);

    // Check if user is the primary broker (owner)
    const isPrimaryBroker = transaction?.brokerId === user.id;

    const permissions = currentParticipant?.permissions || [];
    // If user is primary broker, allow BROKER role even if not in participants list
    const role = isPrimaryBroker ? 'BROKER' : currentParticipant?.role;

    const checkPermission = (permission: ParticipantPermission) => {
        if (isPrimaryBroker) return true;
        if (role === 'BROKER') return true;

        if (role === 'BUYER') {
            const defaultBuyerPermissions: ParticipantPermission[] = [
                'VIEW_DOCUMENTS',
                'VIEW_PROPERTIES',
                'VIEW_STAGE',
                'VIEW_CONDITIONS'
            ];
            if (defaultBuyerPermissions.includes(permission)) return true;
        }

        if (role === 'SELLER') {
            const defaultSellerPermissions: ParticipantPermission[] = [
                'VIEW_DOCUMENTS',
                'VIEW_OFFERS',
                'VIEW_STAGE',
                'VIEW_CONDITIONS'
            ];
            if (defaultSellerPermissions.includes(permission)) return true;
        }

        if (!currentParticipant) return false;
        return permissions.includes(permission);
    };

    return {
        permissions,
        checkPermission,
        role
    };
}
