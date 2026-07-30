---
name: Voyage
description: A journey-first travel banking interface built as a living travel credential.
colors:
  passport-ink: "#17352c"
  passport-ink-soft: "#295045"
  journey-paper: "#f7f3e8"
  clean-sheet: "#fffdf7"
  ruled-line: "#cfd8ce"
  travel-mist: "#e7eee8"
  action-moss: "#0b684d"
  readiness-lime: "#ddeb83"
  restrained-brass: "#a77b35"
  safety-alert: "#a7442d"
typography:
  display:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "40px"
    fontWeight: 700
    lineHeight: 0.94
    letterSpacing: "-0.03em"
  title:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "27px"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.025em"
  body:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "11px"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "0.01em"
rounded:
  control: "10px"
  surface: "14px"
  card: "16px"
  dialog: "20px"
spacing:
  tight: "8px"
  control: "12px"
  content: "22px"
  section: "32px"
components:
  button-primary:
    backgroundColor: "{colors.passport-ink}"
    textColor: "{colors.clean-sheet}"
    rounded: "{rounded.control}"
    padding: "14px 16px"
    height: "44px"
  journey-folio:
    backgroundColor: "{colors.passport-ink}"
    textColor: "{colors.journey-paper}"
    padding: "25px 24px 22px"
---

# Design System: Voyage

## Overview

**Creative North Star: "The Living Travel Credential"**

Voyage combines the material confidence of a modern travel document with the restraint of a private bank. The interface is warm rather than clinical, information-dense without becoming a dashboard, and designed for one-handed use before departure or under pressure abroad.

The current journey is the organizing object. Destination, readiness, money, preferred card, payments, and protection should feel like parts of the same credential rather than unrelated cards.

**Key Characteristics:**

- Destination-led hierarchy
- Warm paper and deep ink surfaces
- Ruled divisions instead of nested containers
- Stamped status marks and restrained brass details
- Clear, calm actions with strong focus states

## Colors

The palette uses travel-document neutrals with deep green authority and rare semantic accents.

### Primary

- **Passport Ink** (#17352c): Primary navigation, journey folio, actions, and high-emphasis text.
- **Action Moss** (#0b684d): Interactive links, contextual labels, and secondary emphasis.

### Secondary

- **Readiness Lime** (#ddeb83): Positive readiness and high-visibility focus.
- **Restrained Brass** (#a77b35): Credential and stamped-status detail only.
- **Safety Alert** (#a7442d): Fraud, destructive actions, and recovery warnings.

### Neutral

- **Journey Paper** (#f7f3e8): App canvas.
- **Clean Sheet** (#fffdf7): Raised content surface.
- **Travel Mist** (#e7eee8): Selected and quiet control states.
- **Ruled Line** (#cfd8ce): Dividers and structural borders.

**The Rare Accent Rule.** Lime, brass, and alert red communicate state; they never become general decoration.

## Typography

**Display Font:** DM Sans (with sans-serif fallback)  
**Body Font:** DM Sans (with sans-serif fallback)

**Character:** A single clean sans-serif family keeps destinations, financial details, and controls direct and highly legible on a phone.

### Hierarchy

- **Display** (700, 40px, .94): Destination and root-screen headings.
- **Headline** (700, 27px, 1): Section and sheet headings.
- **Title** (700, 23px, 1.05): Journey and action titles.
- **Body** (400–600, 14px, 1.5): Guidance, details, and explanations.
- **Label** (700, 11–12px): Short status and financial metadata.

**The Destination Leads Rule.** The largest type names the place or task, never a generic metric.

## Layout

The product is mobile-only. The working canvas is at most 402px wide in desktop preview and fills the viewport on phones. Content uses 22px horizontal insets, 32px section separation, and a fixed bottom navigation. The journey folio deliberately bleeds to the content edges. At widths below 360px, core insets reduce to 16–18px and the destination display size becomes 33px.

## Elevation & Depth

The system is flat by default. Tonal surface changes and ruled borders establish hierarchy. Shadows are reserved for the outer phone preview and protected authentication dialog; operating content does not use decorative elevation.

**The Flat Credential Rule.** Inside the app, prefer ink, paper, and rules over floating cards and ambient shadows.

## Shapes

Content surfaces use a 14px radius, controls use 10px, and protected dialogs use 20px. Circular geometry is reserved for stamped states, icon shortcuts, and close controls. Pills are limited to compact statuses.

## Components

### Buttons

- **Primary:** Passport Ink background, Clean Sheet text, 10px radius, and at least 44px height.
- **Focus:** A 3px Readiness Lime outline with visible offset.
- **Secondary:** Paper background with a ruled border and Passport Ink text.

### Cards / Containers

- **Corner Style:** 14px.
- **Background:** Clean Sheet or a semantic tinted paper.
- **Shadow Strategy:** None at rest.
- **Border:** One Ruled Line stroke.
- **Internal Padding:** 17–18px.

### Inputs / Fields

- **Style:** Clean Sheet fill, one ruled border, 10–12px radius, and generous touch padding.
- **Focus:** Border shifts toward moss with a light green focus field.
- **Error / Disabled:** Alert tint for errors; reduced saturation and opacity for disabled controls.

### Navigation

The bottom navigation uses Today, Trips, Wallet, and Safety. The active tab is a solid Passport Ink field with white icon and label; inactive tabs are quiet gray-green. Targets remain at least 44px.

### Journey Folio

The signature component is a full-width Passport Ink travel credential showing destination, dates, readiness, available budget, and preferred card in one hierarchy. Concentric stamped linework is allowed only here and in closely related credential states.

## Do's and Don'ts

### Do:

- **Do** organize financial information around the current journey.
- **Do** use ruled divisions to group related information.
- **Do** keep emergency and recovery actions explicit and reachable.
- **Do** use native buttons for interactive cards and move focus when dynamic views open.

### Don't:

- **Don't** rebuild the page as a generic metric dashboard.
- **Don't** nest cards or apply shadows to every surface.
- **Don't** use brass, lime, or red as non-semantic decoration.
- **Don't** reduce important financial or safety text below 11px.
