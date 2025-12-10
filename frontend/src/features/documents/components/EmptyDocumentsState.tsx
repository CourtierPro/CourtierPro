import { FileText } from "lucide-react";
import { useTranslation } from "react-i18next";

export function EmptyDocumentsState() {
    const { t } = useTranslation("documents");

    return (
        <div className="flex flex-col items-center justify-center p-8 text-center bg-gray-50/50 rounded-lg border-2 border-dashed border-gray-200 min-h-[300px]">
            <div className="bg-white p-4 rounded-full shadow-sm mb-4">
                <FileText className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-1">
                {t("emptyState.title", "All Caught Up!")}
            </h3>
            <p className="text-sm text-gray-500 max-w-xs mx-auto">
                {t("emptyState.description", "There are no documents required for you at this moment. We'll notify you if anything new is needed.")}
            </p>
        </div>
    );
}
