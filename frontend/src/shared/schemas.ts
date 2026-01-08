import { z } from "zod";
import { DocumentTypeEnum } from "@/features/documents/types";

// Transaction Schemas
export const transactionCreateSchema = z.object({
    transactionSide: z.enum(["buy", "sell"]),
    clientId: z.string().min(1, "errorSelectClient"),
    streetNumber: z.string().optional(),
    streetName: z.string().optional(),
    city: z.string().optional(),
    province: z.string().optional(),
    postalCode: z.string().optional(),
    initialStage: z.string().min(1, "errorSelectStage"),
}).superRefine((data, ctx) => {
    if (data.transactionSide === 'sell') {
        if (!data.streetNumber?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["streetNumber"] });
        if (!data.streetName?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["streetName"] });
        if (!data.city?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["city"] });
        if (!data.province?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["province"] });
        if (!data.postalCode?.trim()) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorRequired", path: ["postalCode"] });
        } else if (!/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(data.postalCode)) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "errorInvalidPostalCode", path: ["postalCode"] });
        }
    }
});

export type TransactionCreateFormValues = z.infer<typeof transactionCreateSchema>;

export const stageUpdateSchema = z.object({
    stage: z.string().min(1, "selectStageFirst"),
    note: z.string().trim().optional(),
    visibleToClient: z.boolean(),
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
