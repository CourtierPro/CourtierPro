import { createContext } from 'react';

export interface SearchContextType {
    query: string;
    setQuery: (query: string) => void;
    isOpen: boolean;
    setIsOpen: (isOpen: boolean | ((prev: boolean) => boolean)) => void;
}

export const SearchContext = createContext<SearchContextType | undefined>(undefined);
