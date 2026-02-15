# Manage Transaction Documents - Owned Use Case (V2)

## How to Read This

- This document keeps the exact deliverable order required by `Instructions.txt`.
- Start with **Section 1.0 Core Flow Snapshot** and **Section 1.2 Fully Developed Use Case**.
- Use the IDs in **Section 1.3 Canonical Traceability Matrix** (`FDUC-*`, `SSD-*`, `DLSD-*`, `STD-*`) to jump across sections.
- Section 4 gives interaction and behavioral detail; Section 5 gives short implementation evidence snippets.
- Optional DRAFT-first request handling exists, but is only mentioned briefly for readability.

## ID Naming Convention

Use this table as the quick legend for all IDs in this document and its diagrams. If a section feels dense, read this first, then come back to the matrixes.

| Prefix / Pattern | Meaning | Example | Where Used |
|---|---|---|---|
| `FDUC-*` | Fully Developed Use Case step IDs | `FDUC-1.4` | Section 1.2 flow table, traceability matrix |
| `FDUC-A*` | Alternate-flow step IDs | `FDUC-A1.2` | Section 1.2 alternate flow rows |
| `FDUC-R*` | Revision-cycle step IDs | `FDUC-R1.2` | Section 1.2 revision cycle rows |
| `FDUC-E*` | Use case exception IDs | `FDUC-E1` | Section 1.2 exceptions, traceability matrix |
| `FDUC-INC-*` | Inclusion IDs (always-included behavior) | `FDUC-INC-2` | Section 1.2 inclusions, Section 4.5 mappings |
| `FDUC-EXT-*` | Extension IDs (conditional behavior) | `FDUC-EXT-1` | Section 1.2 extensions, Section 4.5 mappings |
| `SSD-Sx-My` | System Sequence Diagram scenario/message IDs | `SSD-S4-M3` | `SystemSequenceDiagram.puml`, traceability mappings |
| `DLSD-Fx-My` | Design-Level Sequence flow/message IDs | `DLSD-F3-M3` | Combined and split DLSD files, traceability mappings |
| `DLSD-Fx-MyE*` | DLSD exception/alternate branch IDs | `DLSD-F4-M12E4` | F3/F4 `alt` branches in DLSD |
| `STD-T*` | State-transition IDs | `STD-T6` | STD diagram and Section 4.5 state mapping |
| `DLCD-C*` | DLCD presentation class group IDs | `DLCD-C1` | DLCD and Section 4.5 / 5.2 references |
| `DLCD-S*` | DLCD service/business group IDs | `DLCD-S1` | DLCD and Section 4.5 / 5.2 references |
| `DLCD-D*` | DLCD data/repository group IDs | `DLCD-D1` | DLCD and Section 5.2 references |
| `DLCD-I*` | DLCD infrastructure group IDs | `DLCD-I1` | DLCD and Section 5.2 references |
| `DLCD-E*` | DLCD entity/value-object group IDs | `DLCD-E1` | DLCD and Section 4.5 references |
| `DLCD-*.M*` | Method index under a DLCD group | `DLCD-S1.M4` | Traceability matrix and Section 4.5 |
| `DLCD-*.V*` | Service helper/access method index under service group | `DLCD-S2.V1` | DLCD details and explanatory mappings |
| `SNP-*` | Evidence snippet IDs for implementation examples | `SNP-S4` | Section 5.1 snippet headings and Section 5.2 evidence mapping |

## Introduction

Manage Transaction Documents is the broker-client workflow for requesting, submitting, reviewing, sharing, and downloading transaction files. This V2 keeps the core decision path front-and-center while preserving all required alignment coverage.

## Meaningfulness

This use case is central to CourtierPro because every transaction depends on documents being requested, submitted, reviewed, and shared in a clear way.  
When that process is clear, brokers and clients always know what is still needed, what has been completed, and what needs correction before moving forward.

It also strengthens the whole system experience, not just the documents page.  
Clear document handling reduces delays, prevents missed steps, and supports better decisions in related transaction work such as scheduling, offer progress, and overall case readiness.

In short, this use case is one of the system's stability points: when it works well, the rest of the transaction workflow stays reliable and easier to trust.

---

## System-Wide Artifacts

### Use Case Diagram

[Paste Use Case Diagram]
Reference source: `CourtierPro/docs/diagrams/UseCaseDiagram_MidLevel.puml`

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

### 1.0 Core Flow Snapshot

This table gives a fast mental model before the detailed flows and diagrams.

| Item | Core Summary |
|---|---|
| Primary actor | Broker |
| Secondary actor | Client |
| REQUEST flow | Broker requests document -> Client submits file -> Broker reviews decision |
| UPLOAD flow | Broker uploads file -> Broker shares with client -> Client downloads file |
| Status lifecycle | `DRAFT` -> `REQUESTED` -> `SUBMITTED` -> (`APPROVED` \| `NEEDS_REVISION` \| `REJECTED`) |
| Optional path | Broker may start a request in `DRAFT` before sending it |

Key takeaway: REQUEST and UPLOAD start differently, but both converge on the same review-ready state (`SUBMITTED`).

### 1.1 Activity Diagrams

**REQUEST Flow** (Broker requests, Client uploads):

[Paste Activity Diagram - Request Flow]

**UPLOAD Flow** (Broker uploads, Client downloads):

[Paste Activity Diagram - Upload Flow]

### 1.2 Fully Developed Use Case

This profile table defines the scope and guardrails for the use case before step-by-step behavior.

| Field | Description |
|---|---|
| **Use Case Name** | Manage Transaction Documents |
| **Scenario** | Broker and Client collaborate on transaction files through two paths: REQUEST and UPLOAD. |
| **Triggering Event** | Broker opens the documents area of an active transaction and starts a document action. |
| **Actors** | **Primary:** Broker; **Secondary:** Client; **System-level related actor in the use-case diagram:** Admin |
| **Related Use Cases** | Manage Transaction Lifecycle [Amir], Register User [Olivier], Authenticate User, Manage Appointments [Isaac], Monitor Business Performance |
| **Stakeholders** | Broker, Client, Brokerage Admin |
| **Preconditions** | 1. Authenticated user with valid role.<br>2. Active transaction exists.<br>3. User has required transaction document access. |
| **Postconditions** | 1. Document metadata and versions are persisted.<br>2. Status transitions are valid and auditable.<br>3. Relevant actor receives notification on key state change. |

#### Flow of Activities

This flow table is the canonical behavior sequence, with each actor action paired to a direct system response.

| Step ID | Actor Action | System Response |
|---|---|---|
| **Main Flow: REQUEST** | | |
| `FDUC-1.1` | Broker opens transaction documents. | System validates access and returns the current document list. |
| `FDUC-1.2` | Broker submits request form (`docType`, `expectedFrom`, `stage`, `notes`, `dueDate`, `requiresSignature`). | System validates payload, creates `Document` with `flow=REQUEST` and `status=REQUESTED`, then notifies Client. |
| `FDUC-1.3` | Client opens transaction documents. | System validates participation and returns client-visible documents. |
| `FDUC-1.4` | Client uploads requested file. | System validates file, stores object in R2, creates `DocumentVersion`, sets status to `SUBMITTED`, then notifies Broker. |
| `FDUC-1.5` | Broker downloads submitted version for review. | System returns a time-limited presigned download URL. |
| `FDUC-1.6` | Broker submits decision (`APPROVED`, `NEEDS_REVISION`, or `REJECTED`) with optional comments. | System validates current status is `SUBMITTED`, updates status, stores comments, then notifies Client. |
| **Alternate Flow: UPLOAD** | | |
| `FDUC-A1.1` | Broker uploads a document for the client. | System creates `Document` with `flow=UPLOAD` and `status=DRAFT`, then stores uploaded file as version 1. |
| `FDUC-A1.2` | Broker clicks Share with Client. | System transitions `DRAFT -> SUBMITTED`, sets `visibleToClient=true`, then notifies Client. |
| `FDUC-A1.3` | Client downloads shared file. | System returns a time-limited presigned download URL. |
| **Alternate Flow: Revision Cycle** | | |
| `FDUC-R1.1` | Client receives revision feedback after `NEEDS_REVISION`. | System keeps status as `NEEDS_REVISION` with reviewer comments visible in document details. |
| `FDUC-R1.2` | Client submits corrected file. | System stores a new `DocumentVersion`, transitions `NEEDS_REVISION -> SUBMITTED`, then notifies Broker. |

Key takeaway: the lifecycle is action-driven, and every transition is tied to a clear actor/system pair.

#### Inclusions

These always-on rules apply across both REQUEST and UPLOAD paths.

- `FDUC-INC-1 Authenticate User`: Every operation requires a valid authenticated identity.
- `FDUC-INC-2 Validate Transaction Access`: Access policy is checked before reading or mutating documents.
- `FDUC-INC-3 Store File in Object Storage`: Upload paths persist binary files in Cloudflare R2.
- `FDUC-INC-4 Notify Participant`: Key state changes emit notifications to the other actor.

#### Extensions

These rules apply only when specific conditions are met.

- `FDUC-EXT-1 Signature Request`: When `requiresSignature=true`, a source file must exist before sending the request.
- `FDUC-EXT-2 Optional DRAFT-First Request`: A broker may create request metadata in `DRAFT` and send later.

#### Exceptions

These are the expected failure outcomes that keep invalid operations out of the workflow.

- `FDUC-E1 File Validation Failed`: Unsupported MIME type or size exceeds limit -> request rejected.
- `FDUC-E2 Transaction Missing/Inactive`: Target transaction cannot be used -> operation rejected.
- `FDUC-E3 Permission Denied`: Caller lacks required document permission -> operation rejected.
- `FDUC-E4 Invalid State Transition`: Requested operation is incompatible with current status -> operation rejected.
- `FDUC-E5 Missing Signature Source File`: Signature request sent without source file -> operation rejected.
- `FDUC-E6 Document Not Found`: Unknown `documentId` -> operation rejected.

### 1.3 Alignment Explanation

#### Canonical Traceability Matrix

This matrix is the primary cross-reference for this use case. It lets readers trace each FDUC step to matching SSD, DLSD, DLCD, and STD references.

| FDUC ID | SSD ID | DLSD ID | DLCD reference | STD ID |
|---|---|---|---|---|
| `FDUC-1.1` | `SSD-S1-M1` | `DLSD-F1-M1` | `DLCD-C1.M1` `DocumentController.getDocuments(...)` | N/A |
| `FDUC-1.2` | `SSD-S1-M3` | `DLSD-F1-M3` | `DLCD-C1.M2` `createDocument(...)` | `STD-T2` |
| `FDUC-1.3` | `SSD-S3-M1` | `DLSD-F3-M1` | `DLCD-C1.M1` `getDocuments(...)` | N/A |
| `FDUC-1.4` | `SSD-S3-M3` | `DLSD-F3-M3` | `DLCD-S1.M3` `submitDocument(...)` | `STD-T4`, `STD-T8` |
| `FDUC-1.5` | `SSD-S4-M1` | `DLSD-F4-M3` | `DLCD-S1.M5` `getDocumentDownloadUrl(...)` | N/A |
| `FDUC-1.6` | `SSD-S4-M3` | `DLSD-F4-M12` | `DLCD-S1.M4` `reviewDocument(...)` | `STD-T5`, `STD-T6`, `STD-T7` |
| `FDUC-A1.1` | `SSD-S2-M1` | `DLSD-F2-M1` | `DLCD-S1.M2` `createDocument(...)` | `STD-T1` |
| `FDUC-A1.1` | `SSD-S2-M3` | `DLSD-F2-M9` | `DLCD-S1.M6` `uploadFileToDocument(...)` | N/A |
| `FDUC-A1.2` | `SSD-S2-M5` | `DLSD-F2-M16` | `DLCD-S1.M7` `shareDocumentWithClient(...)` | `STD-T3` |
| `FDUC-A1.3` | `SSD-S5-M1` | `DLSD-F5-M3` | `DLCD-S1.M5` `getDocumentDownloadUrl(...)` | N/A |
| `FDUC-E1` | `SSD-S3-M3E1` | `DLSD-F3-M3E1` | `DLCD-S1.M3` `submitDocument(...)` | N/A |
| `FDUC-E4` | `SSD-S4-M3E4` | `DLSD-F4-M12E4` | `DLCD-S1.M4` `reviewDocument(...)` | N/A |

Key takeaway: if you need to verify any requirement, this matrix is the single source of cross-section traceability.

#### How the use case connects to the overall system context

This table explains why this use case matters to the broader product. It pairs each system-level claim with evidence from core artifacts.

| Claim | Evidence |
|---|---|
| Document management is lifecycle-coupled work. | Use Case Diagram links Manage Transaction Documents to Manage Transaction Lifecycle through an include dependency. |
| This owned use case is actor-focused on Broker and Client. | Use Case Diagram shows Broker and Client directly connected to Manage Transaction Documents, while Admin is linked to Register User and Authenticate User. |
| End-to-end interaction crosses UI, API, DB, and object storage. | C4 L1/L2 show React frontend -> Spring backend -> PostgreSQL + Cloudflare R2. |
| The use case is interaction-centered around request, submit/review, and lifecycle dependency. | Diagram support cases and includes show Request Document, Review/Submit Document, and the transaction lifecycle coupling. |

Key takeaway: this use case is not isolated; it is a core operational slice of the broader transaction system.

#### How Use Case Diagram and Domain Model map to FDUC, inclusions, extensions, exceptions

This table shows how high-level business concepts translate into concrete flow rules. It helps verify that actors, entities, and rule types stay consistent from concept to behavior.

| Requirement Source | FDUC Mapping |
|---|---|
| Use Case actors and mid-level sub-cases | Reflected by Broker/Client actions in `FDUC-1.*`, `FDUC-A1.*`, and `FDUC-R1.*`, aligned with Request Document and Review/Submit Document support cases in the mid-level diagram. |
| Mid-level include/extend relationships | Diagram `<<include>>` links (`Manage Transaction Documents -> Manage Transaction Lifecycle`, `-> Request Document`, `-> Review/Submit Document`) and `<<extend>>` (`Request Signature -> Request Document`) map to `FDUC-EXT-1` and core flow structure. |
| DDD `Document` aggregate | Drives fields in `FDUC-1.2`, `FDUC-1.6`, `FDUC-A1.1`, including `status`, `flow`, `stage`, `requiresSignature`. |
| DDD `DocumentVersion` | Realizes file history in `FDUC-1.4`, `FDUC-R1.2`, `FDUC-A1.1`. |
| Inclusion rules | Captured as `FDUC-INC-1..FDUC-INC-4` and referenced by `SSD` and `DLSD` message IDs. |
| Extension rules | `FDUC-EXT-1` and `FDUC-EXT-2` map to guarded transitions and request creation path options. |
| Exceptions | `FDUC-E1..E6` map to validation/authorization/state guards in SSD and DLSD. |

Key takeaway: conceptual artifacts and flow rules remain aligned all the way down to concrete scenario behavior.

#### How each FDUC step predicts SSD and DLSD artifacts

This table provides a forward mapping from each requirement step to expected interaction design. It helps reviewers confirm that SSD and DLSD content is derived from the same use-case intent.

| FDUC step | Predicted SSD messages | Predicted DLSD realization |
|---|---|---|
| `FDUC-1.2` | `SSD-S1-M3` `requestDocument(...)` | Controller -> Service -> Repository (+ notify) in `DLSD-F1-*`. |
| `FDUC-1.4` | `SSD-S3-M3` `submitDocument(...)` | Service validates file, persists version, sets status in `DLSD-F3-*`. |
| `FDUC-1.6` | `SSD-S4-M3` `reviewDocument(...)` | Service enforces state rule and applies decision in `DLSD-F4-*`. |
| `FDUC-A1.2` | `SSD-S2-M5` `shareDocumentWithClient(...)` | Service performs `DRAFT -> SUBMITTED` in `DLSD-F2-*`. |

Key takeaway: each high-level step predicts concrete interaction artifacts without ambiguity.

---

## Section 2: Conceptual & Domain Level Modeling

### 2.1 Alignment Explanation

#### How domain objects support use case information needs

This table shows which domain objects carry the information each workflow step depends on.

| Domain Object | Information Need in Use Case |
|---|---|
| `Document` | Holds identity, classification, lifecycle status, actor-facing notes, and visibility fields used in all main and alternate flows. |
| `DocumentVersion` | Stores each uploaded artifact with timestamp and uploader metadata for review and revision flows. |
| `TransactionRef` | Binds each document to exactly one transaction context for access checks and queries. |
| `StorageObject` | Captures storage key and file metadata used for secure upload/download paths. |
| `DocumentStatusEnum` | Encodes valid lifecycle decisions enforced in review and resubmission paths. |
| `DocumentFlowEnum` | Distinguishes REQUEST and UPLOAD behavior paths. |

Key takeaway: the domain model directly reflects the data that users see and act on in the workflow.

#### How C4 L3 models correspond to DDD, SSD, and UI/UX

This table links each architecture area to the same behavior described in domain and interaction views. It helps check consistency across structure, behavior, and interface design.

| C4 L3 area | Correspondence |
|---|---|
| Frontend components | Screens in Section 3 call SSD-described APIs and render DTO fields derived from `Document` and `DocumentVersion`. |
| Backend components | Controller/service/repository stack in DLSD realizes SSD messages and DDD invariants. |
| Database model | `documents` and `document_versions` tables persist the same attributes and relations used by UI and service logic. |

#### Consistency across domain entities -> persistent storage -> components/services

This table tracks the same business meaning from domain model to storage, service logic, API shape, and UI usage. It helps verify that information stays consistent end to end.

| Chain Stage | Consistency Rule |
|---|---|
| Domain (`Document`, `DocumentVersion`) | Declares core fields and lifecycle invariants. |
| Storage (`documents`, `document_versions`) | Uses matching column semantics and identifiers for those fields. |
| Service (`DocumentServiceImpl`) | Applies lifecycle guards and orchestration aligned to domain and storage. |
| API/DTO (`DocumentRequestDTO`, `DocumentResponseDTO`) | Exposes only fields needed by UI screens for the same flows. |
| UI (Section 3 titles) | Collects and presents data mapped one-to-one to DTO/domain attributes. |

---

## Section 3: UI/UX Design

### 3.1 UI/UX Design Titles

1. **Documents Center - Broker View**
2. **Request Document Modal**
3. **Documents Center - Client View**
4. **Submit Document Modal (Client)**
5. **Broker Review Modal**
6. **Document Download / Preview State**

### 3.2 Alignment Explanation

#### UI field dictionary (domain + validation mapping)

This table is the field-level mapping between what users enter in UI and what the system stores and validates.

| UI Field | Domain Attribute | Type | Validation Rule | DTO/Entity Source |
|---|---|---|---|---|
| Document Type | `Document.docType` | enum | Required; must be a valid `DocumentTypeEnum` | `DocumentRequestDTO.docType` |
| Expected From | `Document.expectedFrom` | enum | Required; valid `DocumentPartyEnum` | `DocumentRequestDTO.expectedFrom` |
| Stage | `Document.stage` | enum | Required; valid `StageEnum` | `DocumentRequestDTO.stage` |
| Notes | `Document.brokerNotes` | string | Optional text | `DocumentRequestDTO.brokerNotes` |
| Due Date | `Document.dueDate` | datetime | Optional; must be future date when supplied | `DocumentRequestDTO.dueDate` |
| Requires Signature | `Document.requiresSignature` | boolean | If true, source file must exist before send | `DocumentRequestDTO.requiresSignature` |
| Upload File | `StorageObject` fields via new `DocumentVersion` | binary + metadata | Required on submit/upload; MIME and size validation | `submitDocument(...)`, `uploadFileToDocument(...)` |
| Review Decision | `Document.status` | enum | Must be one of `APPROVED`, `NEEDS_REVISION`, `REJECTED` | `DocumentReviewRequestDTO.decision` |
| Review Comments | `Document.brokerNotes` | string | Optional | `DocumentReviewRequestDTO.comments` |

Key takeaway: each visible UI field has a direct domain and DTO target, which keeps input handling predictable.

#### UI navigation mapping to scenario steps

This table explains how each screen transition maps back to scenario IDs and backend behavior.

| Screen | Trigger | SSD ID | Backend Action | Next Screen/State |
|---|---|---|---|---|
| Documents Center - Broker | Broker opens documents tab | `SSD-S1-M1` | `getDocumentsForTransaction(...)` | Broker list loaded |
| Request Document Modal | Broker clicks Request Document and submits | `SSD-S1-M3` | `createDocument(... flow=REQUEST, status=REQUESTED)` | Requested item visible |
| Documents Center - Client | Client opens documents tab | `SSD-S3-M1` | `getDocumentsForTransaction(...)` | Client list loaded |
| Submit Document Modal | Client uploads file | `SSD-S3-M3` | `submitDocument(...)` | Status becomes `SUBMITTED` |
| Broker Review Modal | Broker submits decision | `SSD-S4-M3` | `reviewDocument(...)` | `APPROVED` / `NEEDS_REVISION` / `REJECTED` |
| Download / Preview State | Actor clicks download | `SSD-S4-M1`, `SSD-S5-M1` | `getDocumentDownloadUrl(...)` | File opens through presigned URL |

Key takeaway: navigation steps are aligned with scenario steps, so UX flow and system behavior stay in sync.

#### How Figma data matches C4 L3 data tier model

This table verifies that UI data representation matches backend data types and constraints.

| Figma Element | Data Tier Column | Data Type | Validation/Constraint |
|---|---|---|---|
| Document type dropdown | `documents.doc_type` | enum string | Must match backend enum |
| Status badge | `documents.status` | enum string | System-controlled transition rules |
| Flow tag | `documents.flow` | enum string | `REQUEST` or `UPLOAD` |
| Requires signature toggle | `documents.requires_signature` | boolean | Default `false` |
| File upload control | `document_versions.file_name`, `mime_type`, `size_bytes`, `s3key` | string/string/number/string | Must pass file validators before persistence |
| Version timestamp | `document_versions.uploaded_at` | timestamp | System-generated |
| Decision controls | `documents.status` via review API | enum string | Limited to review decision set |

Key takeaway: design-level UI data assumptions match the modeled storage layer contract.

---

## Section 4: Interaction, Behavioral, and Logical Design

### 4.1 System Sequence Diagram (SSD)

[Paste System Sequence Diagram]

### 4.2 Design Level Sequence Diagram (DLSD)

**Combined (Reference):**

[Paste Design Level Sequence Diagram]

**Flow 1 - Request Document (F1):**

[Paste Design Level Sequence Diagram - F1 Request]

**Flow 2 - Upload and Share (F2):**

[Paste Design Level Sequence Diagram - F2 UploadShare]

**Flow 3 - Client Submit (F3):**

[Paste Design Level Sequence Diagram - F3 ClientSubmit]

**Flow 4 - Broker Review (F4):**

[Paste Design Level Sequence Diagram - F4 BrokerReview]

**Flow 5 - Client Download (F5):**

[Paste Design Level Sequence Diagram - F5 ClientDownload]

### 4.3 Design Level Class Diagram (DLCD)

[Paste Design Level Class Diagram]

### 4.4 State Transition Diagram (STD)

[Paste State Transition Diagram]

### 4.5 Alignment Explanation

#### How SSD messages correspond one-to-one with FDUC scenarios, inclusions, extensions, and exceptions

This table maps each system interaction to a specific use-case step and rule type. It helps check coverage and prevent orphan SSD messages that are not grounded in requirements.

| SSD ID | FDUC Mapping | Notes |
|---|---|---|
| `SSD-S1-M1..M4` | `FDUC-1.1`, `FDUC-1.2` | Includes `FDUC-INC-1`, `FDUC-INC-2`, `FDUC-INC-4`; extension `FDUC-EXT-1` guard applies at request send. |
| `SSD-S2-M1..M6` | `FDUC-A1.1`, `FDUC-A1.2` | Represents UPLOAD create/upload/share sequence; optional draft path naturally represented. |
| `SSD-S3-M1..M4` | `FDUC-1.3`, `FDUC-1.4`, `FDUC-R1.2` | Includes `FDUC-INC-1`, `FDUC-INC-3`, `FDUC-INC-4`; exception `FDUC-E1` on file validation. |
| `SSD-S4-M1..M4` | `FDUC-1.5`, `FDUC-1.6` | Includes `FDUC-INC-2`; exception `FDUC-E4` on invalid review transition. |
| `SSD-S5-M1..M2` | `FDUC-A1.3` | Download branch with access check behavior. |

Key takeaway: every SSD message cluster is grounded in a specific use-case segment and rule set.

#### How DLSD realizes SSD messages across three tiers

This table summarizes responsibilities across frontend, controller, service, and data/storage layers. It clarifies which layer performs each part of the interaction.

| Tier | Realization |
|---|---|
| Frontend | `DocumentsCenter` and modal interactions call API through `axiosInstance`. |
| Presentation | `DocumentController` maps HTTP actions to typed service calls. |
| Business | `DocumentServiceImpl` enforces validation and status transition guards. |
| Data/Infra | `DocumentRepository` persists metadata; `ObjectStorageService` handles file storage and presigned URLs. |

Key takeaway: responsibility is intentionally layered to keep UI, orchestration, and persistence concerns separate.

#### How DLSD components align with C4 L3 components

This table maps runtime participants to C4 L3 architecture components. It keeps names and responsibilities aligned between dynamic sequence views and static component views.

| DLSD Participant | C4 L3 Component |
|---|---|
| `DocumentsCenter` | Frontend Documents feature component |
| `axiosInstance` | Frontend API client component |
| `DocumentController` | Backend presentation component |
| `DocumentServiceImpl` | Backend business/service component |
| `DocumentRepository` | Backend repository component |
| `ObjectStorageService` | Backend infrastructure adapter |
| `Cloudflare R2` (external) | C4 external storage dependency |

#### How DLCD reflects classes and methods implied by DLSD

This table ties key interaction messages to concrete class methods. It verifies that sequence behavior is supported by explicit class-level contracts.

| DLSD Interaction | DLCD Method Reference |
|---|---|
| `DLSD-F1-M3 createDocument` | `DLCD-S1.M2` `DocumentService.createDocument(...)` |
| `DLSD-F2-M9 uploadFileToDocument` | `DLCD-S1.M6` `uploadFileToDocument(...)` |
| `DLSD-F3-M3 submitDocument` | `DLCD-S1.M3` `submitDocument(...)` |
| `DLSD-F4-M12 reviewDocument` | `DLCD-S1.M4` `reviewDocument(...)` |
| `DLSD-F4-M3`, `DLSD-F5-M3` download URL | `DLCD-S1.M5` `getDocumentDownloadUrl(...)` |

Key takeaway: interaction messages are backed by explicit class-level operations, not implicit behavior.

#### How class responsibilities and signatures support domain invariants

This table shows where core business rules are enforced in design and implementation. It helps reviewers confirm that invariant ownership is explicit and testable.

| Domain Invariant | Responsible Method(s) |
|---|---|
| Only authorized submit sources reach `SUBMITTED` | `verifyEditAccess(...)` and transaction checks inside `submitDocument(...)` |
| Review decisions are allowed only from `SUBMITTED` | status guard and role/access checks inside `reviewDocument(...)` |
| Signature request requires source file | `sendDocumentRequest(...)` applies the guard when `requiresSignature=true` |
| REQUEST and UPLOAD paths remain distinct | `createDocument(...)`, `sendDocumentRequest(...)`, and `shareDocumentWithClient(...)` apply flow/status guards |

#### How state constraints influence DLSD and code

This table explains how allowed status transitions shape sequence paths and service behavior. It helps verify that lifecycle rules are implemented, not only modeled.

| STD Transition | Code/DLSD Impact |
|---|---|
| `STD-T1` `[*] -> DRAFT` | UPLOAD creation path initializes draft state. |
| `STD-T2` `DRAFT -> REQUESTED` | Request send path commits a draft request. |
| `STD-T3` `DRAFT -> SUBMITTED` | Upload share path exposes broker-uploaded file to client. |
| `STD-T4` `REQUESTED -> SUBMITTED` | Submission API stores new version and advances status. |
| `STD-T5..T7` `SUBMITTED -> decision` | Review API validates and applies decision. |
| `STD-T8` `NEEDS_REVISION -> SUBMITTED` | Resubmission re-enters review-ready state. |

Key takeaway: state rules are enforced as runtime checks, not just documentation.

---

## Section 5: Implementation

### 5.1 Code Snippets

#### Snippet `SNP-C1`: Controller create endpoint (`DLSD-F1-M3`, `FDUC-1.2`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java
@PostMapping
@PreAuthorize("hasRole('BROKER')")
public ResponseEntity<DocumentResponseDTO> createDocument(
        @PathVariable UUID transactionId,
        @RequestBody DocumentRequestDTO requestDTO,
        @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
        HttpServletRequest request) {
    UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createDocument(transactionId, requestDTO, brokerId));
}
```

Proof: Maps the broker request creation action directly to service-layer creation logic.

#### Snippet `SNP-C2`: Controller submit endpoint (`DLSD-F3-M3`, `FDUC-1.4`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java
@PostMapping("/{documentId}/submit")
@PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
public ResponseEntity<DocumentResponseDTO> submitDocument(
        @PathVariable UUID transactionId,
        @PathVariable UUID documentId,
        @RequestParam("file") MultipartFile file,
        @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest request) throws IOException {
    UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
    UploadedByRefEnum uploaderType = UploadedByRefEnum.CLIENT;
    if (jwt != null) {
        List<String> roles = jwt.getClaimAsStringList("https://courtierpro.dev/roles");
        if (roles != null && roles.contains("BROKER")) uploaderType = UploadedByRefEnum.BROKER;
    }
    return ResponseEntity.ok(service.submitDocument(transactionId, documentId, file, userId, uploaderType));
}
```

Proof: Handles multipart upload request and resolves uploader role before calling submit logic.

#### Snippet `SNP-C3`: Controller review endpoint (`DLSD-F4-M12`, `FDUC-1.6`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java
@PatchMapping("/{documentId}/review")
@PreAuthorize("hasRole('BROKER')")
public ResponseEntity<DocumentResponseDTO> reviewDocument(
        @PathVariable UUID transactionId,
        @PathVariable UUID documentId,
        @RequestBody @Valid DocumentReviewRequestDTO reviewDTO,
        HttpServletRequest request) {
    UUID brokerId = UserContextUtils.resolveUserId(request, null);
    return ResponseEntity.ok(service.reviewDocument(transactionId, documentId, reviewDTO, brokerId));
}
```

Proof: Restricts review to broker role and delegates decision processing to the service.

#### Snippet `SNP-C4`: Controller download URL endpoint (`DLSD-F4-M3`, `DLSD-F5-M3`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java
@GetMapping("/{documentId}/versions/{versionId}/download")
@PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
public ResponseEntity<Map<String, String>> getDocumentDownloadUrl(
        @PathVariable UUID transactionId,
        @PathVariable UUID documentId,
        @PathVariable UUID versionId,
        @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest request) {
    UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
    String url = service.getDocumentDownloadUrl(documentId, versionId, userId);
    return ResponseEntity.ok(Map.of("url", url));
}
```

Proof: Exposes a secure URL retrieval endpoint used by both broker and client download flows.

#### Snippet `SNP-C5`: Controller upload-to-draft endpoint (`DLSD-F2-M9`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/presentationlayer/DocumentController.java
@PostMapping("/{documentId}/upload")
@PreAuthorize("hasRole('BROKER')")
public ResponseEntity<DocumentResponseDTO> uploadFileToDocument(
        @PathVariable UUID transactionId,
        @PathVariable UUID documentId,
        @RequestParam("file") MultipartFile file,
        @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
        HttpServletRequest request) throws IOException {
    UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
    return ResponseEntity.ok(
            service.uploadFileToDocument(transactionId, documentId, file, brokerId, UploadedByRefEnum.BROKER));
}
```

Proof: Handles broker draft upload requests and routes them to upload-specific service logic.

#### Snippet `SNP-S1`: Service contract for core methods (`DLCD-S1`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentService.java
public interface DocumentService {
    DocumentResponseDTO createDocument(UUID transactionId, DocumentRequestDTO requestDTO, UUID userId);
    DocumentResponseDTO submitDocument(UUID transactionId, UUID documentId, MultipartFile file,
                    UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException;
    String getDocumentDownloadUrl(UUID documentId, UUID versionId, UUID userId);
    DocumentResponseDTO reviewDocument(UUID transactionId, UUID documentId,
                    DocumentReviewRequestDTO reviewDTO, UUID brokerId);
    DocumentResponseDTO uploadFileToDocument(UUID transactionId, UUID documentId, MultipartFile file,
                    UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException;
    DocumentResponseDTO shareDocumentWithClient(UUID documentId, UUID brokerId);
}
```

Proof: Defines the exact business operations used across the five DLSD flows.

#### Snippet `SNP-S2`: Service access guard methods (`FDUC-INC-2`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
private void verifyEditAccess(Transaction tx, UUID userId) {
    if (tx.getClientId().equals(userId)) return;
    verifyBrokerOrCoManager(tx, userId,
            com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
}

private void verifyBrokerOrCoManager(Transaction tx, UUID userId,
                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission requiredPermission) {
    String userEmail = userAccountRepository.findById(userId).map(UserAccount::getEmail).orElse(null);
    java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants =
            participantRepository.findByTransactionId(tx.getTransactionId());
    TransactionAccessUtils.verifyBrokerOrCoManagerAccess(tx, userId, userEmail, participants, requiredPermission);
}
```

Proof: Centralizes edit authorization checks before state-changing operations.

#### Snippet `SNP-S3`: Service createDocument core initialization (`DLSD-F1-M4`, `STD-T1`, `STD-T2`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
Transaction tx = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
verifyBrokerOrCoManager(tx, userId,
        com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

DocumentStatusEnum status = DocumentStatusEnum.REQUESTED;
if (requestDTO.getStatus() != null) {
    if (requestDTO.getStatus() == DocumentStatusEnum.DRAFT
            || requestDTO.getStatus() == DocumentStatusEnum.REQUESTED) {
        status = requestDTO.getStatus();
    }
}

document.setStatus(status);
document.setFlow(requestDTO.getFlow() != null ? requestDTO.getFlow() : DocumentFlowEnum.REQUEST);
document.setRequiresSignature(requestDTO.getRequiresSignature() != null ? requestDTO.getRequiresSignature() : false);
Document savedDocument = repository.save(document);
```

Proof: Shows the initial state/flow rules applied when a document is created.

#### Snippet `SNP-S4`: Service submitDocument transition (`DLSD-F3-M4`, `STD-T4`, `STD-T8`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
Document document = repository.findByDocumentId(documentId)
        .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
    throw new BadRequestException("Document does not belong to transaction: " + transactionId);
}
Transaction tx = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
verifyEditAccess(tx, uploaderId);

StorageObject storageObject = storageService.uploadFile(file, transactionId, documentId);
DocumentVersion version = DocumentVersion.builder()
        .versionId(UUID.randomUUID())
        .uploadedAt(LocalDateTime.now())
        .uploadedBy(uploadedBy)
        .storageObject(storageObject)
        .document(document)
        .build();

document.getVersions().add(version);
document.setStatus(DocumentStatusEnum.SUBMITTED);
document.setLastUpdatedAt(LocalDateTime.now());
Document savedDocument = repository.save(document);
```

Proof: Persists a new version and advances status to `SUBMITTED` in the submit flow.

#### Snippet `SNP-S5`: Service reviewDocument guard + decision (`DLSD-F4-M13`, `FDUC-E4`, `STD-T5..T7`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
Document document = repository.findByDocumentId(documentId)
        .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
    throw new BadRequestException("Document does not belong to this transaction");
}
Transaction tx = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
verifyBrokerOrCoManager(tx, brokerId,
        com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
if (document.getStatus() != DocumentStatusEnum.SUBMITTED) {
    throw new BadRequestException("Only submitted documents can be reviewed");
}

document.setStatus(reviewDTO.getDecision());
document.setBrokerNotes(reviewDTO.getComments());
document.setLastUpdatedAt(LocalDateTime.now());
Document updated = repository.save(document);
```

Proof: Enforces review-state precondition and applies the broker decision atomically.

#### Snippet `SNP-S6`: Service shareDocumentWithClient transition (`DLSD-F2-M17`, `STD-T3`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
if (document.getStatus() != DocumentStatusEnum.DRAFT) {
    throw new BadRequestException("Only draft documents can be shared with client");
}
if (document.getFlow() != DocumentFlowEnum.UPLOAD) {
    throw new BadRequestException("Only UPLOAD flow documents can be shared using this endpoint");
}

document.setStatus(DocumentStatusEnum.SUBMITTED);
document.setVisibleToClient(true);
document.setLastUpdatedAt(LocalDateTime.now());
Document savedDocument = repository.save(document);
```

Proof: Captures the explicit DRAFT-to-SUBMITTED share behavior for upload flow.

#### Snippet `SNP-S7`: Service download URL generation (`DLSD-F4-M4`, `DLSD-F5-M4`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
verifyViewAccess(tx, userId);

DocumentVersion version = document.getVersions().stream()
        .filter(v -> v.getVersionId().equals(versionId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Document version not found: " + versionId));

String s3Key = version.getStorageObject().getS3Key();
String fileName = version.getStorageObject().getFileName();
if (fileName == null || fileName.isBlank()) {
    return storageService.generatePresignedUrl(s3Key);
}
return storageService.generatePresignedUrl(s3Key, fileName);
```

Proof: Uses view access checks plus presigned URL generation for secure downloads.

#### Snippet `SNP-S8`: Service uploadFileToDocument keeps draft lifecycle (`DLSD-F2-M10`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/businesslayer/DocumentServiceImpl.java
Document document = repository.findByDocumentId(documentId)
        .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
    throw new BadRequestException("Document does not belong to transaction: " + transactionId);
}
Transaction tx = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
verifyEditAccess(tx, uploaderId);

StorageObject storageObject = storageService.uploadFile(file, transactionId, documentId);
DocumentVersion version = DocumentVersion.builder()
        .versionId(UUID.randomUUID())
        .uploadedAt(LocalDateTime.now())
        .uploadedBy(uploadedBy)
        .storageObject(storageObject)
        .document(document)
        .build();
document.getVersions().add(version);
document.setLastUpdatedAt(LocalDateTime.now());
Document savedDocument = repository.save(document);
```

Proof: Adds file versions to a draft document without forcing a review decision transition.

#### Snippet `SNP-D1`: Repository methods used by core flows (`DLCD-D1`)

```java
// backend/src/main/java/com/example/courtierprobackend/documents/datalayer/DocumentRepository.java
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocumentId(UUID documentId);
    List<Document> findByTransactionRef_TransactionId(UUID transactionId);
    List<Document> findByTransactionRef_TransactionIdAndStage(UUID transactionId, StageEnum stage);
}
```

Proof: Provides the document lookup patterns used in create/read/review/share lifecycle operations.

#### Snippet `SNP-I1`: Object storage upload and presigned URL (`DLCD-I1`, `DLSD-F3-M7`, `DLSD-F4-M5`)

```java
// backend/src/main/java/com/example/courtierprobackend/infrastructure/storage/ObjectStorageService.java
public StorageObject uploadFile(MultipartFile file, UUID transactionId, UUID documentId) throws IOException {
    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
    String uniqueId = UUID.randomUUID().toString();
    String objectKey = String.format("documents/%s/%s/%s_%s", transactionId, documentId, uniqueId, originalFilename);
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(file.getContentType())
            .build();
    s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    return StorageObject.builder().s3Key(objectKey).fileName(originalFilename)
            .mimeType(file.getContentType()).sizeBytes(file.getSize()).build();
}

public String generatePresignedUrl(String objectKey, String downloadFileName) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .getObjectRequest(getObjectRequest)
            .build();
    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
}
```

Proof: Shows how uploaded content is persisted and later exposed through time-limited download URLs.

#### Snippet `SNP-F1`: Frontend document submit + review API calls (`DLSD-F3-M2`, `DLSD-F4-M11`)

```typescript
// frontend/src/features/documents/api/documentsApi.ts
export const submitDocument = async (
    transactionId: string,
    documentId: string,
    file: File
): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosInstance.post<Document>(
        `/transactions/${transactionId}/documents/${documentId}/submit`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' }, handleLocally: true }
    );
    return response.data;
};

export const reviewDocument = async (
    transactionId: string,
    documentId: string,
    decision: 'APPROVED' | 'NEEDS_REVISION',
    comments?: string
): Promise<Document> => {
    const response = await axiosInstance.patch<Document>(
        `/transactions/${transactionId}/documents/${documentId}/review`,
        { decision, comments },
        { handleLocally: true }
    );
    return response.data;
};
```

Proof: Matches frontend request payloads and routes to the corresponding backend lifecycle endpoints.

### 5.2 Alignment Explanation

#### Code implements the UI/UX flows defined in Section 3

This table shows the direct route from user actions to endpoints and service methods.

| UI Flow | Implementation Evidence |
|---|---|
| Request document | `POST /transactions/{txId}/documents` -> `createDocument(...)` |
| Submit requested document | `POST /transactions/{txId}/documents/{docId}/submit` -> `submitDocument(...)` |
| Review submitted document | `PATCH /transactions/{txId}/documents/{docId}/review` -> `reviewDocument(...)` |
| Download document version | `GET /transactions/{txId}/documents/{docId}/versions/{verId}/download` -> `getDocumentDownloadUrl(...)` |
| Upload for client and share | `createDocument(...)` + `uploadFileToDocument(...)` + `shareDocumentWithClient(...)` |

Key takeaway: the implemented API surface mirrors the UI workflow states one-to-one.

#### Code flow and components correspond to DLSD messages

This table traces each design-level message to the actual code path and snippet evidence. It helps readers validate that documented flows are implemented in code.

| DLSD ID | Code Path | Snippet Evidence |
|---|---|---|
| `DLSD-F1-M3` | Controller `createDocument` -> Service `createDocument` -> Repository save | `SNP-C1`, `SNP-S3`, `SNP-D1` |
| `DLSD-F2-M9` | Controller `uploadFileToDocument` -> Service upload -> Storage upload | `SNP-C5`, `SNP-S8`, `SNP-I1` |
| `DLSD-F3-M3` | Controller `submitDocument` -> Service validate/store/update | `SNP-C2`, `SNP-S4`, `SNP-F1` |
| `DLSD-F4-M12` | Controller `reviewDocument` -> Service validate/update/notify | `SNP-C3`, `SNP-S5`, `SNP-F1` |
| `DLSD-F5-M3` | Controller `getDocumentDownloadUrl` -> Storage `generatePresignedUrl` | `SNP-C4`, `SNP-S7`, `SNP-I1` |

Key takeaway: every core DLSD message is traceable to concrete controller, service, and storage code paths.

#### Classes, interfaces, and signatures match DLCD

This table confirms that the class model and the implementation contract stay aligned.

| DLCD Reference | Implementation Contract |
|---|---|
| `DLCD-C1` | `DocumentController` owns endpoint-to-service mapping methods |
| `DLCD-S1` | `DocumentService` interface declares core flow operations |
| `DLCD-S2` | `DocumentServiceImpl` realizes state, access, and transition rules |
| `DLCD-D1` | `DocumentRepository` handles aggregate persistence |
| `DLCD-I1` | `ObjectStorageService` provides upload/download-url behavior |

#### Domain logic and entity validation match use case rules

| Rule | Enforcement Point |
|---|---|
| Submit path requires transaction consistency and edit permission | `submitDocument(...)` checks transaction ownership and calls `verifyEditAccess(...)` |
| Review allowed only from `SUBMITTED` | `reviewDocument(...)` enforces `document.getStatus() == SUBMITTED` |
| Signature guard when requested | Creation/send flow uses `requiresSignature` flag before sending request |
| Upload/download path uses storage contract | `storageService.uploadFile(...)` and `storageService.generatePresignedUrl(...)` |

#### Data types and constraints match C4 L3 data tier model

| Data Tier Field | Backend Type | Frontend Type |
|---|---|---|
| `documents.document_id` | `UUID` | `string` |
| `documents.status` | `DocumentStatusEnum` | string union |
| `documents.flow` | `DocumentFlowEnum` | string union |
| `documents.requires_signature` | `boolean` | `boolean` |
| `document_versions.file_name` | `String` | `string` |
| `document_versions.mime_type` | `String` | `string` |
| `document_versions.size_bytes` | `Long` | `number` |
| `document_versions.s3key` | `String` | `string` |
