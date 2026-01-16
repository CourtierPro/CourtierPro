import { DocumentCard } from "./DocumentCard";
import { type DocumentRequest } from "@/features/documents/types";

interface DocumentListProps {
    documents: DocumentRequest[];
    onUpload?: (document: DocumentRequest) => void;
    onReview?: (document: DocumentRequest) => void;
    onEdit?: (document: DocumentRequest) => void;
    focusDocumentId?: string | null;
    showBrokerNotes?: boolean;
}

export function DocumentList({ documents, onUpload, onReview, onEdit, focusDocumentId, showBrokerNotes = true }: DocumentListProps) {
    return (
        <div className="grid gap-4">
            {documents.map((doc) => (
                <DocumentCard
                    key={doc.requestId}
                    document={doc}
                    onUpload={onUpload}
                    onReview={onReview}
                    onEdit={onEdit}
                    isFocused={focusDocumentId === doc.requestId}
                    showBrokerNotes={showBrokerNotes}
                />
            ))}
        </div>
    );
}
