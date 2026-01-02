import { useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';
import { useDocuments } from '../api/queries';
import { useRequestDocument } from '../api/mutations';
import { DocumentPartyEnum, DocumentTypeEnum } from '../types';
import { useErrorHandler } from '@/shared/hooks/useErrorHandler';

export function useDocumentsPageLogic(transactionId: string) {
    const { t } = useTranslation('documents');
    const { data: documents = [], isLoading, error: queryError, refetch } = useDocuments(transactionId);
    const requestDocument = useRequestDocument();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const { handleError } = useErrorHandler();

    // Adapt signature to accept stage parameter
    const handleRequestDocument = async (docType: DocumentTypeEnum, customTitle: string, instructions: string, stage: string) => {
        try {
            await requestDocument.mutateAsync({
                transactionId,
                data: {
                    docType: docType,
                    customTitle: docType === DocumentTypeEnum.OTHER ? customTitle : undefined,
                    expectedFrom: DocumentPartyEnum.BUYER, // Defaulting to BUYER, should be dynamic based on transaction
                    visibleToClient: true,
                    brokerNotes: instructions, // Mapping instructions to brokerNotes for now
                    stage: stage // Add stage field
                }
            });
            setIsModalOpen(false);
            toast.success(t('success.documentRequested'));
        } catch (error) {
            handleError(error);
            toast.error(t('errors.requestDocumentFailed', 'Failed to request document'));
        }
    };

    return {
        documents,
        isLoading,
        error: queryError,
        refetch,
        isModalOpen,
        setIsModalOpen,
        handleRequestDocument,
    };
}
