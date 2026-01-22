import { useQuery } from '@tanstack/react-query';
import { fetchClients, toClientDisplay, type ClientDisplay } from '../api/clientsApi';

export const clientKeys = {
    all: ['clients'] as const,
    list: () => [...clientKeys.all, 'list'] as const,
};

export function useClients() {
    return useQuery({
        queryKey: clientKeys.list(),
        queryFn: fetchClients,
    });
}

export function useClientsForDisplay(options?: { enabled?: boolean }) {
    return useQuery({
        queryKey: clientKeys.list(),
        queryFn: async (): Promise<ClientDisplay[]> => {
            const clients = await fetchClients();
            return clients.map(toClientDisplay);
        },
        enabled: options?.enabled,
    });
}
