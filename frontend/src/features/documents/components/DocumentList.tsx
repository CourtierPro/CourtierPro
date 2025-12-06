import { DocumentCard } from "./DocumentCard";
import { type Document } from "@/features/documents/api/queries";

interface DocumentListProps {
    documents: Document[];
}

export function DocumentList({ documents }: DocumentListProps) {
    return (
        <div className="grid gap-4">
            {documents.map((doc) => (
                <DocumentCard key={doc.id} document={doc} />
            ))}
        </div>
    );
}
