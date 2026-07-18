# COMMUNICATIONS — Dars.ma



## 2026-07-04
User preferences: Java + Angular (latest LTS) stack, wants full sprint backlog generated, strict adherence to team-skill framework per task, git push at end of every sprint, NO video recording until the final sprint (overrides default "every version" rule), Docker-only deployment, Kubernetes only introduced if a real need arises (YAGNI).

## UNDERSTAND — Sprint 8 (2026-07-18, session 8)
Scope: Epic 7 (i18n Polish, Hardening & Launch) - stories 7.1 (translation/RTL pass), 7.2 (security hardening confirmation pass), 7.4 (E2E + video + prod Docker deploy). Story 7.3 (CMI live) confirmed skipped again - no real CMI sandbox credentials available (3rd session this's been asked, consistent with sessions 1/7).
User confirmed: (1) skip 7.3, mock mode ships to v1, live-mode is a fast-follow once credentials exist; (2) "production Docker Compose, deployed and reachable" for 7.4 means a local docker-compose.prod.yml run in prod mode (prod Spring profile, COOKIE_SECURE=true, Nginx-served Angular build) demonstrated on localhost - no real remote host/domain/TLS in scope for v1.
