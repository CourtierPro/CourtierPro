import { useState } from "react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { FileText, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { useDocuments } from "@/features/documents/api/queries";
import { useRequestDocument, type RequestDocumentDTO } from "@/features/documents/api/mutations";
import { RequestDocumentModal } from "@/features/documents/components/RequestDocumentModal";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { useTranslation } from "react-i18next";

export function DocumentsPage() {
  const { t } = useTranslation('documents');
  const { data: documents = [], isLoading, error, refetch } = useDocuments();
  const requestDocument = useRequestDocument();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleRequestDocument = async (documentTitle: string, instructions: string) => {
    try {
      await requestDocument.mutateAsync({
        title: documentTitle,
        message: instructions,
        recipientEmail: 'client@example.com', // Hardcoded for now
        transactionId: 'tx-1', // Hardcoded for now
      } as RequestDocumentDTO);
      setIsModalOpen(false);
    } catch (err) {
      console.error("Failed to request document", err);
    }
  };

  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title', 'Documents')}
        subtitle={t('subtitle', 'Manage all your transaction documents in one place.')}
        actions={
          documents.length > 0 && (
            <Button onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {t('requestDocument', 'Request Document')}
            </Button>
          )
        }
      />

      {documents.length === 0 ? (
        <Section>
          <EmptyState
            icon={<FileText />}
            title={t('noDocumentsTitle', 'No documents found')}
            description={t('noDocumentsDesc', "You haven't uploaded any documents yet. Start by creating a transaction.")}
            action={
              <Button onClick={() => setIsModalOpen(true)}>{t('requestDocument', 'Request Document')}</Button>
            }
          />
        </Section>
      ) : (
        <DocumentList documents={documents} />
      )}

      <RequestDocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleRequestDocument}
        transactionType="buy"
        currentStage="offer"
      />
    </div>
  );
}
