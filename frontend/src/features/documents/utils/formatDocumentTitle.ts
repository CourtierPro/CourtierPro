import { DocumentTypeEnum, type Document, type DocumentTypeEnum as DocumentType } from "../types";
import type { TFunction } from "i18next";

const DOCUMENT_TYPE_VALUES = new Set<string>(Object.values(DocumentTypeEnum));

export const normalizeDocumentType = (value?: string | null): DocumentType | null => {
    if (!value) return null;
    const trimmed = value.trim();
    if (!trimmed) return null;

    if (DOCUMENT_TYPE_VALUES.has(trimmed)) {
        return trimmed as DocumentType;
    }

    const normalized = trimmed
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/&/g, " AND ")
        .replace(/['’`]/g, "")
        .replace(/[^A-Za-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "")
        .replace(/_+/g, "_")
        .toUpperCase();

    return DOCUMENT_TYPE_VALUES.has(normalized) ? (normalized as DocumentType) : null;
};

interface LocalizedDocumentTitleParams {
    docType?: string | null;
    customTitle?: string | null;
    fallbackLabel?: string | null;
}

export const getLocalizedDocumentTitle = (
    t: TFunction,
    { docType, customTitle, fallbackLabel }: LocalizedDocumentTitleParams
): string => {
    const custom = customTitle?.trim();
    const fallback = fallbackLabel?.trim();
    const normalizedType = normalizeDocumentType(docType ?? fallback);

    if (normalizedType === DocumentTypeEnum.OTHER) {
        if (custom) {
            return custom;
        }
        if (fallback && normalizeDocumentType(fallback) !== DocumentTypeEnum.OTHER) {
            return fallback;
        }
        return t(`types.${DocumentTypeEnum.OTHER}`, { defaultValue: fallback || DocumentTypeEnum.OTHER });
    }

    if (normalizedType) {
        return t(`types.${normalizedType}`, { defaultValue: fallback || normalizedType });
    }

    if (custom) {
        return custom;
    }

    return fallback || "";
};

export const formatDocumentTitle = (document: Document, t: TFunction): string => {
    return getLocalizedDocumentTitle(t, {
        docType: document.docType,
        customTitle: document.customTitle,
        fallbackLabel: document.customTitle || document.docType,
    });
};
