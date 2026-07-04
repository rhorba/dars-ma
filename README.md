# Dars.ma — Tutoring Micro-Marketplace

Morocco has 800,000 university students, a massive private tutoring economy, and zero verified platform.

## Problem
Students find tutors through word-of-mouth. Tutors have no portfolio or rating system. No escrow or payment safety.

## Solution
Two-sided micro-gig marketplace for academic tutoring — same architecture as Mahara but education-focused. Skill verification, escrow payments, pgvector matching.

## Stack
Java 25 (LTS) + Spring Boot 3.x + Maven, Angular (latest LTS) + Angular Material, PostgreSQL 16 + pgvector, JWT auth, CMI escrow, Docker Compose (Kubernetes deferred until a real scaling need appears)

## Completes
Mahara (micro-gigs for youth → tutoring specifically)

## Key Roles
Student | Tutor | Admin

## Docs
Full foundation documentation (PRD, system design, architecture, security, database, UX, UI, test strategy, DevOps, sprint backlog) lives in `docs/`.
