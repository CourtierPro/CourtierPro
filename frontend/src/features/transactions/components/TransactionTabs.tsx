import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { useState } from 'react';
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { Button } from "@/shared/components/ui/button";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { Textarea } from "@/shared/components/ui/textarea";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/components/ui/tabs";
import { type Transaction, useTransactionConditions, useTransactionOffers } from '@/features/transactions/api/queries';
import { DocumentsPage } from '@/pages/documents/DocumentsPage';
import { Calendar } from 'lucide-react';
import { TransactionTimeline } from './TransactionTimeline';
import { PropertyList } from './PropertyList';
import { OfferList } from './OfferList';
import { ParticipantsList } from './ParticipantsList';
import { ConditionList } from './ConditionList';
import { AddConditionModal } from './AddConditionModal';
import { ConditionDetailModal } from './ConditionDetailModal';
import type { Condition, PropertyOffer } from '@/shared/api/types';

interface TransactionTabsProps {
  transaction: Transaction;
  notes: string;
  onNotesChange: (notes: string) => void;
  onSaveNotes: () => void;
  isSavingNotes: boolean;
  isReadOnly?: boolean;
  TimelineComponent?: React.ComponentType<{ transactionId: string }>;
  onTransactionUpdate?: () => void;
}

export function TransactionTabs({
  transaction,
  notes,
  onNotesChange,
  onSaveNotes,
  isSavingNotes,
  isReadOnly = false,
  TimelineComponent = TransactionTimeline,
  onTransactionUpdate,
}: TransactionTabsProps) {
  const { t } = useTranslation('transactions');
  const [searchParams, setSearchParams] = useSearchParams();

  // Properties tab only for buyer-side transactions
  const isBuyerTransaction = transaction.side === 'BUY_SIDE';
  // Offers tab only for seller-side transactions
  const isSellerTransaction = transaction.side === 'SELL_SIDE';

  // Get tab from URL or default to 'details' (or 'timeline' if read-only)
  const defaultTab = isReadOnly ? 'timeline' : 'details';
  const currentTab = searchParams.get('tab') || defaultTab;
  const focusDocumentId = searchParams.get('focus');

  const handleTabChange = (value: string) => {
    setSearchParams(prev => {
      const newParams = new URLSearchParams(prev);
      newParams.set('tab', value);
      if (value !== 'documents') {
        newParams.delete('focus');
      }
      return newParams;
    });
  };

  // Conditions state
  const { data: conditions = [], isLoading: isLoadingConditions } = useTransactionConditions(transaction.transactionId);
  const [isConditionAddModalOpen, setIsConditionAddModalOpen] = useState(false);
  const [isConditionDetailModalOpen, setIsConditionDetailModalOpen] = useState(false);
  const [selectedCondition, setSelectedCondition] = useState<Condition | undefined>(undefined);

  // Fetch offers for linked conditions display (sell-side transactions)
  const { data: offers = [] } = useTransactionOffers(
    isSellerTransaction ? transaction.transactionId : '',
  );

  const handleAddCondition = () => {
    setSelectedCondition(undefined);
    setIsConditionAddModalOpen(true);
  };

  const handleConditionClick = (condition: Condition) => {
    setSelectedCondition(condition);
    setIsConditionDetailModalOpen(true);
  };

  const handleEditCondition = () => {
    // Close detail modal and open edit modal
    setIsConditionDetailModalOpen(false);
    setIsConditionAddModalOpen(true);
  };

  // Navigation from linked items - navigate to offers tab
  const handleLinkedOfferClick = () => {
    setIsConditionDetailModalOpen(false);
    handleTabChange('offers');
  };

  // Navigation from linked items - navigate to properties tab
  const handleLinkedPropertyOfferClick = () => {
    setIsConditionDetailModalOpen(false);
    handleTabChange('properties');
  };

  // Get linked items for the selected condition
  const getLinkedItemsForCondition = (conditionId: string) => {
    const linkedOffers = offers.filter(
      offer => offer.conditions?.some(c => c.conditionId === conditionId)
    );
    // TODO: Add propertyOffers when available for buy-side
    return { linkedOffers, linkedPropertyOffers: [] as PropertyOffer[] };
  };


  return (
    <Tabs value={currentTab} onValueChange={handleTabChange} className="w-full">
      <TabsList className="border-b border-border w-full justify-start rounded-none bg-transparent h-auto p-0 overflow-x-auto">
        {!isReadOnly && (
          <TabsTrigger
            value="details"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
          >
            {t('details')}
          </TabsTrigger>
        )}
        <TabsTrigger
          value="timeline"
          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
        >
          {t('timeline')}
        </TabsTrigger>
        {/* Properties tab - only for buyer-side transactions */}
        {isBuyerTransaction && (
          <TabsTrigger
            value="properties"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
          >
            {t('properties')}
          </TabsTrigger>
        )}
        {/* Offers tab - only for seller-side transactions */}
        {isSellerTransaction && (
          <TabsTrigger
            value="offers"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
          >
            {t('offers')}
          </TabsTrigger>
        )}
        <TabsTrigger
          value="documents"
          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
        >
          {t('documents')}
        </TabsTrigger>
        {!isReadOnly && (
          <TabsTrigger
            value="participants"
            className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
          >
            {t('participants')}
          </TabsTrigger>
        )}
        <TabsTrigger
          value="appointments"
          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
        >
          {t('appointments')}
        </TabsTrigger>
        <TabsTrigger
          value="conditions"
          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:shadow-none"
        >
          {t('conditions.tab')}
        </TabsTrigger>
      </TabsList>


      <TabsContent value="details" className="py-4">
        <div className="grid grid-cols-1 gap-6">
          {!isReadOnly && (
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
          )}
        </div>
      </TabsContent>

      <TabsContent value="timeline" className="py-4">
        <TimelineComponent transactionId={transaction.transactionId} />
      </TabsContent>

      {/* Properties Tab Content - only for buyer-side transactions */}
      {isBuyerTransaction && (
        <TabsContent value="properties" className="py-4">
          <PropertyList
            transactionId={transaction.transactionId}
            isReadOnly={isReadOnly}
            onTransactionUpdate={onTransactionUpdate}
            currentTransactionAddress={transaction.propertyAddress}
          />
        </TabsContent>
      )}

      {/* Offers Tab Content - only for seller-side transactions */}
      {isSellerTransaction && (
        <TabsContent value="offers" className="py-4">
          <OfferList
            transactionId={transaction.transactionId}
            isReadOnly={isReadOnly}
            clientId={isReadOnly ? transaction.clientId : undefined}
          />
        </TabsContent>
      )}

      <TabsContent value="documents" className="py-4">
        <DocumentsPage
          transactionId={transaction.transactionId}
          focusDocumentId={focusDocumentId}
          isReadOnly={isReadOnly}
          transactionSide={transaction.side}
        />
      </TabsContent>

      {/* Participants Tab Content - only for brokers */}
      {!isReadOnly && (
        <TabsContent value="participants" className="py-4">
          <ParticipantsList
            transactionId={transaction.transactionId}
            isReadOnly={isReadOnly}
          />
        </TabsContent>
      )}

      <TabsContent value="appointments" className="py-4">
        <Section>
          <EmptyState
            icon={<Calendar />}
            title={t('noAppointments')}
            description={t('appointmentsPlaceholder')}
            action={<Button variant="outline">{t('scheduleAppointment')}</Button>}
          />
        </Section>
      </TabsContent>

      <TabsContent value="conditions" className="py-4">
        <ConditionList
          conditions={conditions}
          onAddClick={handleAddCondition}
          onConditionClick={handleConditionClick}
          isLoading={isLoadingConditions}
          isReadOnly={isReadOnly}
          offers={offers}
        />

        {/* Condition Detail Modal */}
        <ConditionDetailModal
          isOpen={isConditionDetailModalOpen}
          onClose={() => setIsConditionDetailModalOpen(false)}
          condition={selectedCondition ?? null}
          transactionId={transaction.transactionId}
          linkedOffers={selectedCondition ? getLinkedItemsForCondition(selectedCondition.conditionId).linkedOffers : []}
          linkedPropertyOffers={selectedCondition ? getLinkedItemsForCondition(selectedCondition.conditionId).linkedPropertyOffers : []}
          onEdit={handleEditCondition}
          onOfferClick={handleLinkedOfferClick}
          onPropertyOfferClick={handleLinkedPropertyOfferClick}
          isReadOnly={isReadOnly}
        />

        {/* Add/Edit Condition Modal */}
        <AddConditionModal
          open={isConditionAddModalOpen}
          onOpenChange={setIsConditionAddModalOpen}
          transactionId={transaction.transactionId}
          existingCondition={selectedCondition}
        />
      </TabsContent>
    </Tabs>
  );
}
