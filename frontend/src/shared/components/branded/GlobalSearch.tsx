import * as React from "react"
import { Search, Loader2, File, User, Building, LayoutDashboard } from "lucide-react"
import { useTranslation } from "react-i18next"

import {
    CommandDialog,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/shared/components/ui/command"
import { useGlobalSearch, type SearchResultType } from "@/features/search/hooks/useGlobalSearch"

export function GlobalSearch() {
    const { t } = useTranslation("common")
    const {
        query,
        setQuery,
        isOpen,
        setIsOpen,
        results,
        isLoading,
        handleSelect,
    } = useGlobalSearch()

    // Keyboard shortcut listener
    React.useEffect(() => {
        const down = (e: KeyboardEvent) => {
            if ((e.key === "k" && (e.metaKey || e.ctrlKey)) || e.key === "/") {
                if (
                    (e.target instanceof HTMLElement && e.target.isContentEditable) ||
                    e.target instanceof HTMLInputElement ||
                    e.target instanceof HTMLTextAreaElement ||
                    e.target instanceof HTMLSelectElement
                ) {
                    return
                }

                e.preventDefault()
                setIsOpen((open) => !open)
            }
        }

        document.addEventListener("keydown", down)
        return () => document.removeEventListener("keydown", down)
    }, [setIsOpen])

    const getIcon = (type: SearchResultType) => {
        switch (type) {
            case 'TRANSACTION': return <Building className="mr-2 h-4 w-4" />;
            case 'DOCUMENT': return <File className="mr-2 h-4 w-4" />;
            case 'USER': return <User className="mr-2 h-4 w-4" />;
            case 'PAGE': return <LayoutDashboard className="mr-2 h-4 w-4" />;
            default: return <Search className="mr-2 h-4 w-4" />;
        }
    }

    return (
        <CommandDialog open={isOpen} onOpenChange={setIsOpen}>
            <CommandInput
                placeholder={t("searchPlaceholder", "Type to search...")}
                value={query}
                onValueChange={setQuery}
            />
            <CommandList>
                <CommandEmpty>
                    {isLoading ? (
                        <div className="flex items-center justify-center p-4">
                            <Loader2 className="h-4 w-4 animate-spin" />
                            <span className="ml-2">{t("searching", "Searching...")}</span>
                        </div>
                    ) : (
                        t("noResults", "No results found.")
                    )}
                </CommandEmpty>

                {results.length > 0 && (
                    <CommandGroup heading={t("suggestions", "Suggestions")}>
                        {results.map((result) => (
                            <CommandItem
                                key={`${result.type}-${result.id}`}
                                value={result.title + " " + result.subtitle} // Helps with local filtering if command does it, but we handle it manually
                                onSelect={() => handleSelect(result)}
                            >
                                {getIcon(result.type)}
                                <div className="flex flex-col">
                                    <span>{result.title}</span>
                                    <span className="text-xs text-muted-foreground">{result.subtitle}</span>
                                </div>
                            </CommandItem>
                        ))}
                    </CommandGroup>
                )}
            </CommandList>
        </CommandDialog>
    )
}
