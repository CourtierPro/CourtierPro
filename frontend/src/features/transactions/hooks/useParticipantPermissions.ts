
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

    // Check if user is the transaction's client
    const isTransactionClient = transaction?.clientId === user.id;

    const permissions = currentParticipant?.permissions || [];

    // Determine role: primary broker > transaction client > participant role
    let role = currentParticipant?.role;
    if (isPrimaryBroker) {
        role = 'BROKER';
    } else if (isTransactionClient) {
        // Client of this transaction - treat as BUYER for buy-side, SELLER for sell-side
        role = transaction?.side === 'BUY_SIDE' ? 'BUYER' : 'SELLER';
    }

    const checkPermission = (permission: ParticipantPermission) => {
        if (isPrimaryBroker) return true;
        if (role === 'BROKER') return true;

        if (role === 'BUYER') {
            const defaultBuyerPermissions: ParticipantPermission[] = [
                'VIEW_DOCUMENTS',
                'VIEW_PROPERTIES',
                'VIEW_STAGE',
                'VIEW_CONDITIONS',
                'VIEW_SEARCH_CRITERIA',
                'EDIT_SEARCH_CRITERIA'
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
