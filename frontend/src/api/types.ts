// API request/response types used by the frontend

export interface PropertyAddress {
  street: string;
  city?: string;
  province?: string;
  postalCode?: string;
}

export type TransactionSideEnum = 'BUY_SIDE' | 'SELL_SIDE';

export interface TransactionRequestDTO {
  clientId: string;
  brokerId?: string;
  side: TransactionSideEnum;
  initialStage: string;
  propertyAddress: PropertyAddress;
}

export interface TransactionResponseDTO {
  transactionId: string;
  clientId: string;
  brokerId: string;
  side: TransactionSideEnum;
  currentStage: string;
  status: string;
}
