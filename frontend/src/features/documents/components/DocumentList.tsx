import { DocumentCard } from "./DocumentCard";
import { type DocumentRequest } from "@/features/documents/types";

interface DocumentListProps {
    documents: DocumentRequest[];
    onUpload?: (document: DocumentRequest) => void;
    onReview?: (document: DocumentRequest) => void;
}

export function DocumentList({ documents, onUpload, onReview }: DocumentListProps) {
    return (
        <div className="grid gap-4">
            {documents.map((doc) => (
                <DocumentCard key={doc.requestId} document={doc} onUpload={onUpload} onReview={onReview} />
            ))}
        </div>
    );
}
