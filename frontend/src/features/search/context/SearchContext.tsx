import { createContext, useContext, useState, type ReactNode } from 'react';

interface SearchContextType {
    query: string;
    setQuery: (query: string) => void;
    isOpen: boolean;
    setIsOpen: (isOpen: boolean | ((prev: boolean) => boolean)) => void;
}

const SearchContext = createContext<SearchContextType | undefined>(undefined);

export function SearchProvider({ children }: { children: ReactNode }) {
    const [query, setQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);

    return (
        <SearchContext.Provider value={{ query, setQuery, isOpen, setIsOpen }}>
            {children}
        </SearchContext.Provider>
    );
}

export function useSearchContext() {
    const context = useContext(SearchContext);
    if (context === undefined) {
        throw new Error('useSearchContext must be used within a SearchProvider');
    }
    return context;
}
