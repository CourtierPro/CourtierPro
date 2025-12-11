import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { Button } from "@/shared/components/ui/button";
import { Textarea } from "@/shared/components/ui/textarea";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/components/ui/tabs";
import { type Transaction } from '@/features/transactions/api/queries';
import { DocumentsPage } from '@/pages/documents/DocumentsPage';
import { Calendar } from 'lucide-react';

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
            <TabsList className="border-b border-border w-full justify-start rounded-none bg-transparent h-auto p-0 overflow-x-auto">
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

            <TabsContent value="details" className="py-4">
                <div className="grid grid-cols-1 gap-6">

                    <Section className="p-4 md:p-6">
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
                <DocumentsPage transactionId={transaction.transactionId} />
            </TabsContent>



            <TabsContent value="appointments" className="py-4">
                <Section className="p-12 text-center flex flex-col items-center justify-center gap-4">
                    <div className="h-12 w-12 rounded-full bg-muted flex items-center justify-center">
                        <Calendar className="h-6 w-6 text-muted-foreground" />
                    </div>
                    <div>
                        <h3 className="text-lg font-medium mb-1">{t('noAppointments')}</h3>
                        <p className="text-muted-foreground max-w-sm mx-auto">{t('appointmentsPlaceholder')}</p>
                    </div>
                    <Button variant="outline">{t('scheduleAppointment')}</Button>
                </Section>
            </TabsContent>
        </Tabs>
    );
}
