import { useClientTransactions } from '../api/queries';

export function useClientTransactionsPageLogic() {
    // TODO: Replace with real client ID from Auth0/Context
    const clientId = "CLIENT123";

    const { data: transactions, isLoading, error } = useClientTransactions(clientId);

    return {
        transactions,
        isLoading,
        error,
    };
}
