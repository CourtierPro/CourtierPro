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
  centrisNumber?: string;
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
  centrisNumber?: string;
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
  | 'STATUS_CHANGE'
  | 'PROPERTY_ADDED'
  | 'PROPERTY_UPDATED'
  | 'PROPERTY_REMOVED'
  | 'OFFER_RECEIVED'
  | 'OFFER_UPDATED'
  | 'OFFER_REMOVED'
  | 'CONDITION_ADDED'
  | 'CONDITION_UPDATED'
  | 'CONDITION_REMOVED'
  | 'CONDITION_SATISFIED'
  | 'CONDITION_FAILED';

export interface TransactionInfo {
  clientName?: string;
  address?: string;
  actorName?: string;
  stage?: string;
  previousStage?: string;
  newStage?: string;
  // Offer-related fields
  buyerName?: string;
  offerAmount?: number;
  offerStatus?: string;
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

export type PropertyOfferStatus =
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
  offerStatus: PropertyOfferStatus;
  offerAmount?: number;
  centrisNumber?: string;
  notes?: string; // Only present for brokers
  createdAt: string;
  updatedAt: string;
}

export interface PropertyRequestDTO {
  address: PropertyAddress;
  askingPrice?: number;
  offerStatus: PropertyOfferStatus;
  offerAmount?: number;
  centrisNumber?: string;
  notes?: string;
}

// ==================== OFFER TYPES (for seller transactions) ====================

export type ReceivedOfferStatus =
  | 'PENDING'
  | 'UNDER_REVIEW'
  | 'COUNTERED'
  | 'ACCEPTED'
  | 'DECLINED';

export interface Offer {
  offerId: string;
  transactionId: string;
  buyerName: string;
  offerAmount?: number;
  status: ReceivedOfferStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OfferRequestDTO {
  buyerName: string;
  offerAmount?: number;
  status: ReceivedOfferStatus;
  notes?: string;
}

// ==================== CONDITION TYPES ====================

export type ConditionType = 'FINANCING' | 'INSPECTION' | 'SALE_OF_PROPERTY' | 'OTHER';
export type ConditionStatus = 'PENDING' | 'SATISFIED' | 'FAILED';

export interface Condition {
  conditionId: string;
  transactionId: string;
  type: ConditionType;
  customTitle?: string;
  description: string;
  deadlineDate: string;
  status: ConditionStatus;
  satisfiedAt?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConditionRequestDTO {
  type: ConditionType;
  customTitle?: string;
  description: string;
  deadlineDate: string;
  status?: ConditionStatus;
  notes?: string;
}

