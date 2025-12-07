import { Section } from "@/shared/components/branded/Section";
import { type Document } from "@/features/documents/api/queries";

interface DocumentCardProps {
    document: Document;
}

export function DocumentCard({ document }: DocumentCardProps) {
    return (
        <Section className="flex justify-between items-center p-4">
            <div>
                <h3 className="font-semibold">{document.title}</h3>
                <p className="text-sm text-muted-foreground">{document.uploadedAt}</p>
                <p className="text-sm">{document.type}</p>
            </div>
            <div className="text-right">
                <span className="inline-block px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                    {document.status}
                </span>
            </div>
        </Section>
    );
}
