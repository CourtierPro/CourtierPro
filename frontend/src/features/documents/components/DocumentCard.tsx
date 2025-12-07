import { Section } from "@/shared/components/branded/Section";
import { type DocumentRequest } from "@/features/documents/types";
import { format } from "date-fns";

interface DocumentCardProps {
    document: DocumentRequest;
}

export function DocumentCard({ document }: DocumentCardProps) {
    const title = document.customTitle || document.docType;
    const date = document.lastUpdatedAt ? format(new Date(document.lastUpdatedAt), 'PPP') : 'N/A';

    return (
        <Section className="flex justify-between items-center p-4">
            <div>
                <h3 className="font-semibold">{title}</h3>
                <p className="text-sm text-muted-foreground">{date}</p>
                <p className="text-sm">{document.docType}</p>
            </div>
            <div className="text-right">
                <span className="inline-block px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                    {document.status}
                </span>
            </div>
        </Section>
    );
}
