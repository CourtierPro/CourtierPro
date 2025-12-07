import { useState } from 'react';
import { toast } from 'sonner';
import { useDocuments } from '../api/queries';
import { useRequestDocument } from '../api/mutations';
import { DocumentPartyEnum, DocumentTypeEnum } from '../types';
import { useErrorHandler } from '@/shared/hooks/useErrorHandler';

export function useDocumentsPageLogic(transactionId: string) {
    const { data: documents = [], isLoading, error: queryError, refetch } = useDocuments(transactionId);
    const requestDocument = useRequestDocument();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const { handleError } = useErrorHandler();

    const handleRequestDocument = async (documentTitle: string, instructions: string, _stage: string) => {
        try {
            await requestDocument.mutateAsync({
                transactionId,
                data: {
                    docType: DocumentTypeEnum.OTHER, // Defaulting to OTHER as modal doesn't support type selection yet
                    customTitle: documentTitle,
                    expectedFrom: DocumentPartyEnum.BUYER, // Defaulting to BUYER, should be dynamic based on transaction
                    visibleToClient: true,
                    brokerNotes: instructions // Mapping instructions to brokerNotes for now
                }
            });
            setIsModalOpen(false);
            toast.success("Document requested successfully");
        } catch (error) {
            handleError(error);
            toast.error("Failed to request document");
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
