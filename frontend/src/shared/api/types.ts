// Permissions possibles pour un participant (doit correspondre au backend)
export type ParticipantPermission =
  // Document Management
  | 'VIEW_DOCUMENTS'
  | 'EDIT_DOCUMENTS'

  // Property Management (Buyer side)
  | 'VIEW_PROPERTIES'
  | 'EDIT_PROPERTIES'

  // Stage Management
  | 'VIEW_STAGE'
  | 'EDIT_STAGE'

  // Offer Management (Seller side) & Property Offers (Buyer side)
  | 'VIEW_OFFERS'
  | 'EDIT_OFFERS'

  // Conditions Management
  | 'VIEW_CONDITIONS'
  | 'EDIT_CONDITIONS'

  // Notes Management
  | 'VIEW_NOTES'
  | 'EDIT_NOTES'

  // Search Criteria Management (Buyer side)
  | 'VIEW_SEARCH_CRITERIA'
  | 'EDIT_SEARCH_CRITERIA';

export type ParticipantRole = 'BROKER' | 'CO_BROKER' | 'NOTARY' | 'LAWYER' | 'BUYER' | 'SELLER' | 'OTHER';

export interface TransactionParticipant {
  id: string;
  transactionId: string;
  name: string;
  role: ParticipantRole;
  email?: string;
  phoneNumber?: string;
  permissions?: ParticipantPermission[];
  isSystem?: boolean; // Ajouté pour la protection UI et exclusion autocomplete
}

export interface UpdateParticipantRequestDTO {
  name?: string;
  role?: ParticipantRole;
  email?: string;
  phoneNumber?: string;
  permissions?: ParticipantPermission[];
}

export interface AddParticipantRequestDTO {
  name: string;
  role: ParticipantRole;
  email?: string;
  phoneNumber?: string;
  permissions?: ParticipantPermission[];
}
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
  | 'STAGE_ROLLBACK'
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
  | 'PROPERTY_OFFER_MADE'
  | 'PROPERTY_OFFER_UPDATED'
  | 'OFFER_DOCUMENT_UPLOADED'
  | 'CONDITION_ADDED'
  | 'CONDITION_UPDATED'
  | 'CONDITION_REMOVED'
  | 'CONDITION_SATISFIED'
  | 'CONDITION_FAILED'
  | 'APPOINTMENT_CONFIRMED'
  | 'APPOINTMENT_CANCELLED'
  | 'APPOINTMENT_DECLINED'
  | 'APPOINTMENT_RESCHEDULED'
  | 'APPOINTMENT_REQUESTED';

export interface TransactionInfo {
  clientName?: string;
  address?: string;
  actorName?: string;
  stage?: string;
  previousStage?: string;
  newStage?: string;
  reason?: string;
  // Appointment-related fields
  appointmentTitle?: string;
  appointmentDate?: string;
  // Offer-related fields
  buyerName?: string;
  offerAmount?: number;
  offerStatus?: string;
  previousOfferStatus?: string;
  // Condition-related fields
  conditionType?: string;
  conditionCustomTitle?: string;
  conditionDescription?: string;
  conditionDeadline?: string;
  conditionPreviousStatus?: string;
  conditionNewStatus?: string;
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
  reason?: string;
}

// ==================== PROPERTY TYPES ====================

export type PropertyStatus =
  | 'SUGGESTED'
  | 'INTERESTED'
  | 'NOT_INTERESTED'
  | 'NEEDS_INFO';

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
  status?: PropertyStatus; // Optional for legacy compatibility, but usually present
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
  status?: PropertyStatus;
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

export type ClientOfferDecision = 'ACCEPT' | 'DECLINE' | 'COUNTER';

export interface Offer {
  offerId: string;
  transactionId: string;
  buyerName: string;
  offerAmount?: number;
  status: ReceivedOfferStatus;
  notes?: string;
  expiryDate?: string;
  // Client decision fields
  clientDecision?: ClientOfferDecision;
  clientDecisionAt?: string;
  clientDecisionNotes?: string;
  documents?: OfferDocument[];
  conditions?: Condition[];
  createdAt: string;
  updatedAt: string;
}

export interface OfferRequestDTO {
  buyerName: string;
  offerAmount?: number;
  status: ReceivedOfferStatus;
  expiryDate?: string;
  notes?: string;
  conditionIds?: string[];
}

export interface ClientOfferDecisionDTO {
  decision: ClientOfferDecision;
  notes?: string;
}

// ==================== OFFER DOCUMENT TYPES ====================

export interface OfferDocument {
  documentId: string;
  fileName: string;
  mimeType?: string;
  sizeBytes?: number;
  fileSize?: number; // Kept for compatibility if backend sends this
  createdAt: string;
}

// ==================== UNIFIED DOCUMENT TYPES ====================

export type DocumentSource = 'CLIENT_UPLOAD' | 'OFFER_ATTACHMENT' | 'PROPERTY_OFFER_ATTACHMENT';

export interface UnifiedDocument {
  documentId: string;
  fileName: string;
  mimeType?: string;
  sizeBytes?: number;
  uploadedAt: string;
  source: DocumentSource;
  sourceId: string;
  sourceName: string;
  status?: string;
}

// ==================== OFFER REVISION TYPES ====================

export interface OfferRevision {
  revisionId: string;
  offerId: string;
  revisionNumber: number;
  previousAmount?: number;
  newAmount?: number;
  previousStatus?: string;
  newStatus?: string;
  createdAt: string;
}

// ==================== PROPERTY OFFER TYPES (for buyer transactions) ====================

export type BuyerOfferStatus =
  | 'OFFER_MADE'
  | 'COUNTERED'
  | 'ACCEPTED'
  | 'DECLINED'
  | 'EXPIRED'
  | 'WITHDRAWN';

export type CounterpartyResponse =
  | 'PENDING'
  | 'ACCEPTED'
  | 'COUNTERED'
  | 'DECLINED'
  | 'NO_RESPONSE';

export interface PropertyOffer {
  propertyOfferId: string;
  propertyId: string;
  offerRound: number;
  offerAmount: number;
  status: BuyerOfferStatus;
  counterpartyResponse?: CounterpartyResponse;
  expiryDate?: string;
  notes?: string;
  documents: OfferDocument[];
  conditions?: Condition[];
  createdAt: string;
  updatedAt: string;
}

export interface PropertyOfferRequestDTO {
  offerAmount: number;
  status?: BuyerOfferStatus;
  counterpartyResponse?: CounterpartyResponse;
  expiryDate?: string;
  notes?: string;
  conditionIds?: string[];
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

// ==================== SEARCH CRITERIA TYPES (for buyer transactions) ====================

export type SearchCriteriaPropertyType =
  | 'SINGLE_FAMILY_HOME'
  | 'CONDO'
  | 'LOFT_STUDIO'
  | 'PLEX'
  | 'INTERGENERATIONAL'
  | 'MOBILE_HOME'
  | 'HOBBY_FARM'
  | 'COTTAGE'
  | 'LOT';

export type SearchCriteriaBuildingStyle =
  | 'NEW_CONSTRUCTION'
  | 'CENTURY_HISTORIC'
  | 'BUNGALOW'
  | 'MORE_THAN_ONE_STOREY'
  | 'SPLIT_LEVEL'
  | 'DETACHED'
  | 'SEMI_DETACHED'
  | 'ATTACHED';

export type SearchCriteriaPlexType =
  | 'DUPLEX'
  | 'TRIPLEX'
  | 'QUADRUPLEX'
  | 'QUINTUPLEX';

export type QuebecRegion =
  | 'BAS_SAINT_LAURENT'
  | 'SAGUENAY_LAC_SAINT_JEAN'
  | 'CAPITALE_NATIONALE'
  | 'MAURICIE'
  | 'ESTRIE'
  | 'MONTREAL'
  | 'OUTAOUAIS'
  | 'ABITIBI_TEMISCAMINGUE'
  | 'COTE_NORD'
  | 'NORD_DU_QUEBEC'
  | 'GASPESIE_ILES_DE_LA_MADELEINE'
  | 'CHAUDIERE_APPALACHES'
  | 'LAVAL'
  | 'LANAUDIERE'
  | 'LAURENTIDES'
  | 'MONTEREGIE'
  | 'CENTRE_DU_QUEBEC';

export type AreaUnit = 'SQFT' | 'SQM';

export interface SearchCriteria {
  searchCriteriaId: string;
  transactionId: string;

  // Property Types
  propertyTypes?: SearchCriteriaPropertyType[];

  // Features
  minBedrooms?: number;
  minBathrooms?: number;
  minParkingSpaces?: number;
  minGarages?: number;
  hasPool?: boolean;
  hasElevator?: boolean;
  adaptedForReducedMobility?: boolean;
  hasWaterfront?: boolean;
  hasAccessToWaterfront?: boolean;
  hasNavigableWater?: boolean;
  isResort?: boolean;
  petsAllowed?: boolean;
  smokingAllowed?: boolean;

  // Building
  minLivingArea?: number;
  maxLivingArea?: number;
  livingAreaUnit?: AreaUnit;
  minYearBuilt?: number;
  maxYearBuilt?: number;
  buildingStyles?: SearchCriteriaBuildingStyle[];

  // Plex Types
  plexTypes?: SearchCriteriaPlexType[];

  // Other Criteria
  minLandArea?: number;
  maxLandArea?: number;
  landAreaUnit?: AreaUnit;
  newSince?: string;
  moveInDate?: string;
  openHousesOnly?: boolean;
  repossessionOnly?: boolean;

  // Price Range
  minPrice?: number;
  maxPrice?: number;

  // Regions
  regions?: QuebecRegion[];

  // Timestamps
  createdAt?: string;
  updatedAt?: string;
}

export interface SearchCriteriaRequestDTO {
  propertyTypes?: SearchCriteriaPropertyType[];
  minBedrooms?: number;
  minBathrooms?: number;
  minParkingSpaces?: number;
  minGarages?: number;
  hasPool?: boolean;
  hasElevator?: boolean;
  adaptedForReducedMobility?: boolean;
  hasWaterfront?: boolean;
  hasAccessToWaterfront?: boolean;
  hasNavigableWater?: boolean;
  isResort?: boolean;
  petsAllowed?: boolean;
  smokingAllowed?: boolean;
  minLivingArea?: number;
  maxLivingArea?: number;
  livingAreaUnit?: AreaUnit;
  minYearBuilt?: number;
  maxYearBuilt?: number;
  buildingStyles?: SearchCriteriaBuildingStyle[];
  plexTypes?: SearchCriteriaPlexType[];
  minLandArea?: number;
  maxLandArea?: number;
  landAreaUnit?: AreaUnit;
  newSince?: string;
  moveInDate?: string;
  openHousesOnly?: boolean;
  repossessionOnly?: boolean;
  minPrice?: number;
  maxPrice?: number;
  regions?: QuebecRegion[];
}
