import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";

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
                <button
                    onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                    disabled={currentPage === 1}
                    className="p-2 rounded-lg hover:bg-muted focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-foreground"
                    aria-label={t('previous')}
                >
                    <ChevronLeft className="w-5 h-5" />
                </button>

                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <button
                        key={page}
                        onClick={() => onPageChange(page)}
                        className={`w-10 h-10 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary transition-colors ${currentPage === page
                            ? 'bg-primary text-primary-foreground'
                            : 'hover:bg-muted text-foreground'
                            }`}
                        aria-label={`Page ${page}`}
                        aria-current={currentPage === page ? 'page' : undefined}
                    >
                        {page}
                    </button>
                ))}

                <button
                    onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                    disabled={currentPage === totalPages}
                    className="p-2 rounded-lg hover:bg-muted focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-foreground"
                    aria-label={t('next')}
                >
                    <ChevronRight className="w-5 h-5" />
                </button>
            </div>
        </Section>
    );
}
