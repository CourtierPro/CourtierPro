import { z } from "zod";
import { DocumentTypeEnum } from "@/features/documents/types";

// Transaction Schemas
export const transactionCreateSchema = z.object({
    transactionSide: z.enum(["buy", "sell"]),
    clientId: z.string().min(1, "errorSelectClient"),
    streetNumber: z.string().trim().min(1, "errorRequired"),
    streetName: z.string().trim().min(1, "errorRequired"),
    city: z.string().trim().min(1, "errorRequired"),
    province: z.string().trim().min(1, "errorRequired"),
    postalCode: z.string().trim().regex(/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/, "errorInvalidPostalCode").transform((val) => val.toUpperCase()),
    initialStage: z.string().min(1, "errorSelectStage"),
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
