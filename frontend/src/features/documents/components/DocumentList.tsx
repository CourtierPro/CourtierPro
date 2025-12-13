import { DocumentCard } from "./DocumentCard";
import { type DocumentRequest } from "@/features/documents/types";

interface DocumentListProps {
    documents: DocumentRequest[];
    onUpload?: (document: DocumentRequest) => void;
    onReview?: (document: DocumentRequest) => void;
    showBrokerNotes?: boolean;
}

export function DocumentList({ documents, onUpload, onReview, showBrokerNotes = true }: DocumentListProps) {
    return (
        <div className="grid gap-4">
            {documents.map((doc) => (
                <DocumentCard
                    key={doc.requestId}
                    document={doc}
                    onUpload={onUpload}
                    onReview={onReview}
                    showBrokerNotes={showBrokerNotes}
                />
            ))}
        </div>
    );
}
