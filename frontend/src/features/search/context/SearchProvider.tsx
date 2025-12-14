import { useState, type ReactNode } from 'react';
import { SearchContext } from './SearchContext';

export function SearchProvider({ children }: { children: ReactNode }) {
    const [query, setQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);

    return (
        <SearchContext.Provider value={{ query, setQuery, isOpen, setIsOpen }}>
            {children}
        </SearchContext.Provider>
    );
}
