import { useState, useCallback, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { TransactionOverviewCard } from "./TransactionOverviewCard";
import type { Transaction } from "@/features/transactions/api/queries";
import { cn } from "@/shared/utils/utils";

interface DocumentCountData {
    documentCount: number;
    approvedDocumentCount: number;
    needsRevisionCount: number;
    submittedDocumentCount: number;
    requestedDocumentCount: number;
}

interface TransactionCarouselProps {
    transactions: Transaction[];
    getDocumentCounts: (transactionId: string) => DocumentCountData;
    onViewDetails?: (transactionId: string) => void;
}

type PanelType = "documents" | "transaction" | "offers" | "properties";

export function TransactionCarousel({
    transactions,
    getDocumentCounts,
    onViewDetails,
}: TransactionCarouselProps) {
    const { t } = useTranslation("dashboard");
    const [currentIndex, setCurrentIndex] = useState(0);
    const [expandedPanel, setExpandedPanel] = useState<PanelType | null>(null);
    const [isAnimating, setIsAnimating] = useState(false);
    const [slideDirection, setSlideDirection] = useState<'left' | 'right' | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    // Touch/swipe state
    const touchStartX = useRef<number | null>(null);
    const touchEndX = useRef<number | null>(null);
    const minSwipeDistance = 50;

    const totalTransactions = transactions.length;
    const hasPrevious = currentIndex > 0;
    const hasNext = currentIndex < totalTransactions - 1;
    const leftCount = currentIndex;
    const rightCount = totalTransactions - currentIndex - 1;

    const goToIndex = useCallback((index: number) => {
        if (index < 0 || index >= totalTransactions || isAnimating) return;

        // Collapse panel when navigating
        if (expandedPanel) {
            setExpandedPanel(null);
        }

        // Determine slide direction
        setSlideDirection(index > currentIndex ? 'left' : 'right');
        setIsAnimating(true);
        setCurrentIndex(index);

        // Reset animation state after transition completes
        setTimeout(() => {
            setIsAnimating(false);
            setSlideDirection(null);
        }, 500);
    }, [totalTransactions, isAnimating, expandedPanel, currentIndex]);

    const goPrevious = useCallback(() => {
        if (hasPrevious) goToIndex(currentIndex - 1);
    }, [hasPrevious, currentIndex, goToIndex]);

    const goNext = useCallback(() => {
        if (hasNext) goToIndex(currentIndex + 1);
    }, [hasNext, currentIndex, goToIndex]);

    // Keyboard navigation
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            // Only handle if carousel is focused or contains focus
            if (!containerRef.current?.contains(document.activeElement)) return;

            if (e.key === "ArrowLeft") {
                e.preventDefault();
                goPrevious();
            } else if (e.key === "ArrowRight") {
                e.preventDefault();
                goNext();
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [goPrevious, goNext]);

    // Touch handlers for swipe
    const onTouchStart = (e: React.TouchEvent) => {
        touchEndX.current = null;
        touchStartX.current = e.targetTouches[0].clientX;
    };

    const onTouchMove = (e: React.TouchEvent) => {
        touchEndX.current = e.targetTouches[0].clientX;
    };

    const onTouchEnd = () => {
        if (!touchStartX.current || !touchEndX.current) return;

        const distance = touchStartX.current - touchEndX.current;
        const isLeftSwipe = distance > minSwipeDistance;
        const isRightSwipe = distance < -minSwipeDistance;

        if (isLeftSwipe && hasNext) {
            goNext();
        } else if (isRightSwipe && hasPrevious) {
            goPrevious();
        }

        touchStartX.current = null;
        touchEndX.current = null;
    };

    const handlePanelChange = useCallback((panel: PanelType | null) => {
        setExpandedPanel(panel);
    }, []);

    if (totalTransactions === 0) {
        return null; // Parent handles empty state
    }

    const currentTransaction = transactions[currentIndex];
    const counts = getDocumentCounts(currentTransaction.transactionId);

    return (
        <div
            ref={containerRef}
            className="relative"
            role="region"
            aria-label={t("carousel.position", { current: currentIndex + 1, total: totalTransactions })}
            tabIndex={0}
            onTouchStart={onTouchStart}
            onTouchMove={onTouchMove}
            onTouchEnd={onTouchEnd}
        >
            {/* Main carousel container */}
            <div className="flex items-stretch gap-2">
                {/* Left navigation */}
                <div className="relative flex items-center justify-center w-12 md:w-16 flex-shrink-0">
                    <Button
                        variant="outline"
                        size="icon"
                        className={cn(
                            "h-12 w-12 md:h-14 md:w-14 rounded-full border-2 transition-all",
                            hasPrevious
                                ? "border-orange-300 hover:border-orange-500 hover:bg-orange-50 dark:hover:bg-orange-950/30"
                                : "border-slate-200 dark:border-slate-700 opacity-40 cursor-not-allowed"
                        )}
                        onClick={goPrevious}
                        disabled={!hasPrevious}
                        aria-label={t("carousel.previous", "Previous transaction")}
                    >
                        <ChevronLeft className="h-6 w-6" />
                    </Button>
                    {leftCount > 0 && (
                        <span className="absolute -bottom-6 left-1/2 -translate-x-1/2 text-sm font-medium text-muted-foreground">
                            {leftCount}
                        </span>
                    )}
                </div>

                {/* Card container with slide animation */}
                <div className="flex-1 min-w-0 overflow-hidden">
                    <div
                        key={`tx-${currentIndex}`}
                        className={cn(
                            slideDirection === 'left' && "animate-slide-in-right",
                            slideDirection === 'right' && "animate-slide-in-left"
                        )}
                    >
                        <TransactionOverviewCard
                            transaction={currentTransaction}
                            documentCount={counts.documentCount}
                            approvedDocumentCount={counts.approvedDocumentCount}
                            needsRevisionCount={counts.needsRevisionCount}
                            submittedDocumentCount={counts.submittedDocumentCount}
                            requestedDocumentCount={counts.requestedDocumentCount}
                            onViewDetails={onViewDetails}
                            expandedPanel={expandedPanel}
                            onPanelChange={handlePanelChange}
                        />
                    </div>
                </div>

                {/* Right navigation */}
                <div className="relative flex items-center justify-center w-12 md:w-16 flex-shrink-0">
                    <Button
                        variant="outline"
                        size="icon"
                        className={cn(
                            "h-12 w-12 md:h-14 md:w-14 rounded-full border-2 transition-all",
                            hasNext
                                ? "border-orange-300 hover:border-orange-500 hover:bg-orange-50 dark:hover:bg-orange-950/30"
                                : "border-slate-200 dark:border-slate-700 opacity-40 cursor-not-allowed"
                        )}
                        onClick={goNext}
                        disabled={!hasNext}
                        aria-label={t("carousel.next", "Next transaction")}
                    >
                        <ChevronRight className="h-6 w-6" />
                    </Button>
                    {rightCount > 0 && (
                        <span className="absolute -bottom-6 left-1/2 -translate-x-1/2 text-sm font-medium text-muted-foreground">
                            {rightCount}
                        </span>
                    )}
                </div>
            </div>

            {/* Dot indicators */}
            {totalTransactions > 1 && (
                <div className="flex justify-center items-center gap-2 mt-4" role="tablist">
                    {transactions.map((_, index) => (
                        <button
                            key={index}
                            role="tab"
                            aria-selected={index === currentIndex}
                            aria-label={t("carousel.position", { current: index + 1, total: totalTransactions })}
                            className={cn(
                                "w-2.5 h-2.5 rounded-full transition-all duration-200",
                                index === currentIndex
                                    ? "bg-orange-500 w-4"
                                    : "bg-slate-300 dark:bg-slate-600 hover:bg-slate-400 dark:hover:bg-slate-500"
                            )}
                            onClick={() => goToIndex(index)}
                        />
                    ))}
                </div>
            )}

            {/* Screen reader position announcement */}
            <div className="sr-only" aria-live="polite">
                {t("carousel.position", { current: currentIndex + 1, total: totalTransactions })}
            </div>
        </div>
    );
}
