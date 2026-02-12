import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import api from '@/shared/api/axiosInstance';
import { useSearchContext } from '@/features/search/context/useSearchContext';

export type SearchResultType = 'TRANSACTION' | 'DOCUMENT' | 'USER' | 'PAGE' | 'APPOINTMENT' | 'ADDRESS';

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
        { id: 'dashboard', type: 'PAGE', title: t('navigation.dashboard', 'Dashboard'), subtitle: t('navigation.home', 'Home'), url: '/dashboard' },
        { id: 'profile', type: 'PAGE', title: t('navigation.profile', 'Profile'), subtitle: t('navigation.manageAccount', 'Manage your account'), url: '/profile' },
        { id: 'settings', type: 'PAGE', title: t('navigation.settings', 'Settings'), subtitle: t('navigation.appPreferences', 'App preferences'), url: '/settings' },
        { id: 'transactions', type: 'PAGE', title: t('navigation.transactions', 'Transactions'), subtitle: t('navigation.list', 'List'), url: '/transactions' },
        { id: 'documents', type: 'PAGE', title: t('navigation.documents', 'Documents'), subtitle: t('navigation.viewAllDocuments', 'View all documents'), url: '/documents' },
        { id: 'contacts', type: 'PAGE', title: t('navigation.contacts', 'Contacts'), subtitle: t('navigation.directory', 'Directory'), url: '/contacts' },
        { id: 'appointments', type: 'PAGE', title: t('navigation.appointments', 'Appointments'), subtitle: t('navigation.calendar', 'Calendar'), url: '/appointments' },
        { id: 'analytics', type: 'PAGE', title: t('navigation.analytics', 'Analytics'), subtitle: t('navigation.reports', 'Reports & Insights'), url: '/analytics' },
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
