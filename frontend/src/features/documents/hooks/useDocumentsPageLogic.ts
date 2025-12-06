import { useState } from 'react';
import { toast } from 'sonner';
import { useDocuments } from '../api/queries';
import { useRequestDocument, type RequestDocumentDTO } from '../api/mutations';

export function useDocumentsPageLogic() {
    const { data: documents = [], isLoading, error, refetch } = useDocuments();
    const requestDocument = useRequestDocument();
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleRequestDocument = async (documentTitle: string, instructions: string) => {
        try {
            await requestDocument.mutateAsync({
                title: documentTitle,
                message: instructions,
                recipientEmail: 'client@example.com', // Hardcoded for now, should come from context
                transactionId: 'tx-1', // Hardcoded for now, should come from context/params
            } as RequestDocumentDTO);
            setIsModalOpen(false);
            toast.success("Document requested successfully");
        } catch (err) {
            toast.error("Failed to request document");
        }
    };

    return {
        documents,
        isLoading,
        error,
        refetch,
        isModalOpen,
        setIsModalOpen,
        handleRequestDocument,
    };
}
