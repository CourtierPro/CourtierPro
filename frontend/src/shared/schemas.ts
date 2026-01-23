import { z } from "zod";
import { DocumentTypeEnum } from "@/features/documents/types";
import { isValidPostalCode, normalizePostalCode } from "@/shared/utils/postal-code";

// Transaction Schemas
export const transactionCreateSchema = z.object({
    transactionSide: z.enum(["buy", "sell"]),
    clientId: z.string().min(1, "errorSelectClient"),
    streetNumber: z.string().optional(),
    streetName: z.string().optional(),
    city: z.string().optional(),
    province: z.string().optional(),
    postalCode: z.string().optional(),
    centrisNumber: z.string().optional(),
    initialStage: z.string().min(1, "errorSelectStage"),
}).superRefine((data, ctx) => {
    if (data.transactionSide === 'sell') {
        if (!data.streetNumber?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["streetNumber"] });
        if (!data.streetName?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["streetName"] });
        if (!data.city?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["city"] });
        if (!data.province?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["province"] });
        if (!data.postalCode?.trim()) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["postalCode"] });
        } else if (!isValidPostalCode(data.postalCode)) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorInvalidPostalCode", path: ["postalCode"] });
        }
        if (!data.centrisNumber?.trim()) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["centrisNumber"] });
        }
    }
});

// Use z.input to get the input type (what the form tracks)
export type TransactionCreateFormValues = z.input<typeof transactionCreateSchema>;

// Helper function to normalize postal code before submission
export function normalizeTransactionFormData(data: TransactionCreateFormValues) {
    return {
        ...data,
        postalCode: data.postalCode ? normalizePostalCode(data.postalCode) : undefined,
        centrisNumber: data.centrisNumber?.trim() || undefined,
    };
}


export const stageUpdateSchema = z.object({
    stage: z.string().min(1, "selectStageFirst"),
    note: z.string().trim().optional(),
    reason: z.string().trim().optional(),
});

export type StageUpdateFormValues = z.infer<typeof stageUpdateSchema>;

// Document Schemas
export const requestDocumentSchema = z.object({
    docType: z.nativeEnum(DocumentTypeEnum),
    customTitle: z.string().trim().optional(),
    instructions: z.string().trim().optional(),
    stage: z.string().min(1, "stageRequired"),
}).superRefine((data, ctx) => {
    if (data.docType === DocumentTypeEnum.OTHER && (!data.customTitle || data.customTitle.trim().length === 0)) {
        ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "documentTitleRequired",
            path: ["customTitle"],
        });
    }
});

export type RequestDocumentFormValues = z.infer<typeof requestDocumentSchema>;

// Admin/User Schemas
export const inviteUserSchema = z.object({
    email: z.string().trim().email("inviteUser_emailInvalid"),
    firstName: z.string().trim().min(1, "inviteUser_fillAllFields"),
    lastName: z.string().trim().min(1, "inviteUser_fillAllFields"),
    role: z.enum(["BROKER", "CLIENT", "ADMIN"]),
    preferredLanguage: z.enum(["en", "fr"]),
});

export type InviteUserFormValues = z.infer<typeof inviteUserSchema>;
