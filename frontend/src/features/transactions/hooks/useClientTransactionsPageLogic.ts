import { useAuth0 } from '@auth0/auth0-react';
import { useClientTransactions } from '../api/queries';

export function useClientTransactionsPageLogic() {
    const { user } = useAuth0();
    // Use authenticated user's ID from Auth0 token subject
    const clientId = user?.sub ?? "";

    const { data: transactions, isLoading, error } = useClientTransactions(clientId);

    return {
        transactions,
        isLoading,
        error,
    };
}
