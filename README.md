<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/shawn-nabizada/CourtierPro">
    <img src="images/logo.png" alt="Logo" width="400">
  </a>

<h3 align="center">CourtierPro</h3>

  <p align="center">
    A bilingual broker–client management and communication platform for real estate brokerages in Quebec.
    <br />
    <a href="https://github.com/shawn-nabizada/CourtierPro/docs"><strong>Explore the docs »</strong></a>
    <br />
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
### Overview
**CourtierPro** is a bilingual (English/French) broker–client management and communication platform designed for **Nabizada Courtier Inc.**, a Quebec-based real estate brokerage. The system centralizes all transaction stages, document exchanges, appointments, and communication in one secure environment.

It allows **brokers, clients, and administrative staff** to collaborate seamlessly across all stages of a real estate transaction. From the first offer to final notary signing, while providing real-time visibility into progress, required documents, and scheduled appointments.

### Academic Context
This project was developed as part of **Champlain College Saint-Lambert’s “External Client Project (ECP)”**, a capstone initiative in the Computer Science program.  
In this course, student teams:
- **Find and collaborate with a real external client** to identify an authentic business problem.  
- **Analyze, design, and build a working information system** completely from scratch.  
- **Apply domain-driven design (DDD)** and layered architecture principles across the system’s modules.  
- **Set up full CI/CD pipelines** to automate deployment and quality control.  
- **Deploy the final system independently**, demonstrating real-world readiness.  
- **Document and present** every phase, from proposal to final implementation.

CourtierPro was created by - Shawn Nabizada, Amir Ghadimi, Olivier Goudreault, and Isaac Nachate - as a full-stack bilingual web platform.

### Key Features
- **Transaction Tracking:** Clients and brokers can monitor the entire real estate process with stage-by-stage visibility and timelines.
- **Document Management:** Upload, approve, and track documents with clear submission and review statuses.
- **Appointment Scheduling:** Integrated list and calendar views for property visits, signings, and discussions, with automatic reminders.
- **Analytics Dashboard:** Brokers can review KPIs such as time-to-close, active client counts, and document turnaround times.
- **Bilingual Interface:** Full English/French support with localized formatting for dates, currency, and communication.
- **Secure Authentication:** Role-based access (Client, Broker, Admin) via Auth0 with encrypted storage and audit logs.

### Business Goals
CourtierPro addresses four main operational challenges faced by small Quebec brokerages:
1. Improve client transparency by providing real-time progress tracking.
2. Help brokers manage multiple transactions efficiently from one dashboard.
3. Reduce redundant communication through automated document and stage updates.
4. Coordinate appointments and reminders in one unified calendar.

### Product Vision
> **FOR** real estate brokers and administrative staff at small brokerages in Quebec  
> **WHO** need an organized way to manage multiple clients, transactions, and appointments while maintaining transparent communication  
> **THE CourtierPro web platform**  
> **IS A** bilingual broker–client management and communication system  
> **THAT** centralizes transaction tracking, document handling, appointment scheduling, and workflow analytics in one unified environment  
> **UNLIKE** fragmented spreadsheets, emails, and manual scheduling tools  
> **OUR PRODUCT** offers a single, secure, and intuitive space where brokers and clients can collaborate, track progress, and stay aligned through timely updates and reminders.

### Built With
* [![React][React.js]][React-url]
* [![Spring Boot][Spring-url]][Spring-url]
* [![PostgreSQL][Postgres-url]][Postgres-url]
* [![Auth0][Auth0-url]][Auth0-url]
* [![AWS][AWS-url]][AWS-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

CourtierPro is designed for day-to-day brokerage operations at **Nabizada Courtier Inc.**, allowing brokers, clients, and admins to collaborate transparently through a bilingual and role-based interface.

### 1. User Roles and Access

| Role | Description | Key Actions |
|------|--------------|--------------|
| **Broker** | Licensed real estate professional managing multiple transactions | Create and update transaction stages, review and approve documents, confirm appointments, view analytics |
| **Client** | Buyer or seller working with the brokerage | View transaction progress, upload requested documents, request appointments, receive updates |
| **Admin** | Staff responsible for access control and system setup | Manage user accounts, assign roles, configure language defaults, and maintain system integrity |

All users sign in securely using **Auth0 authentication**, and each sees a personalized dashboard upon login.  
Admins can control permissions and language defaults directly from the “System Settings” section.

---

### 2. Core Workflows

#### 🏠 Broker Dashboard
The broker’s home view provides a **centralized dashboard** summarizing:
- All **active transactions** with stage, client name, and last update date.
- Quick access to pending actions such as document reviews or appointment confirmations.
- **KPI panels** showing workload metrics (e.g., active clients, time-to-close averages).

From here, brokers can drill down into specific transactions to update progress or communicate with clients.

#### 📄 Managing Documents
- Brokers can **request specific documents** from clients (e.g., financing letter, inspection report).
- Clients upload files directly into the system; uploads are stored securely on **Amazon S3**.
- Brokers review each submission and mark it as *Approved* or *Needs Revision*.
- Both parties automatically receive **email notifications** via **Amazon SES** when a change occurs.

This eliminates the confusion of lost emails and ensures all documentation is centralized and auditable.

#### 📅 Scheduling Appointments
Appointments are tightly linked to transactions:
- **Clients** request an appointment by selecting the type (e.g., property visit, signing) and preferred timeslot.  
- **Brokers** review requests and either confirm, reschedule, or decline.
- Once confirmed, reminders are automatically sent to both participants.
- The **calendar view** highlights upcoming and past appointments; the **list view** organizes them by urgency.

#### 🔔 Real-Time Notifications
Every major update, such as document approval, appointment confirmation, or stage progress, is paired with an **automated bilingual email notification**.  
Notifications are templated and localized according to the user’s preferred language, ensuring clarity and professionalism.

#### 📊 Viewing Analytics
Brokers can open the **Analytics** tab to view insights generated from historical activity:
- Active vs. completed transactions
- Average time per transaction stage
- Number of pending client actions
- Document turnaround times
- Client satisfaction indicators (based on event frequency and timing)

These analytics help brokers prioritize daily work and continuously improve service quality.

---

### 3. Bilingual Experience
Every page, label, and notification supports both **English and French**, with an always-available language toggle in the top-right corner.  
System dates, times, and currency values follow **Quebec conventions**, ensuring professional accuracy for financial and legal communication.

---

### 4. Security and Compliance
CourtierPro is built on a **secure four-tier architecture** (Presentation, API Gateway, Service, and Data Layers).  
All actions, such as uploads, stage updates, and login attempts, are logged for auditing.  
Sensitive data is encrypted in transit and at rest to meet industry best practices for digital record-keeping in Quebec’s real estate sector.

---

### 5. Example Workflow

1. **Client Login:** The client logs in via Auth0 and sees their transaction dashboard.  
2. **Document Submission:** They upload the requested financing letter.  
3. **Broker Review:** The broker reviews the submission, approves it, and marks the transaction stage as “Financing Approved.”  
4. **Notification Triggered:** CourtierPro automatically sends the client a bilingual email confirmation.  
5. **Appointment Scheduled:** The broker confirms a property visit; both users see it added to their calendar.  
6. **Analytics Updated:** The broker’s KPI dashboard reflects the new transaction progress.

This end-to-end flow demonstrates how the system maintains transparency, traceability, and efficiency across all business activities.

---

_For more screenshots, see the Figma interface and milestone documentation included with the project._

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- ROADMAP -->
## Roadmap

The development roadmap follows Champlain College’s **External Client Project (ECP)** structure:

### 🧭 Final Project 1 (Fall 2025)
- **Group Formation & Project Proposal**
- **Milestone 1** – Problem analysis, requirements gathering, and Figma prototypes  
- **Milestone 2** – High-level design, C4 & UML diagrams, MVP definition  
- **Sprint 01 (Dec 09–Dec 15)** – Environment setup and one end-to-end use case implemented  

### 🚀 Final Project 2 (Winter 2026)
- **Sprint 02 (Jan 13–Jan 19)** – Implementation of additional user stories  
- **Sprint 03 (Jan 20–Feb 02)** – Full CI/CD setup, authentication, and authorization  
- **Sprint 04 (Feb 03–Feb 16)** – Continued implementation and testing  
- **Sprint 05 (Feb 17–Feb 24)** – Final refinements and documentation  
- **Sprint 06 (Deployment)** – Creation of user manual, deployment documentation, and final system presentation  

🗓 **Final Presentation:** February 25, 2026

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- CONTACT -->
## Contact

**CourtierPro Developers**  

| Name | LinkedIn |
|------|-----------|
| **Shawn Nabizada** | [![LinkedIn][linkedin-shield]](https://linkedin.com/in/shawn-nabizada) |
| **Amir Ghadimi** | [![LinkedIn][linkedin-shield]](https://linkedin.com/in/amir-ghadimi04) |
| **Olivier Goudreault** | [![LinkedIn][linkedin-shield]](https://linkedin.com/in/olivier-goudreault-09120a386) |
| **Isaac Nachate** | [![LinkedIn][linkedin-shield]](https://linkedin.com/in/isaac-nachate-b59229343) |

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- MARKDOWN LINKS & IMAGES -->
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/shawn-nabizada

[React.js]: https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[React-url]: https://react.dev/
[Next.js]: https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white
[Next-url]: https://nextjs.org/
[Spring-url]: https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[Postgres-url]: https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white
[Auth0-url]: https://img.shields.io/badge/Auth0-EB5424?style=for-the-badge&logo=auth0&logoColor=white
[AWS-url]: https://img.shields.io/badge/AWS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white
