import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

import api from '@/shared/api/axiosInstance';
import { useSearchContext } from '@/features/search/context/useSearchContext';

export type SearchResultType = 'TRANSACTION' | 'DOCUMENT' | 'USER' | 'PAGE';

export interface SearchResult {
    id: string;
    type: SearchResultType;
    title: string;
    subtitle: string;
    url: string;
}

const STATIC_ROUTES: SearchResult[] = [
    { id: 'dashboard', type: 'PAGE', title: 'Dashboard', subtitle: 'Go to main dashboard', url: '/' },
    { id: 'profile', type: 'PAGE', title: 'Profile', subtitle: 'Manage your account', url: '/profile' },
    { id: 'settings', type: 'PAGE', title: 'Settings', subtitle: 'App preferences', url: '/settings' },
    { id: 'transactions', type: 'PAGE', title: 'Transactions', subtitle: 'View all transactions', url: '/transactions' },
    { id: 'documents', type: 'PAGE', title: 'Documents', subtitle: 'View all documents', url: '/documents' },
    { id: 'users', type: 'PAGE', title: 'Contacts', subtitle: 'Manage contacts', url: '/contacts' },
];

export function useGlobalSearch() {
    const { query, setQuery, isOpen, setIsOpen } = useSearchContext();
    const navigate = useNavigate();

    // Backend Search
    const { data: backendResults = [], isLoading } = useQuery({
        queryKey: ['globalSearch', query],
        queryFn: async () => {
            if (!query || query.length < 2) return [];
            const response = await api.get<SearchResult[]>('/api/search', { params: { q: query } });
            return response.data;
        },
        enabled: isOpen && query.length >= 2,
        staleTime: 1000 * 60 * 1, // 1 minute
    });

    // Client-side Static Search
    const staticResults = useMemo(() => {
        if (!query) return [];
        const lowerQuery = query.toLowerCase();
        return STATIC_ROUTES.filter(
            (route) =>
                route.title.toLowerCase().includes(lowerQuery) ||
                route.subtitle.toLowerCase().includes(lowerQuery)
        );
    }, [query]);

    const results = useMemo(() => {
        return [...staticResults, ...backendResults];
    }, [staticResults, backendResults]);

    const handleSelect = (result: SearchResult) => {
        setIsOpen(false);
        navigate(result.url);
    };

    return {
        query,
        setQuery,
        isOpen,
        setIsOpen,
        results,
        isLoading,
        handleSelect,
    };
}
