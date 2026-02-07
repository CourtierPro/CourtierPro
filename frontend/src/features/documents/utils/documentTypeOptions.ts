import { DocumentTypeEnum } from '@/features/documents/types';

const BUY_SIDE_DOCUMENT_TYPES = [
  DocumentTypeEnum.MORTGAGE_PRE_APPROVAL,
  DocumentTypeEnum.MORTGAGE_APPROVAL,
  DocumentTypeEnum.PROOF_OF_FUNDS,
  DocumentTypeEnum.PROOF_OF_INCOME,
  DocumentTypeEnum.ID_VERIFICATION,
  DocumentTypeEnum.GOVERNMENT_ID_1,
  DocumentTypeEnum.GOVERNMENT_ID_2,
  DocumentTypeEnum.BROKERAGE_CONTRACT,
  DocumentTypeEnum.PROMISE_TO_PURCHASE,
  DocumentTypeEnum.ACKNOWLEDGED_SELLERS_DECLARATION,
  DocumentTypeEnum.INSPECTION_REPORT,
  DocumentTypeEnum.INSURANCE_LETTER,
  DocumentTypeEnum.NOTARY_CONTACT_SHEET,
  DocumentTypeEnum.BANK_STATEMENT,
  DocumentTypeEnum.OTHER,
] as const;

const SELL_SIDE_DOCUMENT_TYPES = [
  DocumentTypeEnum.COMPARATIVE_MARKET_ANALYSIS,
  DocumentTypeEnum.SELLERS_DECLARATION,
  DocumentTypeEnum.CERTIFICATE_OF_LOCATION,
  DocumentTypeEnum.MUNICIPAL_TAX_BILLS,
  DocumentTypeEnum.MORTGAGE_BALANCE_STATEMENT,
  DocumentTypeEnum.SCHOOL_TAX_BILLS,
  DocumentTypeEnum.ACCEPTED_PROMISE_TO_PURCHASE,
  DocumentTypeEnum.INSPECTION_REPORT,
  DocumentTypeEnum.NOTARY_CONTACT_SHEET,
  DocumentTypeEnum.CURRENT_DEED_OF_SALE,
  DocumentTypeEnum.OTHER,
] as const;

export function getDocumentTypeOptions(side: 'buy' | 'sell'): DocumentTypeEnum[] {
  return side === 'buy'
    ? [...BUY_SIDE_DOCUMENT_TYPES]
    : [...SELL_SIDE_DOCUMENT_TYPES];
}

