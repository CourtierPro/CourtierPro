import { FileText } from "lucide-react";
import { useTranslation } from "react-i18next";

export function EmptyDocumentsState() {
    const { t } = useTranslation("documents");

    return (
        <div className="flex flex-col items-center justify-center p-8 text-center bg-muted/30 rounded-lg border-2 border-dashed border-border min-h-[300px]">
            <div className="bg-background p-4 rounded-full shadow-sm mb-4">
                <FileText className="w-8 h-8 text-muted-foreground/60" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-1">
                {t("emptyState.title", "All Caught Up!")}
            </h3>
            <p className="text-sm text-muted-foreground max-w-xs mx-auto">
                {t("emptyState.description", "There are no documents required for you at this moment. We'll notify you if anything new is needed.")}
            </p>
        </div>
    );
}
