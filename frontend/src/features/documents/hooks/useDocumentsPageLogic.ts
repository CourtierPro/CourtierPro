import { useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';
import { useDocuments } from '../api/queries';
import { useCreateDocument } from '../api/mutations';
import { DocumentPartyEnum, DocumentTypeEnum, DocumentFlowEnum } from '../types';
import { useErrorHandler } from '@/shared/hooks/useErrorHandler';

export function useDocumentsPageLogic(transactionId: string) {
    const { t } = useTranslation('documents');
    const { data: documents = [], isLoading, error: queryError, refetch } = useDocuments(transactionId);
    const createDocument = useCreateDocument();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const { handleError } = useErrorHandler();

    // Adapt signature to accept stage, conditionIds, status, and flow parameters
    // Returns document ID for file upload flow
    const handleRequestDocument = async (
        docType: DocumentTypeEnum,
        customTitle: string,
        instructions: string,
        stage: string,
        conditionIds: string[] = [],
        dueDate?: Date,
        status?: 'DRAFT' | 'REQUESTED' | 'SUBMITTED',
        flow?: DocumentFlowEnum
    ): Promise<string | undefined> => {
        try {
            const result = await createDocument.mutateAsync({
                transactionId,
                data: {
                    docType: docType,
                    customTitle: docType === DocumentTypeEnum.OTHER ? customTitle : undefined,
                    expectedFrom: DocumentPartyEnum.BUYER, // Defaulting to BUYER, should be dynamic based on transaction
                    visibleToClient: true,
                    brokerNotes: instructions, // Mapping instructions to brokerNotes for now
                    stage: stage,
                    conditionIds: conditionIds.length > 0 ? conditionIds : undefined,
                    dueDate: dueDate,
                    status: status, // Pass status to API (DRAFT, REQUESTED, or SUBMITTED)
                    flow: flow // Pass flow to API (REQUEST or UPLOAD)
                }
            });
            setIsModalOpen(false);
            const successMessage = status === 'DRAFT'
                ? t('success.draftSaved', 'Document saved as draft')
                : t('success.documentRequested');
            toast.success(successMessage);
            return result.documentId;
        } catch (error) {
            handleError(error);
            toast.error(t('errors.requestDocumentFailed', 'Failed to request document'));
            return undefined;
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
