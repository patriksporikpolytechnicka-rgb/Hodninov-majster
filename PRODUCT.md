# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Customers:** people who need someone to do a job for them (assembly, repairs, garden, moving, IT help) and want to find and order a local provider.
- **Providers:** tradespeople and helpers who offer their services by the hour, per job or per m² and accept orders.

The home page (catalogue) is the page everyone sees first, both customers and providers.

## Product Purpose

Hodinový majster connects customers with local providers. A customer searches the catalogue, opens a service, sends an order with a date; the provider accepts, does the job and marks it complete. Success: a customer finds a fitting provider and places an order in a few steps.

## Positioning

Unlike an ad board (Bazoš) or Facebook groups, all three of these hold at once:

- **Fair and transparent:** prices are shown up front and calculated by the server; providers pay a flat 10 € per month (30 days free) and no commission is taken from jobs.
- **Fast and local:** search by name, city and category, order in a few clicks.
- **Trust and quality:** real provider profiles; reviews after finished jobs are planned.

## Operating Context

School project (KASV), presented to teachers. Runs against a PostgreSQL database on the school server.

## Capabilities and Constraints

- Roles: CUSTOMER, PROVIDER, ADMIN. Public registration only allows CUSTOMER and PROVIDER.
- Catalogue: search by service name and city, filter by category, paging (6 per page).
- Service has a provider, category, name, description, price and price unit (`HOUR`, `JOB`, `SQUARE_METER`).
- Orders: date, note, server-calculated price; statuses PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED.
- Provider subscription: 30-day trial, then 10 €/month; expired providers cannot accept new orders.
- Not built yet: service editor, chat, reviews, real payment gateway, admin pages.
- Language: Slovak first, English translation planned. Currency EUR.

## Brand Commitments

- Name: **Hodinový majster**.

## Evidence on Hand

- Categories in the school database: Domácnosť, Záhrada, IT pomoc, Sťahovanie.
- No real reviews, ratings, customer counts or testimonials exist. Do not fabricate them.
- No photographs or logo assets yet.

## Product Principles

1. Price and who does the job are always visible before ordering.
2. Few steps from search to order.
3. Never show trust signals (ratings, counts, badges) the data does not have.
4. Slovak text must fit and read naturally; leave room for English.
