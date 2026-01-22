import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { motion, AnimatePresence, useMotionValue, useTransform, type PanInfo } from 'framer-motion';
import { Check, X, HelpCircle, MapPin } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Textarea } from '@/shared/components/ui/textarea';
import type { Property } from '@/shared/api/types';
import { toast } from 'sonner';

interface PropertyReviewModalProps {
    isOpen: boolean;
    onClose: () => void;
    properties: Property[];
    onAccept: (propertyId: string) => Promise<void>;
    onReject: (propertyId: string) => Promise<void>;
    onRequestInfo: (propertyId: string, notes: string) => Promise<void>;
}

export function PropertyReviewModal({ isOpen, onClose, properties, onAccept, onReject, onRequestInfo }: PropertyReviewModalProps) {
    const { t } = useTranslation('transactions');
    const [currentIndex, setCurrentIndex] = useState(0);
    const [infoNote, setInfoNote] = useState('');
    const [showInfoInput, setShowInfoInput] = useState(false);

    // Sort so first card is at the end (top) of visual stack
    const activeProperties = properties.slice(currentIndex);
    const currentProperty = activeProperties[0];

    const x = useMotionValue(0);
    const rotate = useTransform(x, [-200, 200], [-10, 10]);
    const opacityAccept = useTransform(x, [50, 150], [0, 1]);
    const opacityReject = useTransform(x, [-150, -50], [1, 0]);

    if (!isOpen || !currentProperty) return null;

    const handleSwipe = async (direction: 'left' | 'right') => {
        if (direction === 'right') {
            await onAccept(currentProperty.propertyId);
            toast.success(t('propertyAccepted'));
        } else {
            await onReject(currentProperty.propertyId);
            toast.success(t('propertyRejected'));
        }
        advanceCard();
    };

    const handleRequestInfo = async () => {
        if (!infoNote.trim()) {
            toast.error(t('noteRequired'));
            return;
        }
        await onRequestInfo(currentProperty.propertyId, infoNote);
        toast.success(t('infoRequested'));
        setInfoNote('');
        setShowInfoInput(false);
        advanceCard();
    };

    const advanceCard = () => {
        if (currentIndex < properties.length - 1) {
            setCurrentIndex(prev => prev + 1);
            x.set(0);
        } else {
            onClose();
        }
    };

    const handleDragEnd = (_: unknown, info: PanInfo) => {
        if (info.offset.x > 100) {
            handleSwipe('right');
        } else if (info.offset.x < -100) {
            handleSwipe('left');
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="sm:max-w-md h-[600px] flex flex-col p-0 overflow-hidden bg-background/95 backdrop-blur">
                <DialogHeader className="p-6 pb-2">
                    <DialogTitle>{t('reviewProperties')}</DialogTitle>
                    <DialogDescription>
                        {t('reviewPropertiesDescription', { count: activeProperties.length })}
                    </DialogDescription>
                </DialogHeader>

                <div className="flex-1 relative flex items-center justify-center p-4 py-8 overflow-hidden">
                    <AnimatePresence>
                        {activeProperties.slice(0, 2).reverse().map((property, index) => {
                            const isTop = index === activeProperties.slice(0, 2).length - 1;
                            return (
                                <motion.div
                                    key={property.propertyId}
                                    style={{
                                        x: isTop ? x : 0,
                                        rotate: isTop ? rotate : 0,
                                        zIndex: isTop ? 10 : 0,
                                        scale: isTop ? 1 : 0.95,
                                        y: isTop ? 0 : 20
                                    }}
                                    drag={isTop && !showInfoInput ? "x" : false}
                                    dragConstraints={{ left: 0, right: 0 }}
                                    onDragEnd={handleDragEnd}
                                    initial={false}
                                    animate={{ scale: isTop ? 1 : 0.95, y: isTop ? 0 : 10, opacity: 1 }}
                                    exit={{ x: x.get() < 0 ? -500 : 500, opacity: 0 }}
                                    transition={{ type: 'spring', stiffness: 300, damping: 20 }}
                                    className="absolute w-full max-w-sm bg-card border rounded-xl shadow-xl overflow-hidden flex flex-col h-[400px] select-none"
                                >
                                    {/* Overlay Status Indicators for Swipe */}
                                    {isTop && (
                                        <>
                                            <motion.div style={{ opacity: opacityAccept }} className="absolute top-8 left-8 z-20 pointer-events-none">
                                                <div className="border-4 border-emerald-500 text-emerald-500 font-bold text-2xl px-4 py-2 rounded -rotate-12 uppercase tracking-widest bg-emerald-500/10 backdrop-blur-sm">
                                                    {t('clientOfferDecisions.ACCEPT')}
                                                </div>
                                            </motion.div>
                                            <motion.div style={{ opacity: opacityReject }} className="absolute top-8 right-8 z-20 pointer-events-none">
                                                <div className="border-4 border-red-500 text-red-500 font-bold text-2xl px-4 py-2 rounded rotate-12 uppercase tracking-widest bg-red-500/10 backdrop-blur-sm">
                                                    {t('clientOfferDecisions.DECLINE')}
                                                </div>
                                            </motion.div>
                                        </>
                                    )}

                                    {/* Card Content */}
                                    <div className="bg-muted/30 p-6 flex-1 flex flex-col gap-4">
                                        <div className="flex items-start gap-3">
                                            <div className="mt-1 bg-primary/10 p-2 rounded-full">
                                                <MapPin className="w-5 h-5 text-primary" />
                                            </div>
                                            <div>
                                                <h3 className="text-lg font-semibold leading-tight line-clamp-2">
                                                    {property.address?.street}
                                                </h3>
                                                <div className="text-muted-foreground flex items-center gap-1 mt-1">
                                                    {property.address?.city}, {property.address?.province} {property.address?.postalCode}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-2 gap-3 mt-2">
                                            <div className="bg-background rounded-lg p-3 border">
                                                <span className="text-xs text-muted-foreground uppercase">{t('askingPrice')}</span>
                                                <div className="text-lg font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                                                    {new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD', maximumFractionDigits: 0 }).format(property.askingPrice || 0)}
                                                </div>
                                            </div>
                                            <div className="bg-background rounded-lg p-3 border">
                                                <span className="text-xs text-muted-foreground uppercase">{t('centrisNumber')}</span>
                                                <div className="text-lg font-medium mt-0.5">
                                                    {property.centrisNumber || '—'}
                                                </div>
                                            </div>
                                        </div>

                                        {property.notes && (
                                            <div className="bg-amber-50 dark:bg-amber-950/30 border border-amber-100 dark:border-amber-900/50 p-3 rounded-lg text-sm text-amber-900 dark:text-amber-200 italic line-clamp-4">
                                                “{property.notes}”
                                            </div>
                                        )}
                                    </div>

                                    {/* Action Hint */}
                                    {!showInfoInput && isTop && (
                                        <div className="p-3 text-center text-xs text-muted-foreground border-t bg-muted/10">
                                            {t('swipeInstruction')}
                                        </div>
                                    )}
                                </motion.div>
                            );
                        })}
                    </AnimatePresence>
                </div>

                <DialogFooter className="p-4 bg-muted/10 border-t gap-2 sm:gap-0 flex-col sm:flex-row">
                    {showInfoInput ? (
                        <div className="w-full flex flex-col gap-2">
                            <Textarea
                                placeholder={t('brokerNotesPlaceholder')}
                                value={infoNote}
                                onChange={(e) => setInfoNote(e.target.value)}
                                className="h-20"
                                autoFocus
                            />
                            <div className="flex gap-2 justify-end">
                                <Button variant="ghost" size="sm" onClick={() => setShowInfoInput(false)}>
                                    {t('cancel')}
                                </Button>
                                <Button size="sm" onClick={handleRequestInfo}>
                                    {t('common.save')}
                                </Button>
                            </div>
                        </div>
                    ) : (
                        <div className="flex justify-center w-full gap-4">
                            <Button
                                variant="outline"
                                size="icon"
                                className="h-12 w-12 rounded-full border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 dark:border-red-900/50 dark:hover:bg-red-950/50"
                                onClick={() => handleSwipe('left')}
                            >
                                <X className="w-6 h-6" />
                            </Button>

                            <Button
                                variant="outline"
                                size="icon"
                                className="h-12 w-12 rounded-full border-blue-200 text-blue-600 hover:bg-blue-50 hover:border-blue-300 dark:border-blue-900/50 dark:hover:bg-blue-950/50"
                                onClick={() => setShowInfoInput(true)}
                            >
                                <HelpCircle className="w-6 h-6" />
                            </Button>

                            <Button
                                variant="outline"
                                size="icon"
                                className="h-12 w-12 rounded-full border-emerald-200 text-emerald-600 hover:bg-emerald-50 hover:border-emerald-300 dark:border-emerald-900/50 dark:hover:bg-emerald-950/50"
                                onClick={() => handleSwipe('right')}
                            >
                                <Check className="w-6 h-6" />
                            </Button>
                        </div>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
