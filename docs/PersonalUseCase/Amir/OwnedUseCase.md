# Owned Use Case: Manage Transaction Lifecycle (UC-T1)
**Student:** Amir Ghadimi
**Project:** CourtierPro

## 1. Use Case Specification (FDUC)

| Field | Description |
| :--- | :--- |
| **Use Case Name** | Manage Transaction Lifecycle (UC-T1) |
| **Brief Description** | The broker initiates a real estate transaction, manages its legal progression through mandatory stages (e.g., Offer, Inspection, Financing), and concludes the deal, ensuring all compliance steps are recorded. |
| **Primary Actor** | Broker |
| **Preconditions** | 1. Broker is logged in and authenticated. <br> 2. Client account must already exist in the system. <br> 3. Property address must not have an existing "Active" transaction. |
| **Postconditions** | 1. Transaction state is persisted in the database. <br> 2. An immutable `TimelineEntry` is created. <br> 3. Client is notified via email/SMS. |
| **Main Success Scenario** | 1. **Initiate:** Broker selects "New Transaction," assigns a Client, and enters Property Address. <br> 2. **Monitor:** Broker views the Transaction Details page to check status. <br> 3. **Update:** Broker clicks "Update Stage" (e.g., from *Offer Accepted* to *Inspection*). <br> 4. **Logic Check:** System validates the transition is legally allowed. <br> 5. **Record:** System saves the change and appends a "Stage Changed" entry to the Timeline. <br> 6. **Notify:** System dispatches an email to the Client. <br> 7. **Close:** Once all stages are complete, Broker marks the transaction as `CLOSED`. |
| **Exception Flows** | **E1: Invalid Stage Sequence:** Broker tries to skip "Inspection." System throws `400 Bad Request`. <br> **E2: Duplicate Property:** Broker tries to create a file for an address that is already active. System throws `409 Conflict`. |

---


## 2. Conceptual & Domain-Level Modeling

### 2.1. Domain Model
**Artifact:** [DDD Domain Model](../../diagrams/CourtierProDDD.puml)
> **[INSERT YOUR DOMAIN MODEL IMAGE HERE - linked to CourtierProDDD.puml]**

**Alignment Explanation: Use Case <-> Domain Model**
The **"Manage Transaction Lifecycle" (UC-T1)** use case is directly supported by the **Transaction Aggregate Root** in the Domain Model. The specific steps in the FDUC to "Update Stage" (Step 4) rely on the `buyerStage` and `sellerStage` attributes defined in the `Transaction` entity. The domain model enforces the segregation of duties described in the use case by using the `TransactionSide` Value Object (Enum) to determine whether the `BuyerStage` or `SellerStage` transition logic applies. Furthermore, the `TimelineEntry` entity modeled in the domain provides the persistent storage required for the "Audit History" post-condition described in the FDUC.

### 2.2. Architectural Alignment (C4)
**Artifact:** [C4 Level 3 Component Diagram (Backend)](../../diagrams/CourtierProC4L3Backend.puml)
> **[INSERT YOUR C4 LEVEL 3 BACKEND IMAGE HERE - linked to CourtierProC4L3Backend.puml]**

**Alignment Explanation: Domain <-> C4 Architecture**
The `Transaction` aggregate is managed by the `TransactionService` component in the **C4 Level 3 Backend diagram**, ensuring that domain invariants (like valid stage sequences) are enforced at the service layer. The flow of events is supported by components modeling the FDUC flow:
* **Create:** Handled by the `TransactionController`.
* **Logic Check:** Enforced by the `TransactionService` business logic.
* **Notify:** Delegated to the `NotificationService`, ensuring the 'Notify Client' inclusion is decoupled.
* **Data Persistence:** The `TransactionRepository` component maps to the persistent storage of the aggregate.

---

## 3. UI/UX Design

**Artifact:** Figma Prototype
> **[INSERT LINK TO FIGMA PROTOTYPE HERE]**
> **[INSERT SCREENSHOTS OF TRANSACTION DETAILS PAGE HERE]**

**Alignment Explanation: UI <-> Domain & Data**
* **Field Mapping:** The "Current Stage" dropdown in the UI maps directly to the `buyerStage` / `sellerStage` enum in the Domain Model.
* **Navigation:** The "Update Stage" button action triggers the transition logic defined in the FDUC Step 3.
* **Data Validation:** The UI restricts validation choices based on the current state, mirroring the `StageTranslationUtil` logic in the backend.



## 4. Interaction, Behavioral, and Logical Design

### 4.1. System Behavior (SSD)
**Artifact:** [System Sequence Diagram (SSD)](SSD_ManageTransaction.puml)
> **[INSERT SSD IMAGE HERE - generated from `SSD_ManageTransaction.puml`]**

**Alignment Explanation: SSD <-> Use Case**
The **System Sequence Diagram (SSD)** follows the **FDUC Main Success Scenario** step-by-step:
* **FDUC Step 1 ("Initiate")** maps to the SSD message `createTransaction(clientId, propertyAddress, side)`.
* **FDUC Step 4 ("Update Stage")** maps directly to the SSD message `updateTransactionStage(transactionId, newStage, note)`.
* **FDUC Step 7 ("Notify")** is represented in the SSD by the return arrow `successMessage`, implying the system has completed the notification side-effect.

### 4.2. Detailed Interactions (DLSD)
**Artifact:** [Design-Level Sequence Diagram (DLSD)](DLSD_UpdateStage.puml)
> **[INSERT DLSD IMAGE HERE - generated from `DLSD_UpdateStage.puml`]**

**Alignment Explanation: DLSD <-> SSD**
The **Design-Level Sequence Diagram (DLSD)** realizes the single, high-level `updateTransactionStage` message shown in the SSD by decomposing it into specific object interactions:
1.  The `TransactionController` accepts the HTTP request.
2.  It delegates control to `TransactionServiceImpl.updateTransactionStage()`.
3.  The service retrieves the aggregate via `TransactionRepository`.
4.  The service explicitly calls `TimelineService.addEntry()`, `EmailService.sendStageUpdateEmail()`, and `NotificationService.createNotification()`.
This proves that the "black box" behavior in the SSD is correctly implemented by the collaborating classes in the design.

### 4.3. Class Structure (DLCD)
**Artifact:** [Design-Level Class Diagram (DLCD)](DLCD_TransactionDomain.puml)
> **[INSERT DLCD IMAGE HERE - generated from `DLCD_TransactionDomain.puml`]**

**Alignment Explanation: Code <-> Design**
The Java implementation faithfully reproduces the structure defined in the **Design-Level Class Diagram (DLCD)**:
* **Class Structure:** The `TransactionServiceImpl` class implements the `TransactionService` interface exactly as modeled.
* **Method Signatures:** The method `public TransactionResponseDTO updateTransactionStage(...)` in `TransactionServiceImpl.java` matches the operation signature in the DLCD.
* **Dependencies:** The DLCD shows associations between `TransactionService` and external services; this is realized in the code via Constructor Injection of `TimelineService`, `NotificationService`, and `EmailService` into `TransactionServiceImpl`, ensuring the code is loosely coupled as designed.

### 4.4. Lifecycle Modeling (STD)
**Artifact:** [State Transition Diagram (STD)](STD_TransactionLifecycle.puml)
> **[INSERT STD IMAGE HERE - generated from `STD_TransactionLifecycle.puml`]**

**Alignment Explanation: STD <-> Logic**
The State Transition Diagram visualizes the complex rules buried in `StageTranslationUtil.java` (and service logic). It ensures that a transaction cannot jump from `BUYER_PREQUALIFY_FINANCIALLY` directly to `CLOSED_SUCCESSFULLY` without passing through the mandatory legal states like `BUYER_HOME_INSPECTION` and `BUYER_FINANCING_FINALIZED`. The implementation in `TransactionServiceImpl.updateTransactionStage` validates these transitions before persisting changes.


## 5. Implementation

### 5.1. Codebase Access
* **Repository Link:** [Link to GitHub Repo]
* **Pull Request:** [Link to PR if applicable]

### 5.2. Implementation Alignment
**Alignment Explanation: Code <-> Architecture**
* **UI/UX Implementation:** The React frontend implements the flows defined in Figma. The `TransactionStageSelect.tsx` component restricts options based on the `allowedTransitions` API response, mirroring the STD rules.
* **DLSD Correspondence:** The `TransactionServiceImpl.java` flow matches the DLSD sequence line-for-line, ensuring controllers delegate to services which then coordinate repositories and external notifiers.
* **DLCD Match:** The strict typing in `TransactionRequestDTO` and `TransactionResponseDTO` ensures the data contract matches the C4 L3 definitions.
* **Domain Logic:** `StageTranslationUtil.java` contains the switch-case logic that enforces the invariants defined in the DDD model (e.g., cannot skip Audit/Inspection).

---

## 6. Use Case Validation

**Artifact:** E2E Test Suite (Cypress)
> **[INSERT SCREENSHOT OF PASSING CYPRESS TESTS HERE]**

*No E2E tests have been implemented yet.*

> **[INSERT LINK TO CYPRESS REPO/Folder HERE]**

**Alignment Explanation: Tests <-> Requirements**
* **Traceability:** Each Cypress test file is named after a specific Use Case path (e.g., `uc_t1_happy_path.cy.ts`).
* **Data Verification:** Tests assert that the text on the screen matches the data returned by the API, confirming the full stack data alignment from DB -> API -> UI.