# DECISIONS — Dars.ma



## DECISION — 2026-07-04
Scope: COMPREHENSIVE (full README vision) — trilingual FR/AR/EN, escrow, pgvector matching, admin moderation, skill verification, notifications, analytics.
Sprint count target: 7-10 sprints, Scrum Master to size backlog against this.

## DECISION — 2026-07-04
Architecture: layered package-by-feature Spring Boot monolith. Payment gateway (CMI) behind EscrowPaymentProvider Strategy interface w/ mock impl. Spring in-process domain events for cross-feature reactions. Angular standalone components, lazy-loaded per feature.

## DECISION — 2026-07-04
Java toolchain gap (host has JDK21 max, no Maven; project targets Java 25): backend build/test runs via Docker containers only (maven:3.9-eclipse-temurin-25), no local JDK25/Maven install. Matches Docker-only project philosophy.
