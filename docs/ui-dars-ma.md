# UI Foundation: Dars.ma
**UX Reference**: docs/ux-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: Angular Material (component library) + a minimal custom token layer on top
- **Rationale (YAGNI)**: Trilingual with mandatory Arabic RTL support is the hardest UI requirement here — Angular Material has first-class `dir="rtl"` support and integrates natively with Angular's CDK bidi module, avoiding hand-rolled RTL CSS across a full custom design system. No brand identity work has been commissioned, so a full custom system isn't justified.

## 2. Design Tokens (theme overrides on top of Angular Material)
```css
/* Colors — semantic, WCAG AA checked */
--color-primary:     #0F6B5C;   /* deep teal — trust, education */
--color-primary-dark:#0B4F44;
--color-secondary:   #D98E04;   /* warm amber — CTA accents, ratings/stars */
--color-background:  #FFFFFF;   /* dark: #0F0F0F */
--color-surface:      #F5F5F5;  /* dark: #1A1A1A */
--color-text:         #111111; /* dark: #F0F0F0 */
--color-text-muted:   #666666; /* dark: #999999 */
--color-border:       #E0E0E0; /* dark: #333333 */
--color-success:      #16A34A; /* escrow released / verified */
--color-warning:      #F59E0B; /* pending verification / awaiting confirmation */
--color-error:        #DC2626; /* rejected / disputed */
--color-info:         #2563EB;

/* Typography */
--font-family: 'Inter', 'Noto Sans Arabic', system-ui, sans-serif; /* Noto Sans Arabic covers AR script cleanly */
--font-size-sm:  0.875rem;
--font-size-md:  1rem;
--font-size-lg:  1.25rem;
--font-size-xl:  1.5rem;

/* Spacing (4px base grid) */
--spacing-xs: 4px;  --spacing-sm: 8px;
--spacing-md: 16px; --spacing-lg: 24px;
--spacing-xl: 32px;
```

## 3. Component Inventory
| Component | Reuse Existing | Build New | Notes |
|---|---|---|---|
| Button | Angular Material `mat-button` | No | Primary=filled teal, Secondary=outlined, Danger=red filled (dispute/reject actions) |
| Form fields | Angular Material `mat-form-field` | No | Labels always visible per UX spec |
| Card (tutor/gig listing) | Angular Material `mat-card` | No | Custom content layout: avatar/name → subject tags → rating → CTA |
| Booking status stepper | Angular Material `mat-stepper` (non-linear, read-only) | No | Maps directly to booking status enum from DB schema |
| Verification badge | — | Yes | Small pill: Pending (amber) / Verified (green, check icon) / Rejected (red) |
| Rating stars | Angular Material or lightweight custom | Build New (thin wrapper) | 1-5 stars, read-only display + input mode |
| Message bubble | — | Yes | Simple sent/received bubble, RTL-aware alignment (flip side in `dir="rtl"`) |
| Language switcher | — | Yes | Simple dropdown (FR/AR/EN) in header, persists to `preferred_lang` |
| Data table (admin queues) | Angular Material `mat-table` | No | Verification queue, disputes list |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single column, bottom-anchored primary CTA (thumb zone), nav collapses to hamburger/bottom bar |
| Tablet | 768–1024px | 2-column card grids (tutor/gig browse), stepper stays horizontal |
| Desktop | > 1024px | 3-column card grids, persistent side nav for Student/Tutor/Admin areas |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 body text, 3:1 large text) — verified for the token palette above
- Focus indicators: visible on all interactive elements (Angular Material defaults retained, not overridden)
- Semantic HTML first; ARIA only where native semantics are insufficient (e.g., stepper state announcements)
- RTL: `dir="rtl"` applied at document root when `preferred_lang = ar`; Angular CDK `Directionality` service drives layout mirroring — verified per-component, not assumed automatic
