# Manage Transaction Documents - Owned Use Case

## System-Wide Artifacts

### Use Case Diagram

[Paste Use Case Diagram]

### DDD Domain Model

[Paste DDD Domain Model Diagram]

### C4 Level 1 - System Context Diagram

[Paste C4 Level 1 Diagram]

### C4 Level 2 - Container Architecture Diagram

[Paste C4 Level 2 Diagram]

### C4 Level 3 - Frontend Component Diagram

[Paste C4 Level 3 Frontend Diagram]

### C4 Level 3 - Backend Component Diagram

[Paste C4 Level 3 Backend Diagram]

### C4 Level 3 - Database Schema Component Diagram

[Paste C4 Level 3 Database Diagram]

---

## Section 1: Use Case Definition & Requirements

### 1.1 Activity Diagrams

**REQUEST Flow** (Broker requests, Client uploads):

[Paste Activity Diagram - Request Flow]

**UPLOAD Flow** (Broker uploads, Client downloads):

[Paste Activity Diagram - Upload Flow]

### 1.2 Fully Developed Use Case

| Field | Description |
|---|---|
| **Use Case Name** | Manage Transaction Documents |
| **Scenario** | A broker creates, tracks, and reviews documents associated with a real estate transaction. Depending on the document flow, the broker either requests documents from the client (REQUEST flow) or uploads documents to share with the client (UPLOAD flow). The client responds by uploading requested files or downloading shared documents. The broker reviews submissions, approves or requests revisions, and manages a stage-based document checklist to track transaction readiness. |
| **Triggering Event** | A broker navigates to the documents section of an active transaction and initiates a document action (request, upload, review, checklist check, or reminder). |
| **Actors** | **Primary:** Broker (initiates most document workflows) |
| | **Secondary:** Client (uploads documents, downloads shared files) |
| | **Supporting:** System (CourtierPro backend, Cloudflare R2, AWS SES, Auth0) |
| **Related Use Cases** | Manage Transactions (parent - documents belong to transactions), View Broker Analytics Dashboard (outstanding documents feed broker dashboard), Manage Appointments (both are sub-workflows of a transaction) |
| **Stakeholders** | Broker (needs organized document tracking per transaction stage), Client (needs clear instructions and timely notifications), Brokerage Admin (needs audit trail and compliance oversight) |
| **Preconditions** | 1. The broker is authenticated via Auth0 and has the BROKER role. |
| | 2. An active transaction exists (status = ACTIVE) with a linked client. |
| | 3. The broker has EDIT_DOCUMENTS permission for the transaction. |
| | 4. The client is authenticated (for client-side actions) with CLIENT role. |
| **Postconditions** | 1. Documents are created and tracked with proper status lifecycle (DRAFT -> REQUESTED -> SUBMITTED -> APPROVED/NEEDS_REVISION/REJECTED). |
| | 2. Files are securely stored in Cloudflare R2 with version history preserved. |
| | 3. All parties are notified via bilingual email and in-app notifications. |
| | 4. Stage checklist reflects document completion status (auto-checked or manually overridden). |
| | 5. Timeline entries record all significant document actions. |
| | 6. Soft-deleted documents remain in the database for audit purposes. |

#### Flow of Activities

| Step | Actor Action | System Response |
|---|---|---|
| **Main Flow: REQUEST (Broker requests, Client uploads)** | | |
| 1. | Broker navigates to transaction documents page. | 1.1. System verifies broker authentication and EDIT_DOCUMENTS permission via TransactionAccessUtils. Returns list of documents for the transaction, filtering out soft-deleted records. |
| 2. | Broker clicks "Request Document" and fills form: document type (DocumentTypeEnum), expected from party (DocumentPartyEnum), transaction stage (StageEnum), broker notes, due date, and optionally sets requiresSignature=true. | 2.1. System validates the form inputs. Creates a Document entity with status=DRAFT (or REQUESTED if broker selects immediate send), flow=REQUEST. Stores in PostgreSQL via DocumentRepository. Records timeline entry. Returns DocumentResponseDTO. |
| 3. | (Optional) Broker uploads a reference file to the DRAFT document (e.g., a template for the client to fill). | 3.1. System validates file (type, size). Uploads to Cloudflare R2 via ObjectStorageService. Creates DocumentVersion with uploaderType=BROKER. Status remains DRAFT. |
| 4. | Broker clicks "Send Request" to transition from DRAFT to REQUESTED. | 4.1. System validates: if requiresSignature=true, at least one version (source document) must exist. Updates status to REQUESTED. Sends bilingual email to client via EmailService.sendDocumentRequestedNotification (respects client's preferredLanguage and emailNotificationsEnabled). Creates in-app Notification (category=DOCUMENT). Records timeline entry. |
| 5. | Client receives email and/or in-app notification, navigates to transaction documents. | 5.1. System authenticates client, verifies transaction participation. Returns document list filtered for client visibility (DRAFT documents hidden, only visibleToClient=true shown). |
| 6. | Client selects the requested document and uploads a file. If requiresSignature=true, client first downloads the source document, signs it externally, then uploads the signed version. | 6.1. System validates file constraints (type, size). Uploads file to Cloudflare R2 via ObjectStorageService. Creates DocumentVersion with uploaderType=CLIENT. Updates document status from REQUESTED to SUBMITTED. Sends email to broker via EmailService.sendDocumentSubmittedNotification. Creates in-app notification for broker. Records timeline entry. |
| 7. | Broker receives notification, navigates to the submitted document, and downloads the file for review via pre-signed URL. | 7.1. System generates a time-limited pre-signed URL from Cloudflare R2 via ObjectStorageService.getPreSignedUrl. Returns URL to frontend. |
| 8. | Broker reviews the document and selects a decision: APPROVED, NEEDS_REVISION, or REJECTED, with optional comments. | 8.1. System validates document is in SUBMITTED status. Updates status to the selected decision. Stores broker comments in brokerNotes. If APPROVED: auto-checks the corresponding stage checklist item via ChecklistStateRepository. Sends bilingual status update email to client via EmailService.sendDocumentStatusUpdatedNotification. Creates in-app notification. Records timeline entry. |
| **Alternate Flow: UPLOAD (Broker uploads, Client downloads)** | | |
| A1. | Broker clicks "Upload for Client" and fills form: document type, stage, notes. Uploads the document file. | A1.1. System creates Document with flow=UPLOAD, status=DRAFT. Validates and uploads file to R2. Creates DocumentVersion with uploaderType=BROKER. |
| A2. | Broker clicks "Share with Client." | A2.1. System updates status from DRAFT to SUBMITTED. Sets visibleToClient=true. Sends email notification to client. Creates in-app notification. Auto-checks stage checklist item. |
| A3. | Client receives notification, views and downloads the shared document. | A3.1. System generates pre-signed download URL. Client downloads document. |
| **Alternate Flow: Revision Cycle** | | |
| R1. | After NEEDS_REVISION decision, client receives notification with feedback. | R1.1. System status is NEEDS_REVISION with broker's feedback in brokerNotes. |
| R2. | Client uploads a corrected file. | R2.1. System creates new DocumentVersion (uploaderType=CLIENT). Updates status back to SUBMITTED. Notifies broker. (Repeats from step 7 of main flow.) |
| **Alternate Flow: Stage Checklist Management** | | |
| C1. | Broker navigates to the stage checklist for a given transaction stage. | C1.1. System builds checklist from StageDocumentTemplateRegistry templates for the specified stage. For each template item, finds matching documents by templateKey. Auto-checks items where matching document is APPROVED (REQUEST flow) or SUBMITTED (UPLOAD flow). Overlays manual override states from ChecklistStateRepository. Returns StageChecklistResponseDTO. |
| C2. | Broker manually toggles a checklist item (override). | C2.1. System creates or updates TransactionStageChecklistState with manualChecked value and manualCheckedBy userId. Returns updated checklist. |
| **Alternate Flow: Outstanding Documents & Reminders** | | |
| O1. | Broker views outstanding documents dashboard. | O1.1. System queries documents with status REQUESTED or NEEDS_REVISION where dueDate has passed. Computes daysOutstanding. Returns List<OutstandingDocumentDTO> with client name, email, transaction address. |
| O2. | Broker sends a reminder for an overdue document. | O2.1. System re-sends document request email to client using EmailService.sendDocumentRequestedNotification with same parameters. |

#### Inclusions

- **Authenticate User**: Every action requires valid Auth0 JWT token. The UserContextFilter maps auth0UserId to internal UUID.
- **Validate Transaction Access**: Before any document operation, TransactionAccessUtils verifies the caller has appropriate permissions (VIEW_DOCUMENTS or EDIT_DOCUMENTS) for the transaction.
- **Upload File to R2**: File upload is included in both submitDocument and uploadFileToDocument operations. ObjectStorageService handles S3-compatible API calls to Cloudflare R2.
- **Send Notification**: Document state changes trigger both email (EmailService) and in-app (NotificationService) notifications. Email respects user's preferredLanguage and emailNotificationsEnabled settings.

#### Extensions

- **Signature Request Flow**: When requiresSignature=true on a REQUEST-flow document, the broker must attach a source document before sending the request. The client downloads, signs externally, and uploads the signed copy. The email template differentiates signature requests from standard document requests.
- **Stage Checklist Auto-Check**: When a document reaches APPROVED (REQUEST flow) or SUBMITTED (UPLOAD flow), the system automatically checks the corresponding checklist item via the StageDocumentTemplateRegistry mapping. Brokers can also manually override checklist items.
- **Outstanding Documents Dashboard**: Broker can view all overdue documents across all transactions, filtered by REQUESTED/NEEDS_REVISION status with past due dates.

#### Exceptions

- **E1: File validation fails** - File exceeds size limit or has invalid MIME type. System returns 400 error with validation message. Document status unchanged.
- **E2: Transaction not found or inactive** - System returns 404 error. No document created.
- **E3: Permission denied** - User lacks EDIT_DOCUMENTS permission. System returns 403 Forbidden.
- **E4: Invalid status transition** - e.g., reviewing a document not in SUBMITTED status, or sending a request for a non-DRAFT document. System returns 400 with descriptive error.
- **E5: Missing source document for signature** - Broker tries to send a requiresSignature=true document request without first uploading a source document. System returns 400 error.
- **E6: Document not found** - documentId does not exist or is soft-deleted. System returns 404.

### 1.3 Alignment Explanation

#### How the use case connects to the overall system context

The "Manage Transaction Documents" use case is a central sub-workflow of the broader CourtierPro system, which serves as a bilingual broker-client management platform for Quebec real estate brokerages. As shown in the **Use Case Diagram**, "Manage Transaction Documents" is connected to the parent "Manage Transactions" use case via an `<<include>>` relationship - every active transaction has associated documents that must be managed throughout the transaction lifecycle. The **C4 Level 1** diagram shows that documents flow between all three user roles (Broker, Client, Admin) through the CourtierPro system, with file storage delegated to Cloudflare R2 and notifications routed through AWS SES. The **C4 Level 2** container view shows the React 19 SPA communicating with the Spring Boot 3 backend API, which in turn persists documents in PostgreSQL and stores files in R2.

#### How the Use Case Diagram and Domain Model map to the FDUC

- **Use Case Diagram actors** map directly to FDUC actors: Broker (primary), Client (secondary), and external systems (Auth0, R2, SES) as supporting actors.
- **Use Case sub-cases** (Request Document, Upload for Client, Submit Document, Review Document, Download Document, Request Signature, Send Reminder, Manage Stage Checklist) map one-to-one to the FDUC flow of activities and alternate flows.
- **DDD Domain Model** entities drive the FDUC's data needs:
  - `Document` aggregate root with `status`, `flow`, `requiresSignature`, `stage`, `templateKey` fields directly appear in FDUC steps (e.g., step 2 creates Document with these fields).
  - `DocumentVersion` entity with `UploadedBy` value object maps to FDUC steps 3, 6, A1 where files are uploaded and version history tracked.
  - `TransactionStageChecklistState` entity maps to alternate flow C1-C2 (checklist management).
  - `DocumentStatusEnum` (DRAFT, REQUESTED, SUBMITTED, APPROVED, NEEDS_REVISION, REJECTED) defines the valid transitions expressed in the FDUC.
  - `DocumentFlowEnum` (REQUEST, UPLOAD) determines which FDUC flow path is taken.

#### How each FDUC step predicts future design artifacts

- **Steps 1, 5** (navigate to documents) predict **SSD Scenarios 1, 3** (getDocumentsForTransaction) and **DLSD Flow 1** (DocumentController -> DocumentServiceImpl -> DocumentRepository).
- **Step 2** (create document) predicts **SSD Scenario 1** (createDocument) and **DLSD Flow 1** showing the full object interaction chain from React component through axiosInstance, DocumentController, DocumentServiceImpl, to DocumentRepository.
- **Steps 3-4** (upload file + send request) predict **SSD Scenario 1** (uploadFileToDocument, sendDocumentRequest) and **DLSD Flow 1** showing the two-phase DRAFT -> REQUESTED transition with EmailService and NotificationService involvement.
- **Step 6** (client submits) predicts **SSD Scenario 3** and **DLSD Flow 2**, showing ObjectStorageService.uploadFile, DocumentVersion creation, and status update to SUBMITTED.
- **Step 7** (download for review) predicts **SSD Scenario 4** and **DLSD Flow 3** (getDocumentDownloadUrl -> ObjectStorageService.getPreSignedUrl).
- **Step 8** (review decision) predicts **SSD Scenario 4** and **DLSD Flow 3**, showing reviewDocument with branching for APPROVED/NEEDS_REVISION/REJECTED and the auto-check side effect on ChecklistStateRepository.
- **Alternate flows A1-A3** (UPLOAD flow) predict **SSD Scenario 2** (createDocument, uploadFileToDocument, shareDocumentWithClient).
- **Alternate flows C1-C2** (checklist) predict **SSD Scenario 8** (getStageChecklist, setChecklistManualState).
- **Alternate flows O1-O2** (outstanding + reminder) predict **SSD Scenario 6** (getOutstandingDocuments, sendDocumentReminder).
- **State transitions** in the FDUC (DRAFT->REQUESTED->SUBMITTED->APPROVED/NEEDS_REVISION/REJECTED) are formalized in the **State Transition Diagram**.
- **DLCD** classes directly realize the FDUC actors and data: DocumentController handles all broker/client HTTP interactions, DocumentServiceImpl implements all business logic described in system responses, DocumentRepository/ObjectStorageService handle persistence and file storage.

---

## Section 2: Conceptual & Domain Level Modeling

### 2.1 Alignment Explanation

#### How domain objects in the DDD model support the information needs of the use case

The **DDD Domain Model** defines the Document Management bounded context with entities and value objects that directly support every step of the "Manage Transaction Documents" use case:

| DDD Domain Object | Use Case Information Need |
|---|---|
| **Document** (Aggregate Root) | Central entity for every FDUC step. Fields `documentId`, `docType`, `customTitle`, `status`, `flow`, `requiresSignature`, `stage`, `templateKey`, `brokerNotes`, `visibleToClient`, `dueDate` store all data captured in FDUC steps 2, A1 and used in steps 4-8, O1. |
| **DocumentVersion** (Entity) | Supports version history for FDUC steps 3, 6, A1, R2. Each file upload creates a new version, preserving the audit trail. The `uploadedBy: UploadedBy` value object distinguishes broker vs. client uploads. |
| **UploadedBy** (Value Object) | Embedded in DocumentVersion, tracks `uploaderType` (CLIENT/BROKER/SYSTEM/EXTERNAL), `uploaderId`, and `party`. Directly supports FDUC's distinction between broker-uploaded reference files (step 3) and client-submitted documents (step 6). |
| **StorageObject** (Value Object) | Holds `s3Key`, `fileName`, `mimeType`, `sizeBytes` for Cloudflare R2 integration. Supports FDUC steps involving file upload/download (3, 6, 7, A1, A3). |
| **TransactionRef** (Value Object) | Embedded in Document, holds `transactionId`, `clientId`, `side`. Links every document to its parent transaction, supporting FDUC precondition #2 and enabling per-transaction document queries. |
| **TransactionStageChecklistState** (Entity) | Tracks `manualChecked`, `autoChecked`, `itemKey`, `stage` per transaction. Directly supports FDUC alternate flow C1-C2 (checklist management). |
| **DocumentStatusEnum** | DRAFT, REQUESTED, SUBMITTED, APPROVED, NEEDS_REVISION, REJECTED - defines valid states referenced throughout the FDUC and formalized in the STD. |
| **DocumentFlowEnum** | REQUEST vs. UPLOAD - determines which FDUC main flow vs. alternate flow A is executed. |
| **StageEnum** | Maps buyer/seller transaction stages to document requirements, supporting stage-based filtering and checklist templates. |
| **DocumentPartyEnum** | BUYER, SELLER, BROKER, LENDER, NOTARY, INSPECTOR, CLIENT, OTHER - categorizes the expected document source in FDUC step 2. |

Cross-context relationships in the DDD model:
- **Document -> Transaction**: Via TransactionRef, establishing the parent relationship (FDUC precondition: active transaction must exist).
- **Document -> UserAccount**: Broker and client references resolve through TransactionRef.clientId and the transaction's brokerRef, supporting notification routing in FDUC steps 4, 6.1, 8.1.
- **Notification -> Document**: Notifications reference documents for in-app display (FDUC steps 4.1, 6.1, 8.1).

#### How C4 L3 models correspond to the DDD model, SSD, and UI/UX

**C4 L3 Backend -> DDD Model:**

| C4 L3 Backend Component | DDD Model Correspondence |
|---|---|
| DocumentController, GlobalDocumentController | REST endpoints that accept/return DTOs derived from Document aggregate attributes |
| DocumentService / DocumentServiceImpl | Application service enforcing Document aggregate invariants (status transitions, signature validation, visibility filtering) |
| StageDocumentTemplateRegistry | Realizes the StageDocumentTemplate value object concept - maps StageEnum to predefined document templates |
| EmailService | Supports the notification needs implied by Document state changes in the DDD model |
| DocumentRepository | Persists the Document aggregate root (with embedded TransactionRef, cascade to DocumentVersion) |
| ChecklistStateRepository | Persists TransactionStageChecklistState entity |
| ObjectStorageService | Infrastructure adapter for StorageObject value object - bridges DDD's storage abstraction to Cloudflare R2 |

**C4 L3 Database -> DDD Model:**

| Database Table | DDD Entity/VO |
|---|---|
| `documents` | Document aggregate root (documentId, status, flow, requiresSignature, stage, templateKey, dueDate, etc.) |
| `document_versions` | DocumentVersion entity (versionId, uploadedAt, uploader_type, party, s3key, file_name, etc.) |
| `transaction_stage_checklist_state` | TransactionStageChecklistState entity (transaction_id, stage, item_key, manual_checked, auto_checked) |
| `document_conditions` | DocumentConditionLink join entity linking conditions to documents |

**C4 L3 Frontend -> SSD/UI:**

| C4 L3 Frontend Component | SSD Scenario / UI Function |
|---|---|
| DocumentsCenter | Container component rendering document list, modals, and checklist - entry point for all SSD scenarios |
| DocumentCard | Displays individual document with status badge, version history - supports SSD scenarios 3, 4, 5 |
| RequestDocumentModal | Form for creating REQUEST-flow documents - SSD Scenario 1 |
| UploadForClientModal | Form for creating UPLOAD-flow documents - SSD Scenario 2 |
| DocumentReviewModal | Broker review decision interface - SSD Scenario 4 |
| StageChecklistPanel | Stage-based checklist display and toggle - SSD Scenario 8 |
| OutstandingDocumentsDashboard | Overdue document listing - SSD Scenario 6 |
| StatusFilterBar | Filters documents by status - supports all list-view scenarios |

**Consistency across domain entities -> persistent storage -> components/services:**

The three-tier consistency is maintained through a strict mapping chain:
1. **DDD Model** defines the conceptual structure (Document with status, flow, versions).
2. **PostgreSQL tables** (C4 L3 Database) implement this structure with proper column types, indexes, and soft-delete support (deletedAt/deletedBy).
3. **Spring Data Repositories** (C4 L3 Backend) provide type-safe access with custom queries (e.g., `findOutstandingDocumentsForBroker`).
4. **Service layer** enforces DDD invariants: status transition rules, signature validation, visibility filtering.
5. **DTOs** (DocumentRequestDTO, DocumentResponseDTO) carry exactly the fields needed by the frontend.
6. **React components** (C4 L3 Frontend) consume DTOs via React Query hooks, displaying data from the DDD model.

---

## Section 3: UI/UX Design

### 3.1 UI/UX Design Titles

The following Figma designs are needed to cover the full "Manage Transaction Documents" use case:

1. **Transaction Documents Page (Broker View)** - Main documents listing within a transaction, showing all documents grouped/filtered by stage or status, with action buttons for "Request Document" and "Upload for Client". Each document card shows type, status badge, due date, and version count.

2. **Transaction Documents Page (Client View)** - Client-facing document listing showing only visible documents (visibleToClient=true, DRAFT excluded). Shows pending requests with upload buttons, shared documents with download buttons, and review status feedback.

3. **Request Document Modal** - Modal form for broker to create a REQUEST-flow document. Fields: document type dropdown (DocumentTypeEnum), custom title (optional), expected from party, transaction stage, broker notes, due date, requires signature toggle. Actions: "Save as Draft" and "Send Request."

4. **Upload for Client Modal** - Modal form for broker to create an UPLOAD-flow document. Fields: document type, transaction stage, broker notes, file upload drop zone. Actions: "Save as Draft" and "Share with Client."

5. **Document Review Modal** - Modal for broker to review a submitted document. Shows: document name, submitted file preview/download link, version history. Decision radio buttons: Approve, Request Revision, Reject. Comments text area. Action: "Submit Review."

6. **Document Detail / Version History Panel** - Expandable panel or slide-over showing full document details: all versions with timestamps, uploader info, download links; broker notes; status history; linked conditions.

7. **Stage Checklist Panel** - Sidebar or accordion panel showing stage-based document checklist. Each item shows: label, checked state (with AUTO or MANUAL indicator), linked document status. Broker can toggle manual override checkboxes.

8. **Outstanding Documents Dashboard** - Dedicated page/section showing all overdue documents across all broker's transactions. Table columns: document title, transaction address, client name, due date, days outstanding, status. Actions: "Send Reminder" button per row.

9. **Signature Request Variant (within Request Document Modal)** - When "Requires Signature" toggle is on, shows additional file upload zone for the source document and explanatory text indicating client must download, sign, and re-upload.

10. **Document Status Badge Components** - Design system component showing all status states: DRAFT (gray), REQUESTED (yellow), SUBMITTED (blue), APPROVED (green), NEEDS_REVISION (orange), REJECTED (red).

### 3.2 Alignment Explanation

#### How each UI field maps to domain model attributes and validation rules

| UI Screen | UI Fields | DDD Model Attribute | Validation Rule |
|---|---|---|---|
| Request Document Modal | Document Type dropdown | `Document.docType: DocumentTypeEnum` | Required. 26 enum values from DocumentTypeEnum. |
| Request Document Modal | Custom Title | `Document.customTitle: String` | Optional. Used when docType=OTHER or for custom naming. |
| Request Document Modal | Expected From | `Document.expectedFrom: DocumentPartyEnum` | Required. Dropdown of BUYER, SELLER, BROKER, LENDER, etc. |
| Request Document Modal | Transaction Stage | `Document.stage: StageEnum` | Required. Filtered by transaction side (buy/sell stages). |
| Request Document Modal | Broker Notes | `Document.brokerNotes: String` | Optional. Instructions for the client. |
| Request Document Modal | Due Date | `Document.dueDate: LocalDateTime` | Optional. Must be in the future. |
| Request Document Modal | Requires Signature toggle | `Document.requiresSignature: boolean` | Boolean. If true, source document upload becomes required before sending. |
| Upload for Client Modal | File upload | `StorageObject.{s3Key, fileName, mimeType, sizeBytes}` | Required. File type and size validated by validateFile(). |
| Document Review Modal | Decision radio | `Document.status: DocumentStatusEnum` | Required. Must be one of APPROVED, NEEDS_REVISION, REJECTED. |
| Document Review Modal | Comments | `Document.brokerNotes: String` (via DocumentReviewRequestDTO.comments) | Optional but recommended for NEEDS_REVISION. |
| Stage Checklist Panel | Manual check toggle | `TransactionStageChecklistState.manualChecked: Boolean` | Boolean toggle. Overrides auto-check. |
| Outstanding Dashboard | Days Outstanding | `OutstandingDocumentDTO.daysOutstanding` | Computed: current date minus dueDate. Read-only. |

#### How UI navigation reflects the scenario steps

The UI navigation flow mirrors the FDUC activity flow:

1. **Broker Dashboard** -> "Pending Document Reviews" or "Outstanding Documents" widget (FDUC steps O1) -> navigates to documents
2. **Transaction Details Page** -> **Documents Tab** (FDUC step 1) -> shows document list
3. **"Request Document" button** -> opens **Request Document Modal** (FDUC step 2)
4. **Document Card "Send Request"** -> triggers DRAFT->REQUESTED transition (FDUC step 4)
5. **Client's Transaction Page** -> **Documents section** shows pending requests (FDUC step 5)
6. **"Upload" button on document card** -> file upload (FDUC step 6)
7. **Broker's Document Card "Review"** -> opens **Document Review Modal** (FDUC steps 7-8)
8. **"Stage Checklist" sidebar** -> shows checklist panel (FDUC flow C1-C2)
9. **Outstanding Documents page** -> shows overdue list with reminder buttons (FDUC flow O1-O2)

#### How data captured in Figma matches the C4 L3 data tier model

| Figma UI Element | C4 L3 Data Tier Column | Data Type | Validation |
|---|---|---|---|
| Document Type dropdown | `documents.doc_type` | VARCHAR (enum) | Must be valid DocumentTypeEnum value |
| Custom Title text input | `documents.custom_title` | VARCHAR(255) | Optional, max length enforced |
| Status badge | `documents.status` | VARCHAR (enum) | System-controlled, not user-editable |
| Due Date picker | `documents.due_date` | TIMESTAMP | Nullable, must be future date |
| Requires Signature toggle | `documents.requires_signature` | BOOLEAN | Default false |
| Flow indicator | `documents.flow` | VARCHAR (enum) | REQUEST or UPLOAD, set at creation |
| Stage selector | `documents.stage` | VARCHAR (enum) | Must match transaction side stages |
| File upload | `document_versions.s3key, file_name, mime_type, size_bytes` | VARCHAR, VARCHAR, VARCHAR, BIGINT | File type whitelist, max size limit |
| Uploader info | `document_versions.uploader_type, party, uploader_id` | VARCHAR (enum), VARCHAR (enum), UUID | System-set based on authenticated user |
| Checklist toggle | `transaction_stage_checklist_state.manual_checked` | BOOLEAN | Nullable (null = no override) |
| Broker Notes | `documents.broker_notes` | TEXT | Optional, no length limit |
| Version timestamp | `document_versions.uploaded_at` | TIMESTAMP | System-generated, not user-editable |

---

## Section 4: Interaction, Behavioral, and Logical Design

### 4.1 System Sequence Diagram (SSD)

[Paste System Sequence Diagram]

### 4.2 Design Level Sequence Diagram (DLSD)

**Combined (all 3 flows):**

[Paste Design Level Sequence Diagram]

**Flow 1 - Broker Creates Document Request (DRAFT -> Send):**

[Paste Design Level Sequence Diagram - Create Request]

**Flow 2 - Client Submits Document:**

[Paste Design Level Sequence Diagram - Client Submit]

**Flow 3 - Broker Reviews Document:**

[Paste Design Level Sequence Diagram - Broker Review]

### 4.3 Design Level Class Diagram (DLCD)

[Paste Design Level Class Diagram]

#### DLCD Chunked Views

**Presentation Layer:**

[Paste Presentation Layer Diagram]

**Business Layer:**

[Paste Business Layer Diagram]

**Data Layer:**

[Paste Data Layer Diagram]

**Infrastructure Layer:**

[Paste Infrastructure Layer Diagram]

**Enums:**

[Paste Enums Diagram]

**Relationships Overview:**

[Paste Relationships Diagram]

### 4.4 State Transition Diagram (STD)

[Paste State Transition Diagram]

### 4.5 Alignment Explanation

#### How SSD messages correspond to FDUC steps, alternate scenarios, inclusions, extensions, and exceptions

| SSD Scenario | SSD Messages | FDUC Mapping |
|---|---|---|
| **Scenario 1: Broker Creates Document Request** | `getDocumentsForTransaction`, `createDocument`, `uploadFileToDocument` (opt), `sendDocumentRequest` | Main flow steps 1-4. Includes: Authenticate User, Validate Transaction Access. Extension: Signature Request (opt upload before send). |
| **Scenario 2: Broker Uploads for Client** | `createDocument`, `uploadFileToDocument`, `shareDocumentWithClient` | Alternate flow A1-A2. Includes: Authenticate User, Validate Transaction Access. Extension: Stage Checklist Auto-Check. |
| **Scenario 3: Client Submits Document** | `getDocumentsForTransaction`, `submitDocument` | Main flow steps 5-6. Includes: Authenticate User (client), Upload File to R2. Exception E1: file validation fails. |
| **Scenario 4: Broker Reviews Document** | `getDocumentsForTransaction`, `getDocumentDownloadUrl`, `reviewDocument` | Main flow steps 7-8. Includes: Validate Transaction Access. Exception E4: invalid status transition (document not SUBMITTED). |
| **Scenario 5: Client Downloads Document** | `getDocumentDownloadUrl` | Main flow step 7 (client variant) and alternate flow A3. Exception E3: permission denied (not authorized). |
| **Scenario 6: Broker Sends Reminder** | `getOutstandingDocuments`, `sendDocumentReminder` | Alternate flow O1-O2 (Outstanding Documents & Reminders). |
| **Scenario 7: Broker Deletes Document** | `deleteDocument` | Implicit exception/cleanup flow. Exception E6: document not found. |
| **Scenario 8: Stage Checklist** | `getStageChecklist`, `setChecklistManualState` (opt) | Alternate flow C1-C2. Extension: Stage Checklist Auto-Check (reflected in auto-checked items). |

#### How the DLSD realizes SSD messages with actual object interactions across all three tiers

The DLSD expands each SSD system-level message into concrete object interactions spanning Frontend, Presentation, Business, and Data/Infrastructure layers:

**SSD `createDocument` -> DLSD Flow 1:**
- **Frontend tier**: `DocumentsCenter` (React) -> `axiosInstance` (Axios wrapper) -> POST /transactions/{txId}/documents
- **Presentation tier**: `DocumentController.createDocument(txId, dto, userId)`
- **Business tier**: `DocumentServiceImpl.createDocument()` -> validates broker permission via `TransactionAccessUtils`, calls `TransactionRepository.findByTransactionId()`, saves via `DocumentRepository.save()`
- **Data tier**: `DocumentRepository` -> PostgreSQL INSERT

**SSD `sendDocumentRequest` -> DLSD Flow 1 (continued):**
- **Presentation tier**: `DocumentController.sendDocumentRequest(txId, docId, brokerId)`
- **Business tier**: `DocumentServiceImpl.sendDocumentRequest()` -> loads Document, validates DRAFT status, validates signature source doc if needed, updates to REQUESTED, calls `UserAccountRepository.findById(clientId)` to resolve client email/language, calls `EmailService.sendDocumentRequestedNotification()`, calls `NotificationService.createDocumentNotification()`
- **Data tier**: `DocumentRepository.save()` -> PostgreSQL UPDATE; `NotificationRepository` -> PostgreSQL INSERT

**SSD `submitDocument` -> DLSD Flow 2:**
- **Frontend tier**: `DocumentsCenter` -> `axiosInstance` -> POST multipart/form-data
- **Presentation tier**: `DocumentController.submitDocument(txId, docId, file, userId, CLIENT)`
- **Business tier**: `DocumentServiceImpl.submitDocument()` -> validates file, validates status in [REQUESTED, NEEDS_REVISION], calls `ObjectStorageService.uploadFile()` -> Cloudflare R2 PutObject, creates DocumentVersion, updates status to SUBMITTED, calls EmailService + NotificationService
- **Data tier**: R2 storage, PostgreSQL UPDATE + INSERT version

**SSD `reviewDocument` -> DLSD Flow 3:**
- **Frontend tier**: `DocumentsCenter` -> `axiosInstance` -> PATCH
- **Business tier**: `DocumentServiceImpl.reviewDocument()` -> validates SUBMITTED status, sets decision status, if APPROVED calls `ChecklistStateRepository` to UPSERT auto-check, calls EmailService + NotificationService
- **Data tier**: PostgreSQL UPDATE document, UPSERT checklist state, INSERT notification

#### How the classes and components used in the DLSD align with those defined in the C4 L3 diagrams

| DLSD Participant | C4 L3 Component | Layer |
|---|---|---|
| DocumentsCenter (React Component) | DocumentsCenter in C4 L3 Frontend | Frontend |
| axiosInstance (API Client) | ApiClient in C4 L3 Frontend | Frontend |
| DocumentController | DocumentController in C4 L3 Backend | Presentation |
| GlobalDocumentController | GlobalDocumentController in C4 L3 Backend | Presentation |
| DocumentServiceImpl | DocumentService in C4 L3 Backend | Business |
| StageTemplateRegistry | (part of DocumentService) in C4 L3 Backend | Business |
| NotificationService | NotificationService in C4 L3 Backend | Business |
| EmailService | (part of NotificationService / JavaMailSender) in C4 L3 Backend | Business/Infrastructure |
| DocumentRepository | DocumentRepository in C4 L3 Backend | Data |
| TransactionRepository | TransactionRepository in C4 L3 Backend | Data |
| UserAccountRepository | UserAccountRepository in C4 L3 Backend | Data |
| ChecklistStateRepository | (part of DocumentRepository) in C4 L3 Backend | Data |
| ObjectStorageService | ObjectStorageService in C4 L3 Backend | Infrastructure |
| PostgreSQL | courtierpro-db in C4 L3 Database | Data Tier |
| Cloudflare R2 | Cloudflare R2 (external system) in C4 L1/L2 | Infrastructure |

#### How the DLCD reflects the classes and methods implied by the DLSD

Every participant and method call in the DLSD appears as a class with corresponding method signature in the DLCD:

| DLSD Interaction | DLCD Class.Method |
|---|---|
| `DocCtrl -> DocSvc: createDocument(txId, dto, userId)` | `DocumentController.createDocument()` -> `DocumentService.createDocument()` |
| `DocSvc -> TxRepo: findByTransactionId(txId)` | `DocumentServiceImpl` dependency on `TransactionRepository.findByTransactionId()` |
| `DocSvc -> DocRepo: save(document)` | `DocumentServiceImpl` dependency on `DocumentRepository` (persists Document entity) |
| `DocSvc -> Storage: uploadFile(txId, docId, file)` | `DocumentServiceImpl` dependency on `ObjectStorageService.uploadFile()` |
| `DocSvc -> EmailSvc: sendDocumentRequestedNotification(...)` | `DocumentServiceImpl` dependency on `EmailService.sendDocumentRequestedNotification()` |
| `DocSvc -> NotifSvc: create notification` | `DocumentServiceImpl` dependency on `NotificationService.createDocumentNotification()` |
| `DocSvc -> CheckRepo: auto-check checklist item` | `DocumentServiceImpl` dependency on `ChecklistStateRepository.findByTransactionIdAndStageAndItemKey()` |
| `DocSvc -> UserRepo: findById(clientId)` | `DocumentServiceImpl` dependency on `UserAccountRepository.findById()` |
| `Storage -> R2: PutObject` | `ObjectStorageServiceImpl` (implements `ObjectStorageService`) uses S3Client to connect to Cloudflare R2 |

Additional DLCD elements not shown in DLSD but implied:
- **StageDocumentTemplateRegistry**: Used by DocumentServiceImpl for checklist building (SSD Scenario 8)
- **TransactionStageChecklistState**: Entity persisted by ChecklistStateRepository for manual overrides
- **Validation methods**: `validateFile()`, `validateCanSubmit()`, `validateCanReview()` are private methods in DocumentServiceImpl called within DLSD flows
- **filterForClientVisibility()**: Private method in DocumentServiceImpl that ensures clients don't see DRAFT documents

#### How class responsibilities and method signatures support domain invariants from the DDD model

| DDD Invariant | DLCD Class Responsibility |
|---|---|
| Document status transitions follow a strict lifecycle (DRAFT->REQUESTED->SUBMITTED->APPROVED/NEEDS_REVISION/REJECTED) | `DocumentServiceImpl.validateCanSubmit()` ensures document is REQUESTED or NEEDS_REVISION before submission. `validateCanReview()` ensures document is SUBMITTED before review. `sendDocumentRequest()` validates status is DRAFT. |
| Each Document belongs to exactly one Transaction | `Document.transactionRef: TransactionRef` is a required embedded value object. `DocumentServiceImpl.createDocument()` validates transaction exists via `TransactionRepository.findByTransactionId()`. |
| Signature requests require a source document | `DocumentServiceImpl.sendDocumentRequest()` validates `versions.size() > 0` when `requiresSignature=true`. |
| Only visible documents shown to Client | `DocumentServiceImpl.filterForClientVisibility()` removes DRAFT documents and documents with `visibleToClient=false` for client queries. |
| Files must be valid (type, size) | `DocumentServiceImpl.validateFile()` enforces MIME type whitelist and size limits before ObjectStorageService upload. |
| Soft delete preserves audit trail | `Document.deletedAt/deletedBy` and `DocumentVersion.deletedAt/deletedBy` fields. `DocumentServiceImpl.deleteDocument()` sets these instead of physical deletion. |
| Stage checklist auto-completion | `DocumentServiceImpl` auto-checks checklist items when document reaches APPROVED (REQUEST) or SUBMITTED (UPLOAD) via `ChecklistStateRepository`. |

#### How state constraints influence validation and logic in the DLSD and code

The **State Transition Diagram** defines 6 states and their valid transitions. These constraints directly shape the DLSD and implementation logic:

| State Constraint (STD) | DLSD / Code Impact |
|---|---|
| **DRAFT -> REQUESTED**: Only via `sendDocumentRequest` | DLSD Flow 1: `DocumentServiceImpl.sendDocumentRequest()` checks `document.status == DRAFT` before updating. If not DRAFT, throws exception (FDUC E4). |
| **DRAFT -> SUBMITTED**: Only via `shareDocumentWithClient` (UPLOAD flow) | DLSD (implicit): `shareDocumentWithClient()` checks `flow == UPLOAD && status == DRAFT`. Sets `visibleToClient=true` on transition. |
| **DRAFT -> DRAFT**: `uploadFileToDocument` (self-loop) | DLSD Flow 1 (opt block): Broker uploads reference file, status stays DRAFT. New DocumentVersion created without status change. |
| **REQUESTED -> SUBMITTED**: Only via `submitDocument` with valid file | DLSD Flow 2: `validateCanSubmit()` checks status in [REQUESTED, NEEDS_REVISION]. `validateFile()` checks file constraints. Both must pass before transition. |
| **SUBMITTED -> APPROVED/NEEDS_REVISION/REJECTED**: Only via `reviewDocument` | DLSD Flow 3: `validateCanReview()` checks `status == SUBMITTED`. Decision must be one of the three valid values. APPROVED triggers auto-check side effect. |
| **NEEDS_REVISION -> SUBMITTED**: Client resubmit cycle | DLSD Flow 2 reuse: Same submitDocument flow applies. `validateCanSubmit()` accepts NEEDS_REVISION status. Creates new DocumentVersion. |
| **APPROVED and REJECTED are terminal** | No transitions defined from these states in STD. Any attempt to modify returns error (FDUC E4). |

---

## Section 5: Implementation

### 5.1 Code Snippets

#### Backend: Document Entity (Data Layer)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/datalayer/Document.java

@Entity
@Table(name = "documents")
@SQLRestriction("deleted_at IS NULL")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID documentId;

    @Embedded
    private TransactionRef transactionRef;

    @Enumerated(EnumType.STRING)
    private DocumentTypeEnum docType;

    private String customTitle;

    @Enumerated(EnumType.STRING)
    private DocumentStatusEnum status;

    @Enumerated(EnumType.STRING)
    private DocumentPartyEnum expectedFrom;

    @Enumerated(EnumType.STRING)
    private DocumentFlowEnum flow;

    private boolean requiresSignature;

    @Enumerated(EnumType.STRING)
    private StageEnum stage;

    private String templateKey;
    private boolean autoGenerated;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("deleted_at IS NULL")
    private List<DocumentVersion> versions = new ArrayList<>();

    private String brokerNotes;
    private boolean visibleToClient;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
```

#### Backend: DocumentService Interface (Business Layer)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentService.java

public interface DocumentService {
    List<DocumentResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId);
    DocumentResponseDTO getDocument(UUID documentId, UUID userId);
    DocumentResponseDTO createDocument(UUID transactionId, DocumentRequestDTO dto, UUID userId);
    DocumentResponseDTO updateDocument(UUID documentId, DocumentRequestDTO dto, UUID userId);
    void deleteDocument(UUID documentId, UUID userId);
    DocumentResponseDTO submitDocument(UUID txId, UUID docId, MultipartFile file, UUID userId, UploadedByRefEnum type);
    DocumentResponseDTO uploadFileToDocument(UUID txId, UUID docId, MultipartFile file, UUID userId, UploadedByRefEnum type);
    List<DocumentResponseDTO> getAllDocumentsForUser(UUID userId);
    String getDocumentDownloadUrl(UUID documentId, UUID versionId, UUID userId);
    DocumentResponseDTO reviewDocument(UUID txId, UUID docId, DocumentReviewRequestDTO dto, UUID brokerId);
    DocumentResponseDTO sendDocumentRequest(UUID documentId, UUID brokerId);
    DocumentResponseDTO shareDocumentWithClient(UUID documentId, UUID brokerId);
    List<OutstandingDocumentDTO> getOutstandingDocumentSummary(UUID brokerId);
    void sendDocumentReminder(UUID documentId, UUID brokerId);
    StageChecklistResponseDTO getStageChecklist(UUID txId, String stage, UUID userId);
    StageChecklistResponseDTO setChecklistManualState(UUID txId, String stage, String itemKey, boolean checked, UUID brokerId);
}
```

#### Backend: DocumentController (Presentation Layer)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java

@RestController
@RequestMapping("/transactions/{transactionId}/documents")
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getDocuments(
            @PathVariable UUID transactionId,
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(documentService.getDocumentsForTransaction(transactionId, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> createDocument(
            @PathVariable UUID transactionId,
            @RequestBody DocumentRequestDTO dto,
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(transactionId, dto, userId));
    }

    @PostMapping("/{documentId}/submit")
    public ResponseEntity<DocumentResponseDTO> submitDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(documentService.submitDocument(
                transactionId, documentId, file, userId, /* resolved from role */));
    }

    @PatchMapping("/{documentId}/review")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> reviewDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestBody DocumentReviewRequestDTO dto,
            @RequestAttribute("userId") UUID brokerId) {
        return ResponseEntity.ok(documentService.reviewDocument(transactionId, documentId, dto, brokerId));
    }

    @PostMapping("/{documentId}/send")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> sendDocumentRequest(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestAttribute("userId") UUID brokerId) {
        return ResponseEntity.ok(documentService.sendDocumentRequest(documentId, brokerId));
    }

    @PostMapping("/{documentId}/share")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> shareDocumentWithClient(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestAttribute("userId") UUID brokerId) {
        return ResponseEntity.ok(documentService.shareDocumentWithClient(documentId, brokerId));
    }

    @GetMapping("/checklist")
    public ResponseEntity<StageChecklistResponseDTO> getStageChecklist(
            @PathVariable UUID transactionId,
            @RequestParam String stage,
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(documentService.getStageChecklist(transactionId, stage, userId));
    }
}
```

#### Frontend: Document Types (TypeScript)

```typescript
// frontend/src/features/documents/types.ts

export type DocumentStatusEnum = 'DRAFT' | 'REQUESTED' | 'SUBMITTED' | 'APPROVED' | 'NEEDS_REVISION' | 'REJECTED';
export type DocumentFlowEnum = 'REQUEST' | 'UPLOAD';
export type DocumentPartyEnum = 'BUYER' | 'SELLER' | 'BROKER' | 'LENDER' | 'NOTARY' | 'INSPECTOR' | 'CLIENT' | 'OTHER';

export interface Document {
  documentId: string;
  transactionRef: { transactionId: string; clientId: string; side: string };
  docType: DocumentTypeEnum;
  customTitle?: string;
  status: DocumentStatusEnum;
  expectedFrom: DocumentPartyEnum;
  versions: DocumentVersion[];
  brokerNotes?: string;
  lastUpdatedAt?: string;
  visibleToClient: boolean;
  stage: string;
  dueDate?: string;
  flow: DocumentFlowEnum;
  requiresSignature?: boolean;
}

export interface DocumentVersion {
  versionId: string;
  uploadedAt: string;
  uploadedBy: { uploaderType: string; party: DocumentPartyEnum; uploaderId: string; externalName?: string };
  storageObject: { s3Key: string; fileName: string; mimeType: string; sizeBytes: number };
}

export interface StageChecklistItemDTO {
  itemKey: string;
  label: string;
  docType: DocumentTypeEnum;
  flow: DocumentFlowEnum;
  requiresSignature: boolean;
  checked: boolean;
  source: 'AUTO' | 'MANUAL';
  documentId?: string | null;
  documentStatus?: string | null;
}
```

#### Frontend: React Query Mutations

```typescript
// frontend/src/features/documents/api/mutations.ts

export const useCreateDocument = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ transactionId, data }: { transactionId: string; data: DocumentCreateDTO }) =>
      createDocument(transactionId, data),
    onSuccess: (_, { transactionId }) => {
      queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.stat(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
    },
  });
};

export const useSubmitDocument = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ transactionId, documentId, file }: { transactionId: string; documentId: string; file: File }) =>
      submitDocument(transactionId, documentId, file),
    onSuccess: (_, { transactionId }) => {
      queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.stat(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
    },
  });
};

export const useReviewDocument = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ transactionId, documentId, decision, comments }:
      { transactionId: string; documentId: string; decision: string; comments?: string }) =>
      reviewDocument(transactionId, documentId, decision, comments),
    onSuccess: (_, { transactionId }) => {
      queryClient.invalidateQueries({ queryKey: documentKeys.list(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.stat(transactionId) });
      queryClient.invalidateQueries({ queryKey: documentKeys.checklists() });
      queryClient.invalidateQueries({ queryKey: documentKeys.outstanding() });
    },
  });
};
```

#### Frontend: Documents API Layer

```typescript
// frontend/src/features/documents/api/documentsApi.ts

export const fetchDocuments = async (transactionId: string): Promise<Document[]> => {
  const response = await axiosInstance.get(`/transactions/${transactionId}/documents`);
  return response.data;
};

export const createDocument = async (transactionId: string, data: DocumentCreateDTO): Promise<Document> => {
  const response = await axiosInstance.post(`/transactions/${transactionId}/documents`, data);
  return response.data;
};

export const submitDocument = async (transactionId: string, documentId: string, file: File): Promise<Document> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await axiosInstance.post(
    `/transactions/${transactionId}/documents/${documentId}/submit`, formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return response.data;
};

export const getDocumentDownloadUrl = async (
  transactionId: string, documentId: string, versionId: string
): Promise<string> => {
  const response = await axiosInstance.get(
    `/transactions/${transactionId}/documents/${documentId}/versions/${versionId}/download`
  );
  return response.data.url;
};

export const fetchStageChecklist = async (
  transactionId: string, stage: string
): Promise<StageChecklistResponseDTO> => {
  const response = await axiosInstance.get(
    `/transactions/${transactionId}/documents/checklist`, { params: { stage } }
  );
  return response.data;
};
```

#### Database: Schema (Flyway Migration)

```sql
-- backend/src/main/resources/migration/V1__init_schema.sql (relevant excerpt)

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE,
    transaction_id UUID NOT NULL,
    client_id UUID NOT NULL,
    side VARCHAR(20) NOT NULL,
    doc_type VARCHAR(60) NOT NULL,
    custom_title VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    expected_from VARCHAR(30),
    flow VARCHAR(20) NOT NULL DEFAULT 'REQUEST',
    requires_signature BOOLEAN NOT NULL DEFAULT FALSE,
    stage VARCHAR(60),
    template_key VARCHAR(120),
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE,
    broker_notes TEXT,
    visible_to_client BOOLEAN NOT NULL DEFAULT TRUE,
    due_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    version_id UUID NOT NULL UNIQUE,
    document_id BIGINT NOT NULL REFERENCES documents(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploader_type VARCHAR(20) NOT NULL,
    party VARCHAR(30),
    uploader_id UUID,
    external_name VARCHAR(255),
    s3key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE TABLE transaction_stage_checklist_state (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL,
    stage VARCHAR(60) NOT NULL,
    item_key VARCHAR(120) NOT NULL,
    manual_checked BOOLEAN,
    manual_checked_by UUID,
    manual_checked_at TIMESTAMP,
    auto_checked BOOLEAN NOT NULL DEFAULT FALSE,
    auto_checked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(transaction_id, stage, item_key)
);
```

### 5.2 Alignment Explanation

#### The code implements the UI/UX flows defined in Figma

Each Figma screen corresponds to a frontend component that calls the backend API:

| Figma Screen | Frontend Component | API Call |
|---|---|---|
| Transaction Documents Page (Broker) | `DocumentList` + `DocumentCard` | `GET /transactions/{id}/documents` via `useDocuments()` |
| Transaction Documents Page (Client) | Same components, filtered by `filterForClientVisibility` | Same API, server-side filtering |
| Request Document Modal | `RequestDocumentModal` | `POST /transactions/{id}/documents` via `useCreateDocument()` |
| Upload for Client Modal | `UploadForClientModal` | `POST /documents` + `POST /documents/{id}/upload` + `POST /documents/{id}/share` |
| Document Review Modal | `DocumentReviewModal` | `PATCH /documents/{id}/review` via `useReviewDocument()` |
| Stage Checklist Panel | `StageChecklistPanel` | `GET /documents/checklist?stage=` via `useStageChecklist()` |
| Outstanding Documents Dashboard | `OutstandingDocumentsDashboard` | `GET /documents/outstanding` via `useGetOutstandingDocuments()` |

#### The code flow and components correspond to the DLSD messages

Each DLSD message maps to actual code:

| DLSD Message | Code Implementation |
|---|---|
| `FE -> Axios: POST /transactions/{txId}/documents` | `documentsApi.createDocument()` calls `axiosInstance.post()` |
| `Axios -> DocCtrl: createDocument(txId, dto, userId)` | `DocumentController.createDocument()` receives `@RequestBody DocumentRequestDTO` |
| `DocCtrl -> DocSvc: createDocument(txId, dto, userId)` | Controller delegates to `DocumentService.createDocument()` |
| `DocSvc -> TxRepo: findByTransactionId(txId)` | `DocumentServiceImpl` calls `transactionRepository.findByTransactionId()` |
| `DocSvc -> DocRepo: save(document)` | `DocumentServiceImpl` calls `documentRepository.save()` |
| `DocSvc -> Storage: uploadFile(txId, docId, file)` | `DocumentServiceImpl` calls `objectStorageService.uploadFile()` |
| `DocSvc -> EmailSvc: sendDocumentRequestedNotification(...)` | `DocumentServiceImpl` calls `emailService.sendDocumentRequestedNotification()` |
| `DocSvc -> NotifSvc: create notification` | `DocumentServiceImpl` calls `notificationService.createDocumentNotification()` |
| `DocSvc -> CheckRepo: auto-check checklist item` | `DocumentServiceImpl` calls `checklistStateRepository.findByTransactionIdAndStageAndItemKey()` then saves |

#### Classes, interfaces, and method signatures match the DLCD

| DLCD Class | Implementation File | Verified Match |
|---|---|---|
| `DocumentController` | `documents/presentationlayer/DocumentController.java` | All 13 endpoints match DLCD methods |
| `GlobalDocumentController` | `documents/presentationlayer/GlobalDocumentController.java` | 3 endpoints match DLCD methods |
| `DocumentService` (interface) | `documents/businesslayer/DocumentService.java` | All 16 methods match DLCD interface |
| `DocumentServiceImpl` | `documents/businesslayer/DocumentServiceImpl.java` | 8 dependencies + all public/private methods match DLCD |
| `StageDocumentTemplateRegistry` | `documents/businesslayer/StageDocumentTemplateRegistry.java` | `getTemplatesForStage()` and `getAllTemplates()` match |
| `EmailService` | `email/businesslayer/EmailService.java` | 4 document notification methods match DLCD |
| `DocumentRepository` | `documents/datalayer/DocumentRepository.java` | 8 query methods match DLCD interface |
| `ChecklistStateRepository` | `documents/datalayer/TransactionStageChecklistStateRepository.java` | 2 query methods match DLCD |
| `Document` (entity) | `documents/datalayer/Document.java` | All 18 fields match DLCD entity |
| `DocumentVersion` (entity) | `documents/datalayer/DocumentVersion.java` | All fields match including embedded VOs |
| `ObjectStorageServiceImpl` | `infrastructure/ObjectStorageServiceImpl.java` | Implements ObjectStorageService with `uploadFile`, `getPreSignedUrl`, `deleteFile` |

#### Domain logic and entity validation match the DDD model and use case rules

| DDD Invariant / Use Case Rule | Code Implementation |
|---|---|
| Status transitions DRAFT->REQUESTED->SUBMITTED->APPROVED | `DocumentServiceImpl` methods enforce: `sendDocumentRequest` checks DRAFT, `submitDocument` checks [REQUESTED, NEEDS_REVISION], `reviewDocument` checks SUBMITTED |
| Signature requires source document | `sendDocumentRequest()`: `if (doc.isRequiresSignature() && doc.getVersions().isEmpty())` throws exception |
| Client visibility filtering | `filterForClientVisibility()` removes DRAFT docs and `visibleToClient=false` docs |
| File validation | `validateFile()` checks MIME type whitelist and max file size |
| Soft delete | `deleteDocument()` sets `deletedAt = LocalDateTime.now()`, `deletedBy = userId` instead of physical deletion. `@SQLRestriction("deleted_at IS NULL")` filters deleted records from queries. |
| Stage checklist auto-check | After APPROVED (REQUEST) or SUBMITTED (UPLOAD), `DocumentServiceImpl` upserts `TransactionStageChecklistState.autoChecked = true` |

#### Data types and constraints match C4 L3 data tier models

| C4 L3 Data Tier | SQL Type | Java Type | TypeScript Type |
|---|---|---|---|
| `documents.document_id` | `UUID NOT NULL UNIQUE` | `UUID documentId` | `string (documentId)` |
| `documents.status` | `VARCHAR(30) NOT NULL DEFAULT 'DRAFT'` | `DocumentStatusEnum status` | `DocumentStatusEnum` (string literal union) |
| `documents.flow` | `VARCHAR(20) NOT NULL DEFAULT 'REQUEST'` | `DocumentFlowEnum flow` | `DocumentFlowEnum` (string literal union) |
| `documents.requires_signature` | `BOOLEAN NOT NULL DEFAULT FALSE` | `boolean requiresSignature` | `boolean` |
| `documents.due_date` | `TIMESTAMP` (nullable) | `LocalDateTime dueDate` | `string \| undefined` |
| `document_versions.s3key` | `VARCHAR(500) NOT NULL` | `String s3Key` (in StorageObject) | `string (storageObject.s3Key)` |
| `document_versions.size_bytes` | `BIGINT` | `Long sizeBytes` (in StorageObject) | `number (storageObject.sizeBytes)` |
| `document_versions.uploader_type` | `VARCHAR(20) NOT NULL` | `UploadedByRefEnum uploaderType` | `string (uploadedBy.uploaderType)` |
| `checklist_state.manual_checked` | `BOOLEAN` (nullable) | `Boolean manualChecked` | `boolean (checked)` |
| `checklist_state.item_key` | `VARCHAR(120) NOT NULL` | `String itemKey` | `string (itemKey)` |
