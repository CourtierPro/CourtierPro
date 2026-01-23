import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Play } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { DocumentCard } from "./DocumentCard";
import { type DocumentRequest, DocumentStatusEnum } from "@/features/documents/types";

interface DocumentListProps {
    documents: DocumentRequest[];
    onUpload?: (document: DocumentRequest) => void;
    onReview?: (document: DocumentRequest) => void;
    onEdit?: (document: DocumentRequest) => void;
    focusDocumentId?: string | null;
    showBrokerNotes?: boolean;
}

export function DocumentList({
    documents,
    onUpload,
    onReview,
    onEdit,
    focusDocumentId,
    showBrokerNotes = true
}: DocumentListProps) {
    const { t } = useTranslation('transactions');

    // Initialize all rows as expanded (not collapsed)
    const [collapsedRows, setCollapsedRows] = useState<Record<string, boolean>>({});

    const toggleRow = (status: string) => {
        setCollapsedRows(prev => ({ ...prev, [status]: !prev[status] }));
    };

    const statusOrder = [
        DocumentStatusEnum.REQUESTED,
        DocumentStatusEnum.SUBMITTED,
        DocumentStatusEnum.APPROVED,
        DocumentStatusEnum.NEEDS_REVISION,
        DocumentStatusEnum.REJECTED
    ];

    const renderRow = (status: DocumentStatusEnum) => {
        const isCollapsed = collapsedRows[status];
        const statusDocs = documents.filter(d => d.status === status);
        const count = statusDocs.length;

        // Skip rendering row if no documents and we want to keep it clean? 
        // User said "one column for each of the 5 statuses", implying they should always be there even if empty?
        // Property columns persist even if empty (showing "No properties..."). I'll do the same.

        return (
            <motion.div
                layout
                key={status}
                initial={false}
                animate={{ backgroundColor: isCollapsed ? 'rgba(0,0,0,0)' : 'var(--muted)' }} // Optional highlight
                className={`flex flex-col gap-2 bg-muted/40 p-2 rounded-lg border border-border w-full transition-colors ${isCollapsed ? 'hover:bg-muted/60' : ''}`}
            >
                {/* Header Row */}
                <div
                    className="flex items-center gap-4 p-2 cursor-pointer select-none rounded-md hover:bg-background/50 transition-colors"
                    onClick={() => toggleRow(status)}
                >
                    <Button
                        variant="ghost"
                        size="sm"
                        className="p-1 h-6 w-6 shrink-0"
                    >
                        <Play className={`h-3 w-3 transition-transform duration-200 ${isCollapsed ? "" : "rotate-90"}`} />
                    </Button>

                    <div className="flex items-center justify-between flex-1">
                        <span className="font-semibold text-sm uppercase tracking-wide">
                            {t(`documentStatus.${status.toLowerCase()}`)}
                        </span>
                        <span className="text-muted-foreground text-xs font-medium bg-background px-2 py-0.5 rounded-full border">
                            {count}
                        </span>
                    </div>
                </div>

                {/* Content */}
                <AnimatePresence initial={false}>
                    {!isCollapsed && (
                        <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            transition={{ duration: 0.2 }}
                            className="overflow-hidden"
                        >
                            <div className="p-2 pt-0 grid gap-4">
                                {statusDocs.length > 0 ? (
                                    statusDocs.map((doc) => (
                                        <DocumentCard
                                            key={doc.requestId}
                                            document={doc}
                                            onUpload={onUpload}
                                            onReview={onReview}
                                            onEdit={onEdit}
                                            isFocused={focusDocumentId === doc.requestId}
                                            showBrokerNotes={showBrokerNotes}
                                        />
                                    ))
                                ) : (
                                    <div className="text-center py-6 border-2 border-dashed border-muted rounded-md text-muted-foreground text-sm">
                                        {t('noDocuments')}
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </motion.div>
        );
    };

    return (
        <div className="flex flex-col gap-4 w-full">
            {statusOrder.map(status => renderRow(status))}
        </div>
    );
}
