import type { DocumentRequest } from "../types";
import type { TFunction } from "i18next";

export const formatDocumentTitle = (document: DocumentRequest, t: TFunction): string => {
    const type = document.docType;
    const custom = document.customTitle?.trim();

    // If custom title exists, checks if it is just the raw enum value.
    // Use strict equality as well as checking against stringified version just in case.
    if (custom && custom !== type && custom !== type.toString()) {
        return custom;
    }

    return t(`types.${type}`);
};
