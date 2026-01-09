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
  clientName: string;
  brokerId: string;
  side: TransactionSideEnum;
  currentStage: string;
  status: string;
  openedDate?: string;
}

export type TimelineEventType =
  | 'CREATED'
  | 'STAGE_CHANGE'
  | 'NOTE'
  | 'TRANSACTION_NOTE'
  | 'DOCUMENT_REQUESTED'
  | 'DOCUMENT_SUBMITTED'
  | 'DOCUMENT_APPROVED'
  | 'DOCUMENT_NEEDS_REVISION'
  | 'PROPERTY_ADDED'
  | 'PROPERTY_UPDATED'
  | 'PROPERTY_REMOVED';

export interface TransactionInfo {
  clientName?: string;
  address?: string;
  actorName?: string;
  stage?: string;
  previousStage?: string;
  newStage?: string;
}

export interface TimelineEntryDTO {
  id?: number;
  type: TimelineEventType;
  title?: string;
  note?: string;
  visibleToClient?: boolean;
  occurredAt?: string;
  addedByBrokerId?: string;
  docType?: string;
  status?: string;
  actorName?: string;
  transactionInfo?: TransactionInfo;
}

export interface NoteCreateRequest {
  transactionId: string;
  actorId: string;
  title: string;
  message: string;
  visibleToClient?: boolean;
}

export interface StageUpdateRequestDTO {
  stage: string;
  note?: string;
}

// ==================== PROPERTY TYPES ====================

export type OfferStatus =
  | 'OFFER_TO_BE_MADE'
  | 'OFFER_MADE'
  | 'COUNTERED'
  | 'ACCEPTED'
  | 'DECLINED';

export interface Property {
  propertyId: string;
  transactionId: string;
  address: PropertyAddress;
  askingPrice?: number;
  offerStatus: OfferStatus;
  offerAmount?: number;
  centrisNumber?: string;
  notes?: string; // Only present for brokers
  createdAt: string;
  updatedAt: string;
}

export interface PropertyRequestDTO {
  address: PropertyAddress;
  askingPrice?: number;
  offerStatus: OfferStatus;
  offerAmount?: number;
  centrisNumber?: string;
  notes?: string;
}
