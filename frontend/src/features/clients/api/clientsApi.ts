import axiosInstance from '@/shared/api/axiosInstance';

export interface Client {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    active: boolean;
    preferredLanguage: string;
}

export interface ClientDisplay {
    id: string;
    name: string;
    email: string;
}

export const fetchClients = async (): Promise<Client[]> => {
    const response = await axiosInstance.get<Client[]>('/api/broker/clients');
    return response.data;
};

export const toClientDisplay = (client: Client): ClientDisplay => {
    const hasName = client.firstName || client.lastName;
    const name = hasName
        ? `${client.firstName ?? ''} ${client.lastName ?? ''}`.trim()
        : client.email?.split('@')[0] ?? 'Unknown';
    return {
        id: client.id,
        name,
        email: client.email,
    };
};
