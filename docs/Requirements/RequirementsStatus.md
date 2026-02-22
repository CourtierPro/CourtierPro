# Requirements Status — ECP Proposal vs. Final Implementation

This document compares the use cases originally planned during the design stage (ECP Proposal) with the use cases implemented across the five development sprints. Each original use case is marked as **Implemented**, **Modified**, or **Not Implemented**, with an explanation in business terms relevant to Nabizada Courtier Inc. and Quebec real estate brokerage operations.

---

## 1. Original Planned Use Cases — Status

### Broker Use Cases

| # | Original Use Case | Status | Explanation |
|---|---|---|---|
| B1 | Broker prioritizes transactions | **Modified** | Implemented as a **pinning system** on the broker dashboard. Brokers can pin specific transactions for quick access via the Pinned Transactions panel. Additionally, the system automatically surfaces priority items through smart cards highlighting expiring offers, pending documents, upcoming appointments, and approaching conditions — giving brokers both manual and automatic ways to focus on what matters most. |
| B2 | Broker assigns transactions | **Modified** | Full multi-broker transaction reassignment was not implemented, but the system supports **adding co-managing brokers** to a transaction through the Participants tab. This allows multiple brokers to collaborate on a single transaction, which fits the operational model of Nabizada Courtier Inc. as a small brokerage where formal reassignment workflows are unnecessary. |
| B3 | Broker exports transaction summary (CSV) | **Modified** | CSV export was implemented within the **Analytics** module rather than on individual transactions. Brokers can export filtered analytics data as CSV or PDF, covering transaction overviews, stage distributions, document stats, and appointment metrics — providing a more comprehensive reporting capability. |
| B4 | Broker views all transactions dashboard | **Implemented** | Fully implemented. Brokers have a dedicated **Transactions** page with card and table views, plus the **Broker Dashboard** featuring KPI cards for active transactions and clients, priority cards, and a recent activity feed. |
| B5 | Broker filters transactions | **Implemented** | Fully implemented. The transaction list supports filtering by transaction side (Buy/Sell), status (Active/Archived/All), and text search by client name or property address. |
| B6 | Broker updates transaction stage | **Implemented** | Fully implemented. Brokers can advance transactions through the defined stage workflows — six stages for buy-side (Financial Preparation through Possession) and six for sell-side (Initial Consultation through Handover) — with automatic client notification and timeline logging. |
| B7 | Broker enters transaction dates & milestones | **Modified** | Milestone tracking was replaced with a **stage-based workflow** with automatic timeline logging. Instead of manually entering dates, the system records timestamps for every stage change, document action, and appointment event. This provides an automatic audit trail without requiring brokers to manually enter milestone dates. |
| B8 | Broker views calculated tax breakdown | **Not Implemented** | Tax calculations (GST/QST) were deprioritized during development. The team focused on the core transaction management, document exchange, and communication workflows that are essential to daily brokerage operations. Tax calculation is typically handled by notaries and accountants in Quebec real estate transactions, so it was considered outside the core scope. |
| B9 | Broker receives real-time notifications | **Implemented** | Fully implemented with a comprehensive notification system including: in-app notification bell with unread count, a full notifications page, email notifications via Amazon SES with customizable bilingual templates, and toggle controls for both email and in-app notifications. |
| B10 | Broker views audit trail for transaction | **Implemented** | Fully implemented as the **Transaction Timeline** tab. Every event — stage changes, document submissions, appointment actions — is logged chronologically with timestamps, the actor who performed the action, and a description. |
| B11 | Broker runs commission tax calculation (GST/QST) | **Not Implemented** | Same rationale as B8. Commission tax calculations were deprioritized because they fall outside the core broker-client collaboration workflow. In Quebec real estate, these calculations are typically handled at closing by the notary or the brokerage's accounting department. |
| B12 | Broker inputs commission amount | **Not Implemented** | Related to B8 and B11. Commission management was deprioritized in favour of building out the full document lifecycle, appointment scheduling, and property/offer management features that directly support the day-to-day buying and selling process. |
| B13 | Broker views transaction details | **Implemented** | Fully implemented with a rich, tabbed interface covering Details, Timeline, Properties (buy side), Search Criteria (buy side), Offers (sell side), Documents, Participants, Appointments, and Conditions. This far exceeds the originally planned scope. |

### Client Use Cases

| # | Original Use Case | Status | Explanation |
|---|---|---|---|
| C1 | Client logs in | **Implemented** | Fully implemented using Auth0 with automatic redirect, role-based dashboards, and session timeout after 30 minutes of inactivity. |
| C2 | Client accesses transaction summary | **Modified** | Significantly expanded beyond the original scope. Clients have a full dashboard with KPI cards, a transaction carousel showing document completion stats, an appointment widget, and recent updates. They can access detailed transaction views including stage tracker, timeline, documents, and appointments. |
| C3 | Client views transaction timeline with % complete | **Modified** | The percentage-based progress indicator was replaced with a **visual stage tracker** that shows the current stage within the defined workflow (e.g., "Financial Preparation → Property Search → Offer and Negotiation…"). This provides clearer context for clients about where their transaction stands in the Quebec real estate process rather than an abstract percentage. |
| C4 | Client views own transaction progress | **Implemented** | Fully implemented. Clients can view their transactions via the sidebar, see the stage progress tracker, and access the full transaction timeline with all events. |
| C5 | Client confirms milestone completion | **Modified** | Replaced by the **document review workflow**. Instead of clients confirming abstract milestones, the system uses a concrete document-based approach: brokers request specific documents, clients upload them, and brokers review (approve, request revision, or reject). This maps more closely to how Quebec real estate transactions actually operate — through signed documents, inspection reports, and financing confirmations. |

### Admin Use Cases

| # | Original Use Case | Status | Explanation |
|---|---|---|---|
| A1 | Admin configures transaction stage definitions | **Modified** | Stage definitions are built into the system with fixed, industry-standard workflows for buy-side and sell-side Quebec real estate transactions. While administrators cannot dynamically redefine stages, the predefined stages reflect the standard flow of a Quebec real estate transaction and ensure consistency across all transactions. |
| A2 | Admin configures dashboard defaults & reporting periods | **Not Implemented** | Dashboard layouts are standardized by role. The analytics module provides date-range filtering for reporting periods. A fixed dashboard design was chosen to ensure a consistent, predictable experience for all users. |
| A3 | Admin views system audit logs | **Implemented** | Fully implemented with three dedicated audit views: **Login Audit** (login events with IP, user agent, expandable details), **Password Reset Audit** (reset events with status badges), and **System Logs** (organization settings change history). |
| A4 | Admin manages language settings | **Implemented** | Fully implemented. Administrators can set the default language (English or French) at the organization level via Organization Settings. Individual users can also override their language preference in their profile. The entire interface, notifications, and email templates are fully bilingual. |
| A5 | Admin manages notification templates & delivery rules | **Implemented** | Fully implemented with a comprehensive **Email Template Editor** supporting 11 notification types. Each template can be customized in both English and French with dynamic variable insertion and live preview. Users can individually toggle email and in-app notifications. |
| A6 | Admin archives old transactions | **Modified** | Implemented as a **soft-delete and restore** system within the **Resources** page. Administrators can delete transactions, documents, and users with the ability to restore them. A full deletion audit history tracks who deleted what and when, including cascaded deletions. This is safer than permanent purging and provides a complete audit trail. |
| A7 | Admin creates and manages user accounts | **Implemented** | Fully implemented. Administrators can invite new users (brokers, clients, admins) by email, activate or deactivate accounts, and trigger password resets. The invitation system sends setup instructions via email. |
| A8 | Admin configures role-based permissions | **Modified** | Permissions are enforced through a fixed role-based system (Broker, Client, Admin) built into the platform. Each role has well-defined access boundaries — brokers manage transactions and documents, clients interact with their own transactions, and admins oversee the system. While administrators cannot create custom permission profiles, the fixed roles reflect the standard operational structure of a Quebec real estate brokerage. |
| A9 | Admin manages system tax settings (GST/QST rates) | **Not Implemented** | Tax management features (GST/QST) were deprioritized during development. As noted in B8, tax calculations in Quebec real estate are typically handled by notaries at closing and the brokerage's accounting systems, so this was considered outside the core collaboration platform scope. |

### Authentication (All Roles)

| # | Original Use Case | Status | Explanation |
|---|---|---|---|
| AU1 | User logs in | **Implemented** | Fully implemented using Auth0 with secure redirect-based authentication, role-specific dashboard routing, and automatic session timeout. |
| AU2 | User resets password | **Implemented** | Implemented as an admin-triggered password reset via Auth0. Administrators can trigger a password reset for any user from the Manage Users page, and the user receives a reset link by email. |

---

## 2. New Use Cases Added During Development

The following use cases were not part of the original ECP Proposal but were added during the five development sprints to better serve the real-world needs of Quebec real estate brokerage operations.

### Broker — Document Management
- **Broker requests document from client** — Brokers can formally request documents (e.g., proof of financing, inspection reports) with descriptions and due dates.
- **Broker edits document request** — Brokers can modify document request details (name, description, due date) after creation.
- **Broker uploads document for client** — Brokers can upload documents directly, with the option to save as draft or share immediately.
- **Broker reviews submitted document** — Full review workflow with Approve, Needs Revision, and Reject actions plus review notes.
- **Broker views document checklist** — Per-stage checklists showing which documents are expected and which are complete.
- **System auto-requests documents** — The system can automatically generate document requests based on the transaction stage, ensuring no required documents are missed.
- **Broker views all documents** — Cross-transaction document view with stage filtering.

### Broker — Appointment Management
- **Broker creates appointment** — Schedule appointments with clients for property inspections, notary signings, consultations, house visits, open houses, and more.
- **Broker reviews incoming appointment request** — Confirm, propose a new time, or decline client-initiated requests.
- **Broker cancels appointment** — Cancel with a reason and automatic notification to the other party.
- **Broker views appointment calendar** — List and calendar views for managing the appointment schedule.

### Broker — Properties (Buy Side)
- **Broker adds property** — Maintain a shortlist of potential properties for buyers with address, price, and feature details.
- **Broker views property details** — View full details of a shortlisted property.
- **Broker reviews property** — Approve or decline properties on the shortlist.
- **Broker makes offer on property** — Submit offers with price, conditions, and expiry date.

### Broker — Offers (Sell Side)
- **Broker adds offer** — Record offers received from potential buyers.
- **Broker views and compares offers** — Side-by-side comparison to help sellers evaluate competing offers.
- **Broker manages offer revisions** — Track offer revision history and counter-offers for each offer.

### Broker — Conditions
- **Broker adds condition** — Track contractual conditions (e.g., financing, inspection) attached to offers.
- **Broker views condition details** — Monitor condition status, with links to associated offers.
- **Broker updates condition** — Update condition status and details.

### Broker — Participants
- **Broker adds participant** — Add transaction participants such as notaries, inspectors, and other brokers.
- **Broker edits participant** — Update participant information.
- **Broker manages visitor list** — Track property visitors during open houses and private showings (sell side).

### Broker — Search Criteria (Buy Side)
- **Broker defines search criteria** — Set buyer preferences for price range, bedrooms, bathrooms, property type, and preferred neighborhoods.
- **Broker edits search criteria** — Update existing search criteria as buyer needs evolve.

### Broker — Client Management
- **Broker views client list** — Card-grid view with filtering by status and sorting by name, email, or status.
- **Broker views client details** — View contact information and associated transactions.

### Broker — Analytics
- **Broker views analytics** — Comprehensive performance dashboard with transaction overview, monthly activity charts, stage distribution, pipeline funnels, house visit stats, showing stats, property metrics, offer analytics, document completion rates, appointment stats, condition tracking, client engagement metrics, and trend analysis.
- **Broker exports analytics** — Download filtered analytics data as CSV or PDF.

### Broker — Transaction Lifecycle
- **Broker pins transactions** — Pin important transactions to the dashboard for quick access.
- **Broker closes transaction** — Mark a transaction as completed.
- **Broker archives transaction** — Archive completed or inactive transactions.
- **Broker rolls back transaction stage** — Revert a transaction to a previous stage with a required reason.

### Client — Enhanced Experience
- **Client views dashboard** — Personalized dashboard with KPIs, transaction carousel, appointment widget, and recent updates.
- **Client uploads requested document** — Full document upload workflow with drag-and-drop.
- **Client re-uploads document after revision** — Re-submit a corrected document after broker requests changes.
- **Client views documents** — Cross-transaction document view with status tracking.
- **Client requests appointment** — Client-initiated appointment scheduling with broker.
- **Client reviews appointment response** — Respond to broker proposals with confirm, counter-propose, or decline.

### Admin — Enhanced Administration
- **Admin views dashboard** — Seven KPI cards, quick-access buttons, recent admin actions, system alerts, and system logs preview.
- **Admin triggers test alert** — Test the system alert functionality.
- **Admin manages resources** — Tabbed resource management for transactions, documents, and users with soft delete, restore, and deletion audit history.
- **Admin sends broadcast message** — Send notifications to all system users.
- **Admin views password reset audit** — Dedicated audit page for password reset events.

### Global Features (All Roles)
- **User searches system** — Command palette (Ctrl+K) to search across transactions, documents, users, pages, and appointments.
- **User views profile** — User profile management with notification preferences, language selection, and email editing.
- **User edits profile** — Update email, notification toggles, language, and weekly digest preferences.
- **User confirms email change** — Email confirmation flow when a user changes their email address.
- **User switches dark/light mode** — Theme toggle with persistent preference.
- **User submits feedback** — In-app feedback submission for bug reports and feature requests.
- **System enforces session timeout** — Automatic logout after 30 minutes of inactivity for security.
- **System meets WCAG 2.0 AA compliance** — Accessibility conformance across all pages per Quebec web accessibility standards.

---

## 3. Summary

| Category | Implemented | Modified | Not Implemented | New (Added in Sprints) |
|---|---|---|---|---|
| Broker (Original) | 5 | 4 | 2 | 35+ |
| Client (Original) | 2 | 3 | 0 | 7+ |
| Admin (Original) | 4 | 2 | 2 | 5+ |
| Auth (Original) | 2 | 0 | 0 | 3+ |
| **Totals** | **13** | **9** | **4** | **50+** |

Out of the **26 original planned use cases**, **13 were fully implemented**, **9 were modified** to better fit real-world Quebec real estate workflows, and **4 were not implemented** because they fell outside the core brokerage-client collaboration scope (primarily tax/commission features and dashboard configuration).

The development sprints added over **50 new use cases** that were not part of the original proposal, dramatically expanding the platform's capabilities in document management, appointment scheduling, property and offer tracking, analytics, accessibility, and administrative oversight.
