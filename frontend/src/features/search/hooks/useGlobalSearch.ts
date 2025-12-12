import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

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

export function useGlobalSearch() {
    const { t } = useTranslation('common'); // Added useTranslation hook
    const { query, setQuery, isOpen, setIsOpen } = useSearchContext();
    const navigate = useNavigate();

    // Moved STATIC_ROUTES inside the hook and translated
    const staticRoutes: SearchResult[] = useMemo(() => [
        { id: 'dashboard', type: 'PAGE', title: t('nav.dashboard', 'Dashboard'), subtitle: t('nav.home', 'Home'), url: '/dashboard' },
        { id: 'profile', type: 'PAGE', title: t('nav.profile', 'Profile'), subtitle: t('nav.manageAccount', 'Manage your account'), url: '/profile' },
        { id: 'settings', type: 'PAGE', title: t('nav.settings', 'Settings'), subtitle: t('nav.appPreferences', 'App preferences'), url: '/settings' },
        { id: 'transactions', type: 'PAGE', title: t('nav.transactions', 'Transactions'), subtitle: t('nav.list', 'List'), url: '/transactions' },
        { id: 'documents', type: 'PAGE', title: t('nav.documents', 'Documents'), subtitle: t('nav.viewAllDocuments', 'View all documents'), url: '/documents' },
        { id: 'contacts', type: 'PAGE', title: t('nav.contacts', 'Contacts'), subtitle: t('nav.directory', 'Directory'), url: '/contacts' },
    ], [t]); // Dependency on t for re-evaluation if language changes

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
        const normalizedQuery = query.toLowerCase();
        // Filter static pages
        const pageResults = staticRoutes.filter(page =>
            page.title.toLowerCase().includes(normalizedQuery) ||
            page.subtitle.toLowerCase().includes(normalizedQuery)
        );
        return pageResults;
    }, [query, staticRoutes]); // Added staticRoutes as a dependency

    const results = useMemo(() => {
        return [...staticResults, ...backendResults];
    }, [staticResults, backendResults]);

    const handleSelect = (result: SearchResult) => {
        setIsOpen(false);
        setQuery('');
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
