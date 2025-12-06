import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { AttributeRow } from "@/shared/components/branded/AttributeRow";
import { type Transaction } from '@/features/transactions/api/queries';

interface TransactionTabsProps {
    transaction: Transaction;
    notes: string;
    onNotesChange: (notes: string) => void;
    onSaveNotes: () => void;
    isSavingNotes: boolean;
}

export function TransactionTabs({
    transaction,
    notes,
    onNotesChange,
    onSaveNotes,
    isSavingNotes,
}: TransactionTabsProps) {
    const { t } = useTranslation('transactions');
    const [activeTab, setActiveTab] = useState<'details' | 'documents' | 'appointments'>('details');

    return (
        <>
            {/* Tabs */}
            <div className="flex gap-4 border-b border-border">
                <button
                    onClick={() => setActiveTab('details')}
                    className={`px-4 py-2 font-medium transition-colors border-b-2 ${activeTab === 'details'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-muted-foreground hover:text-foreground'
                        }`}
                >
                    {t('details')}
                </button>
                <button
                    onClick={() => setActiveTab('documents')}
                    className={`px-4 py-2 font-medium transition-colors border-b-2 ${activeTab === 'documents'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-muted-foreground hover:text-foreground'
                        }`}
                >
                    {t('documents')}
                </button>
                <button
                    onClick={() => setActiveTab('appointments')}
                    className={`px-4 py-2 font-medium transition-colors border-b-2 ${activeTab === 'appointments'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-muted-foreground hover:text-foreground'
                        }`}
                >
                    {t('appointments')}
                </button>
            </div>

            {/* Tab Content */}
            <div className="py-4">
                {activeTab === 'details' && (
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        <Section className="lg:col-span-2 p-6">
                            <SectionHeader title={t('propertyDetails')} />
                            <div className="space-y-0">
                                <AttributeRow label={t('street')} value={transaction.propertyAddress.street} />
                                <AttributeRow label={t('city')} value={transaction.propertyAddress.city} />
                                <AttributeRow label={t('province')} value={transaction.propertyAddress.province} />
                                <AttributeRow label={t('postalCode')} value={transaction.propertyAddress.postalCode} />
                            </div>
                        </Section>

                        <Section className="p-6">
                            <SectionHeader title={t('notes')} />
                            <textarea
                                value={notes}
                                onChange={(e) => onNotesChange(e.target.value)}
                                className="w-full h-32 p-3 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none mb-4"
                                placeholder={t('addNotesPlaceholder')}
                            />
                            <button
                                onClick={onSaveNotes}
                                disabled={isSavingNotes}
                                className="w-full px-4 py-2 bg-secondary text-secondary-foreground rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
                            >
                                {isSavingNotes ? t('saving') : t('saveNotes')}
                            </button>
                        </Section>
                    </div>
                )}

                {activeTab === 'documents' && (
                    <Section className="p-12 text-center">
                        <p className="text-muted-foreground">{t('documentsPlaceholder')}</p>
                    </Section>
                )}

                {activeTab === 'appointments' && (
                    <Section className="p-12 text-center">
                        <p className="text-muted-foreground">{t('appointmentsPlaceholder')}</p>
                    </Section>
                )}
            </div>
        </>
    );
}
