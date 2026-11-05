---
version: 1
slug: "frontend-src-app-tsx"
primary_target: "frontend/src/App.tsx"
related_targets: []
---

# Catalogue (home page)

Scope: the catalogue at `/`, the first page every visitor sees. Mode: Operate (find a service, compare price, open it).
Audience: customers looking for help and providers checking how they appear. Desktop 1440 and mobile 390, designed in Figma first.
Content: example services labelled "Ukážkové dáta" until real services exist; empty state still owed.
Chosen by the user from the decision page: the acetate-manual world (adopted from the declined row).

## Direction contract

THESIS: The catalogue is a tabbed reference manual: each category is a coloured section board, and the services sit on milk acetate leaves above it. Refuses the default marketplace of a search hero plus photo cards.

OWN-WORLD: Milk acetate reading field (#F3F2EE) over full-strength section boards: chrome yellow (all / primary), oxide orange (Domácnosť), grass (Záhrada), ultramarine (IT pomoc), sienna (Sťahovanie); teal and violet reserved for future categories, vermilion only for errors. Condensed grotesk caps for heads, serif body, mono for labels and units. Hairline-ruled leaves, no shadows, no easing.

STORY: The visitor sees all services with price and provider, narrows by category or search, and opens one to order. Providers see how their service is presented.

FIRST VIEWPORT: Top bar with brand, mono facts line (categories, services, 0 % provízia), login and a yellow primary action. Search row (what + city + HĽADAŤ). A 3x3 grid of leaves, each with a category hue tab, title, provider and city, and the price large with its unit. The stepped tab rail runs down the right fore edge: one tab per category, height proportional to its service count; the selected tab extends and tints the section.

FORM: Acetate tab board (rw-manual-acetate-tab-board), adopted challenger, not on the grounded list. seed:adbe71b53407dd02. Signature move: the stepped tab rail as the category filter.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
