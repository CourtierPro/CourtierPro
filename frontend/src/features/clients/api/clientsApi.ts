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

export const toClientDisplay = (client: Client): ClientDisplay => ({
    id: client.id,
    name: `${client.firstName} ${client.lastName}`,
    email: client.email,
});
