# UX Foundation: Dars.ma
**PRD Reference**: docs/prd-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UX Designer

## 1. User Personas (minimal — YAGNI: primary + one edge case)

## Persona: Yasmine (Student, primary)
**Who**: 2nd-year engineering student in Rabat
**Goal**: Find a trustworthy tutor for a hard course before the exam, pay safely, not get ghosted
**Frustration**: Word-of-mouth tutors are unreliable; no way to check if someone is actually good before paying
**Context**: Mobile browser mostly, between classes, moderate tech-savviness, French-primary but sometimes Arabic
**Quote**: "I just want to know this person actually knows the subject before I hand over money."

## Persona: Karim (Tutor, edge case)
**Who**: Grad student tutoring part-time to pay rent, juggles multiple students
**Goal**: Get matched with relevant requests fast, get paid reliably without chasing students
**Frustration**: Doing free "trial" sessions that never convert to paid work; no way to prove credentials
**Context**: Desktop + mobile, checks the app between his own classes, wants low-friction booking management
**Quote**: "I've been burned before — if the money isn't guaranteed, I'm not doing the session."

## 2. Information Architecture / Site Map
```
[Dars.ma]
├── Public
│   ├── Landing
│   ├── Browse Tutors (search/filter)
│   ├── Login / Register (role choice: Student | Tutor)
├── Student Area
│   ├── My Gig Requests (list, create, edit)
│   ├── Gig Detail → Matches → Tutor Profile
│   ├── My Bookings (list, detail: status/escrow/complete/review)
│   └── Messages (per booking thread)
├── Tutor Area
│   ├── My Profile (edit, verification status/submit docs)
│   ├── Suggested Gigs (matches for me)
│   ├── My Bookings (list, detail: status/escrow/complete/review)
│   └── Messages (per booking thread)
├── Admin Area
│   ├── Verification Queue (approve/reject)
│   ├── Disputes
│   └── Analytics Dashboard
└── Account
    ├── Profile settings
    ├── Language (FR/AR/EN)
    └── Notifications
```
Navigation: 5 top-level items per role (Home/Requests-or-Gigs/Bookings/Messages/Account) — within the ≤7-item, ≤3-level rule.

## 3. Core User Flows (top 3 journeys)

### Flow 1: Student posts a gig and gets matched
```
(Login as Student) → [Create Gig Request form] → <Valid input?>
  No → [Inline validation errors] → back to form
  Yes → [Gig published, status OPEN] → [Matching runs (pgvector)] → [Match list shown]
      → [View tutor profile] → [Start booking] → (End: proceeds to Flow 2)

Edge cases:
- No matches found (thin tutor pool) → [Empty state: "No matches yet — we'll notify you" + fallback to keyword browse]
- Student edits gig after matches exist → [Re-run matching, old suggestions marked stale]
```

### Flow 2: Booking → Escrow → Completion → Review
```
(From matched tutor) → [Review terms: price, tutor] → [Confirm Booking] → [Redirect to CMI payment]
  → <Payment success?>
     No  → [Payment failed screen] → [Retry] → back to CMI
     Yes → [Escrow held — Booking status ESCROW_HELD] → [Session happens off-platform]
        → [Both parties: "Mark session complete" button]
        → <Both confirmed?>
             No (only one so far) → [Waiting on other party state]
             Yes → [Escrow released to tutor] → [Rate & review prompt] → (End)

Edge cases:
- Only one party confirms after N days → [Reminder notification]; admin can intervene → dispute path
- Dispute raised by either party → [Booking status DISPUTED] → [Admin reviews] → [Escrow released or refunded by admin]
```

### Flow 3: Tutor profile creation + verification
```
(Register as Tutor) → [Basic profile form: subjects, rate, bio] → [Submit]
  → [Prompt: "Verify your skills to appear in matches"] → [Upload diploma/certificate]
  → [Status: PENDING] → (Admin reviews asynchronously)
  → <Approved?>
      Yes → [Status: VERIFIED, notification sent] → [Profile now eligible for matching]
      No  → [Status: REJECTED, reason shown, notification sent] → [Resubmit option]

Edge cases:
- Tutor tries to accept a gig while still PENDING → [Blocked with explanation: "Verification required to accept bookings"]
- Upload fails (wrong file type/size) → [Inline error, retry]
```

## 4. Key Screen Wireframes (text-based)

### Screen: Create Gig Request (Student)
```
┌─────────────────────────────────────┐
│ ← Dars.ma          [FR|AR|EN] [Yasmine ▾] │
├─────────────────────────────────────┤
│ Subject: [ dropdown/search ]         │
│ Level:   [ dropdown ]                │
│ Description: [ textarea ]            │
│ Budget:  [ min ] - [ max ] MAD       │
│ Availability: [ date/time picker ]   │
│                                       │
│         [ Post Gig Request ]         │
├─────────────────────────────────────┤
│ footer                               │
└─────────────────────────────────────┘
```

### Screen: Booking Detail (both roles)
```
┌─────────────────────────────────────┐
│ Booking #1234           Status: ●●●○ │  ← stepper: Payment/Escrow/Session/Done
├─────────────────────────────────────┤
│ Tutor: Karim B.   Price: 150 MAD/hr  │
│ Escrow: HELD (since Jul 2)           │
│                                       │
│ [ Mark session complete ]             │
│ [ Message tutor/student ]             │
│ [ Report an issue ]                   │
├─────────────────────────────────────┤
│ footer                               │
└─────────────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| Match list | "No matches yet — we'll notify you when a tutor fits" + browse-all link | Skeleton cards ×3 | "Couldn't load matches, retry" | List of tutor cards w/ score |
| Booking detail | N/A (always has data once created) | Skeleton stepper | "Couldn't load booking, retry" | Stepper + actions per current status |
| Verification queue (Admin) | "No pending verifications" | Skeleton rows | "Couldn't load queue, retry" | Table w/ Approve/Reject actions |
| Messages | "No messages yet — say hello" | Skeleton bubbles | "Couldn't load messages, retry" | Thread with sent/received bubbles |
