import { useContext } from 'react';
import { SearchContext, type SearchContextType } from './SearchContext';

export function useSearchContext(): SearchContextType {
    const context = useContext(SearchContext);
    if (context === undefined) {
        throw new Error('useSearchContext must be used within a SearchProvider');
    }
    return context;
}
