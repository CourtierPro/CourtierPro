import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";

interface TransactionPaginationProps {
    currentPage: number;
    totalPages: number;
    startIndex: number;
    endIndex: number;
    totalItems: number;
    onPageChange: (page: number) => void;
}

export function TransactionPagination({
    currentPage,
    totalPages,
    startIndex,
    endIndex,
    totalItems,
    onPageChange,
}: TransactionPaginationProps) {
    const { t } = useTranslation('transactions');

    if (totalPages <= 1) return null;

    return (
        <Section className="p-4 flex items-center justify-between flex-wrap gap-4">
            <p className="text-sm text-muted-foreground">
                {t('showing')} {startIndex + 1} {t('to')} {Math.min(endIndex, totalItems)} {t('of')}{' '}
                {totalItems} {t('transactions')}
            </p>

            <div className="flex items-center gap-2">
                <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                    disabled={currentPage === 1}
                    aria-label={t('previous')}
                >
                    <ChevronLeft className="w-5 h-5" />
                </Button>

                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <Button
                        key={page}
                        variant={currentPage === page ? "default" : "ghost"}
                        size="icon"
                        onClick={() => onPageChange(page)}
                        aria-label={`Page ${page}`}
                        aria-current={currentPage === page ? 'page' : undefined}
                    >
                        {page}
                    </Button>
                ))}

                <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                    disabled={currentPage === totalPages}
                    aria-label={t('next')}
                >
                    <ChevronRight className="w-5 h-5" />
                </Button>
            </div>
        </Section>
    );
}

