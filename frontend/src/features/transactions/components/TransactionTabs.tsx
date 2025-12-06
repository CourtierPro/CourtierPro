import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { AttributeRow } from "@/shared/components/branded/AttributeRow";
import { Button } from "@/shared/components/ui/button";
import { Textarea } from "@/shared/components/ui/textarea";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/components/ui/tabs";
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

    return (
        <Tabs defaultValue="details" className="w-full">
            {/* Tabs */}
            <TabsList className="border-b border-border w-full justify-start rounded-none bg-transparent h-auto p-0">
                <TabsTrigger
                    value="details"
                    className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
                >
                    {t('details')}
                </TabsTrigger>
                <TabsTrigger
                    value="documents"
                    className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
                >
                    {t('documents')}
                </TabsTrigger>
                <TabsTrigger
                    value="appointments"
                    className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
                >
                    {t('appointments')}
                </TabsTrigger>
            </TabsList>

            {/* Tab Content */}
            <TabsContent value="details" className="py-4">
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
                        <Textarea
                            value={notes}
                            onChange={(e) => onNotesChange(e.target.value)}
                            className="h-32 mb-4"
                            placeholder={t('addNotesPlaceholder')}
                        />
                        <Button
                            variant="secondary"
                            onClick={onSaveNotes}
                            disabled={isSavingNotes}
                            className="w-full"
                        >
                            {isSavingNotes ? t('saving') : t('saveNotes')}
                        </Button>
                    </Section>
                </div>
            </TabsContent>

            <TabsContent value="documents" className="py-4">
                <Section className="p-12 text-center">
                    <p className="text-muted-foreground">{t('documentsPlaceholder')}</p>
                </Section>
            </TabsContent>

            <TabsContent value="appointments" className="py-4">
                <Section className="p-12 text-center">
                    <p className="text-muted-foreground">{t('appointmentsPlaceholder')}</p>
                </Section>
            </TabsContent>
        </Tabs>
    );
}
