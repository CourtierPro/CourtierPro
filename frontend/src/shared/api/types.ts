
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
  openedDate?: string;
}

export interface TimelineEntryDTO {
  id?: number;
  type: 'CREATED' | 'STAGE_CHANGE' | 'NOTE';
  title?: string;
  note?: string;
  visibleToClient?: boolean;
  occurredAt?: string;
  addedByBrokerId?: string;
}

export interface NoteCreateRequest {
  transactionId: string;
  actorId: string;
  title: string;
  message: string;
  visibleToClient?: boolean;
}
