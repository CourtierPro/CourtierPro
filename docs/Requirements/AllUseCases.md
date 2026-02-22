# All Use Cases

## Authentication & Session
- User logs in (Auth0 redirect)
- User logs out
- System enforces session timeout (auto-logout after 30 min inactivity)
- User confirms email change

## Profile & Preferences
- User views profile
- User edits profile (email, notification toggles, language, weekly digest)
- User switches language (EN/FR)
- User switches dark/light mode

## Global Features (All Roles)
- User searches system (Ctrl+K command palette)
- User views notification bell popover
- User views notifications page
- User submits feedback (Bug Report / Feature Request)

## Broker — Dashboard
- Broker views dashboard (KPIs, appointment widget, quick links, priority cards, activity feed)
- Broker pins transactions

## Broker — Transactions
- Broker views transaction list (filter, search, card/table view)
- Broker creates transaction
- Broker views transaction details (tabs: Details, Timeline, Properties, Search Criteria, Offers, Documents, Participants, Appointments, Conditions)
- Broker updates transaction stage
- Broker rolls back transaction stage (with reason)
- Broker closes transaction
- Broker archives transaction
- Broker views transaction timeline

## Broker — Documents
- Broker requests document from client
- Broker edits document request
- Broker uploads document for client (Save as Draft / Upload & Share)
- Broker reviews submitted document (Approve / Needs Revision / Reject)
- Broker views document checklist (per-stage)
- System auto-requests documents (per-stage)
- Broker views all documents (cross-transaction, stage filter)

## Broker — Appointments
- Broker creates appointment
- Broker reviews incoming appointment request (Confirm / Propose New Time / Decline)
- Broker cancels appointment
- Broker views appointment calendar (List View / Calendar View)

## Broker — Properties (Buy Side)
- Broker adds property
- Broker views property details
- Broker reviews property
- Broker makes offer on property

## Broker — Offers (Sell Side)
- Broker adds offer
- Broker views and compares offers
- Broker manages offer revisions/counter-offers

## Broker — Conditions
- Broker adds condition
- Broker views condition details (status, details, linked offers)
- Broker updates condition

## Broker — Participants
- Broker views participants
- Broker adds participant
- Broker edits participant
- Broker manages visitor list (Sell Side)

## Broker — Search Criteria (Buy Side)
- Broker defines search criteria
- Broker edits search criteria

## Broker — Clients
- Broker views client list (filter by status, sort by name/email/status)
- Broker views client details

## Broker — Analytics
- Broker views analytics (filters: date range, transaction type, client)
- Broker exports analytics (CSV / PDF)

## Client — Dashboard
- Client views dashboard (KPIs, transaction carousel, appointment widget, recent updates)

## Client — Transactions
- Client views transaction progress (stage tracker)
- Client views transaction timeline

## Client — Documents
- Client uploads requested document
- Client re-uploads document after needs revision
- Client views documents (cross-transaction)

## Client — Appointments
- Client requests appointment
- Client reviews appointment response (Confirm / Propose New Time / Decline)

## Client — Notifications
- Client views notifications

## Admin — Dashboard
- Admin views dashboard (KPIs, quick access, recent admin actions, system alerts, system logs preview)
- Admin triggers test alert

## Admin — User Management
- Admin views user list
- Admin invites new user
- Admin activates user
- Admin deactivates user
- Admin triggers password reset

## Admin — Organization Settings
- Admin edits organization name
- Admin sets default language
- Admin edits email templates (11 types, EN/FR, variable insertion, live preview)

## Admin — Audit & Logs
- Admin views login audit
- Admin views password reset audit
- Admin views system logs

## Admin — Resources
- Admin views resources (Transactions, Documents, Users tabs)
- Admin deletes resource (soft delete)
- Admin restores deleted resource
- Admin views deletion audit history

## Admin — Notifications & Broadcast
- Admin views notifications
- Admin sends broadcast message

## Accessibility
- System meets WCAG 2.0 AA compliance
