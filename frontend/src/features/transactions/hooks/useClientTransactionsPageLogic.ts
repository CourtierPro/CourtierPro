import { useCurrentUser } from '@/features/auth/api/useCurrentUser';
import { useClientTransactions } from '../api/queries';

export function useClientTransactionsPageLogic() {
    const { data: currentUser, isLoading: isLoadingUser } = useCurrentUser();

    // Use the internal UUID from /api/me endpoint
    const clientId = currentUser?.id ?? "";

    const {
        data: transactions,
        isLoading: isLoadingTransactions,
        error,
        refetch,
    } = useClientTransactions(clientId);

    return {
        transactions,
        isLoading: isLoadingUser || isLoadingTransactions,
        error,
        refetch,
    };
}
