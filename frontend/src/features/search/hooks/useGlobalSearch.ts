import { useMemo } from 'react';
import { useUserProfile } from '@/features/profile';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import api from '@/shared/api/axiosInstance';
import { useSearchContext } from '@/features/search/context/useSearchContext';

export type SearchResultType = 'TRANSACTION' | 'DOCUMENT' | 'USER' | 'PAGE' | 'APPOINTMENT';

export interface SearchResult {
    id: string;
    type: SearchResultType;
    title: string;
    subtitle: string;
    url: string;
}

export function useGlobalSearch() {
    const { t } = useTranslation('common');
    const { query, setQuery, isOpen, setIsOpen } = useSearchContext();
    const navigate = useNavigate();
    const { data: user } = useUserProfile();
    const isBroker = user?.role === 'BROKER';

    // Static routes
    const staticRoutes: SearchResult[] = useMemo(() => {
        let routes: SearchResult[] = [
            { id: 'dashboard', type: 'PAGE', title: t('navigation.dashboard', 'Dashboard'), subtitle: t('navigation.home', 'Home'), url: '/dashboard' },
            { id: 'transactions', type: 'PAGE', title: t('navigation.transactions', 'Transactions'), subtitle: t('navigation.list', 'List'), url: '/transactions' },
            { id: 'documents', type: 'PAGE', title: t('navigation.documents', 'Documents'), subtitle: t('navigation.viewAllDocuments', 'View all documents'), url: '/documents' },
            { id: 'appointments', type: 'PAGE', title: t('navigation.appointments', 'Appointments'), subtitle: t('navigation.calendar', 'Calendar'), url: '/appointments' },
            { id: 'analytics', type: 'PAGE', title: t('navigation.analytics', 'Analytics'), subtitle: t('navigation.reports', 'Reports & Insights'), url: '/analytics' },
        ];
        if (isBroker) {
            routes = [
                { id: 'clients', type: 'PAGE', title: t('navigation.clients', 'Clients'), subtitle: t('navigation.directory', 'Directory'), url: '/clients' },
                { id: 'notifications', type: 'PAGE', title: t('navigation.notifications', 'Notifications'), subtitle: t('navigation.notifications', 'Notifications'), url: '/notifications' },
                ...routes.filter(r => r.id !== 'profile' && r.id !== 'settings' && r.id !== 'contacts')
            ];
        } else if (user?.role === 'CLIENT') {
            routes = [
                { id: 'dashboard', type: 'PAGE', title: t('navigation.dashboard', 'Dashboard'), subtitle: t('navigation.home', 'Home'), url: '/dashboard/client' },
                { id: 'my-transactions', type: 'PAGE', title: t('navigation.transactions', 'Transactions'), subtitle: t('navigation.list', 'List'), url: '/my-transaction' },
                { id: 'my-documents', type: 'PAGE', title: t('navigation.documents', 'Documents'), subtitle: t('navigation.viewAllDocuments', 'View all documents'), url: '/my-documents' },
                { id: 'appointments', type: 'PAGE', title: t('navigation.appointments', 'Appointments'), subtitle: t('navigation.calendar', 'Calendar'), url: '/appointments' },
                { id: 'analytics', type: 'PAGE', title: t('navigation.analytics', 'Analytics'), subtitle: t('navigation.reports', 'Reports & Insights'), url: '/analytics' },
                { id: 'notifications', type: 'PAGE', title: t('navigation.notifications', 'Notifications'), subtitle: t('navigation.notifications', 'Notifications'), url: '/notifications' },
            ];
        } else if (user?.role === 'ADMIN') {
            routes = [
                { id: 'admin-dashboard', type: 'PAGE', title: t('navigation.dashboard', 'Dashboard'), subtitle: t('navigation.home', 'Home'), url: '/dashboard/admin' },
                { id: 'admin-notifications', type: 'PAGE', title: t('navigation.notifications', 'Notifications'), subtitle: t('navigation.notifications', 'Notifications'), url: '/notifications' },
                { id: 'admin-users', type: 'PAGE', title: t('navigation.manageUsers', 'Manage Users'), subtitle: t('navigation.users', 'Users'), url: '/admin/users' },
                { id: 'admin-settings', type: 'PAGE', title: t('navigation.settings', 'Settings'), subtitle: t('navigation.appPreferences', 'App preferences'), url: '/admin/settings' },
                { id: 'admin-resources', type: 'PAGE', title: t('navigation.resources', 'Resources'), subtitle: t('navigation.resources', 'Resources'), url: '/admin/resources' },
                { id: 'admin-login-audit', type: 'PAGE', title: t('navigation.loginAudit', 'Login Audit'), subtitle: t('navigation.loginAudit', 'Login Audit'), url: '/admin/login-audit' },
                { id: 'admin-system-logs', type: 'PAGE', title: t('navigation.systemLogs', 'System Logs'), subtitle: t('navigation.systemLogs', 'System Logs'), url: '/admin/system-logs' },
            ];
        } else {
            routes = routes.filter(r => r.id !== 'contacts');
        }
        return routes;
    }, [t, isBroker, user?.role]);

    // Backend Search
    const { data: backendResults = [], isLoading } = useQuery({
        queryKey: ['globalSearch', query],
        queryFn: async () => {
            if (!query || query.length < 2) return [];
            const response = await api.get<SearchResult[]>('/api/search', { params: { q: query } });
            return response.data;
        },
        enabled: isOpen && query.length >= 2,
        staleTime: 1000 * 60 * 1,
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
    }, [query, staticRoutes]);

    // Filter backend results: only brokers see USER results
    const filteredBackendResults = useMemo(() => {
        if (isBroker) return backendResults;
        // Remove USER results for non-brokers
        return backendResults.filter(r => r.type !== 'USER');
    }, [backendResults, isBroker]);

    const results = useMemo(() => {
        return [...staticResults, ...filteredBackendResults];
    }, [staticResults, filteredBackendResults]);

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
