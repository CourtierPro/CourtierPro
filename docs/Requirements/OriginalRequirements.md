# CourtierPro Requirements Documentation

## 1. User Requirements
These requirements detail the capabilities and features that each specific user persona needs to interact with the platform effectively.

### Clients (Buyers/Sellers)
* Clients must be able to view a clear, stage-by-stage visual timeline of their real estate transaction progress.
* Clients must be able to securely upload requested documents, such as proof of financing or identification, directly from their devices.
* Clients must receive automated email reminders regarding upcoming deadlines, missing documents, and scheduled appointments.
* Clients must have the ability to request new appointments by submitting their preferred dates, times, and meeting details.
* Clients must be able to navigate a fully bilingual interface (English and French) and view translated versions of important documents.

### Brokers
* Brokers must have access to a centralized dashboard to track all active and completed transactions concurrently.
* Brokers must be able to update transaction stages (e.g., "Offer Accepted", "Financing Approved").
* Brokers must be able to review client-submitted documents and assign them a status of either "Approved" or "Needs Revision" with accompanying notes.
* Brokers must be able to review client appointment requests to confirm them, propose new timeslots, or decline them.
* Brokers must be able to view real-time performance analytics, including the average time to close, document turnaround times, and transaction volume trends.
* Brokers must be able to review a prioritized daily action list that aggregates late stage updates and documents awaiting decision.

### Administrators
* Administrators must be able to provision, modify, or deactivate user accounts for both brokers and clients from a single admin panel.
* Administrators must be able to manage system configurations, including notification templates and bilingual defaults.
* Administrators must have access to detailed audit logs to quickly troubleshoot issues and verify system data integrity.

---

## 2. System Requirements
These requirements dictate the underlying architecture, automated logic, external integrations, and security protocols the system must employ.

### Functional & Business Logic Requirements
* The system must automatically calculate the applicable GST and QST amounts on broker commissions based on current tax rates.
* The system must automatically calculate and display the percentage completion of each transaction by comparing completed stages to total stages.
* The system must strictly enforce document state transitions, ensuring a document moves from "Requested" to "Submitted", and then to either "Approved" or "Needs Revision".
* The system must ensure that each appointment is tied to exactly one transaction and requires explicit broker confirmation before changing status from "Pending" to "Confirmed".
* The system must embed and store snapshot data of Client and Broker details within a Transaction record to ensure historical accuracy, even if contact details change later.

### Architectural & Integration Requirements
* The system must be built using a 4-Tier architecture that includes a Presentation Layer, an API Gateway Layer, a Service Layer, and a Data Layer.
* The system must delegate user authentication and enforce Role-Based Access Control (RBAC) via the external Auth0 service.
* The system must utilize Amazon S3 for the secure storage and retrieval of all client-submitted files and transaction-related binary artifacts.
* The system must dispatch all transactional email notifications (such as stage updates and appointment confirmations) via Amazon SES.
* The system must support comprehensive localization, formatting dates, currency, and numerical values according to Quebec conventions in both English and French.

### Security Requirements
* The system must enforce HTTPS connections to encrypt all data in transit, and encrypt sensitive information at rest within the database.
* The system must automatically expire user sessions after periods of inactivity to prevent unauthorized access from unattended devices.
* The system must record critical events, such as logins, data changes, and permission updates, in dedicated audit logs to support compliance and traceability.
* The system must return uniform failure messages during login attempts to prevent revealing whether a specific username or password is correct.
* The system must restrict Administrators from modifying real estate transaction stages on behalf of brokers.
