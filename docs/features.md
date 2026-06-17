# POS + KDS Project Features

This document outlines the core features of the Point of Sale (POS) and Kitchen Display System (KDS) application. The system focuses on a streamlined, automated workflow between the Cashier and the Chef without needing a manual Manager role.

---

## 🔐 1. Authentication & Security Engine
*Status: Active / Core Implemented*

A secure, stateless session authentication system built with Spring Security and OAuth2 Resource Server.

> [!WARNING]
> **Testing Mode:** Token durations (such as the 2-minute refresh token expiry) are currently extendable and kept short for the purpose of testing.

* **Stateless JWT Access Tokens:** 2-minute (configurable to 15-minute) JSON Web Tokens carrying client claims (`userId`, `role`) for fast authorization.
* **Stateful Refresh Tokens:** Cryptographically secure UUID tokens stored in the database to manage active sessions.
* **Early Refresh Protection:** Prevents client/frontend bugs from spamming token refreshes by enforcing a 30-second cooldown timer.
* **Automatic Expiration Cleanup:** Automatically cleans up expired sessions on subsequent token generation.
* **Custom JSON Error Filters:** Custom `AuthenticationEntryPoint` intercepting all Spring Security failures to return clean, structured JSON errors to the client instead of HTML error pages.

---

## 📦 2. Database & Entity Model
*Status: Active / Core Implemented*

A relational schema modeled in PostgreSQL using Hibernate/JPA, optimized for historical sales auditing and data integrity.

* **Single-Device Enforcement:** Bi-directional `@OneToOne` mapping between `UserEntity` and `TokenEntity` restricts each account to a single active session.
* **Price Snapshot Auditing:** `OrderItemEntity` stores a price snapshot (`unitPrice`) at the exact time of purchase to protect historical sales reports if menu prices change.
* **Automatic JPA Auditing:** `MenuEntity` and `OrderEntity` utilize `@EnableJpaAuditing` to automatically track creation and update timestamps without manual database writes.
* **Cascading Protections:** Cascading is restricted to parent-to-child paths (e.g. deleting an Order deletes its items) while preventing child-to-parent deletion (deleting an Order/Item never deletes a User or Menu Item).

---

## 💵 3. Cashier Flow (POS)
*Status: Active / Implemented*

Endpoints mapped to `/api/orders/**` allowing cashiers to handle customer checkouts and manage tickets.

* **Submit Customer Order:** Creates a new `OrderEntity` with multiple `OrderItemEntity` items, fetching current prices from the menu and capturing snapshots. Newly created orders automatically start with a `waiting` status.
* **Edit/Cancel Waiting Orders:** Cashiers can modify the order items or cancel (set status to `cancel`) the order **only while it is in the `waiting` state**.
* **Transition Blockers:** Cashiers are not allowed to update items or status once the order moves to the `cooking` state.
* **Calculate Order Total:** Automatically sums the snapshot prices of all items.
* **View Active Tickets:** Allows cashiers to track the status of placed orders.

---

## 🍳 4. Chef Flow (KDS)
*Status: Active / Implemented*

Endpoints mapped to `/api/orders/**` enabling kitchen display terminals to display real-time cooking tickets.

* **Receive Waiting Orders:** The Chef receives orders immediately with the `waiting` status as soon as the Cashier creates them.
* **Transition to Cooking:** The Chef accepts a waiting order and updates its status to `cooking`.
* **Transition to Complete:** Once the order is prepared, the Chef updates the status from `cooking` to `complete`.
* **No Cancellation:** Chefs do not have permission to cancel orders; only Cashiers are authorized to cancel them.
* **Finalized (Dead) States:** Once an order is marked `complete` or `cancel`, it becomes finalized and immutable. No further updates to items or status transitions are allowed.

---

## 🔄 5. Real-Time Synchronization Engine
*Status: Active / Implemented*

A lightweight, high-performance real-time synchronization engine using Spring Boot asynchronous processing.

* **Non-Blocking Long Polling:** Utilizes Spring's `DeferredResult` to hold client update requests open without blocking Tomcat servlet threads.
* **Bidirectional Global Updates:** Instantly syncs the cashier's order tracker when a chef changes status (`cooking`, `complete`), and updates the kitchen display immediately when a cashier creates or modifies a ticket.
* **In-Memory Version Cache:** Uses a high-performance, sequence-based in-memory cache to prevent "reconnect-gap" timeouts and avoid database overhead.
* **Menu Availability Sync:** Instantly updates the cashier's active ordering menu when the chef toggles a menu item's availability (`isAvailable = false/true`) in the kitchen.

---

## ⚙️ 6. Admin Panel
*Status: Not Planned Yet (Future Backlog)*

Management endpoints mapped to `/api/admin/**` for restaurant owners/administrators.

* **Pre-seeded Account Provisioning:** Manage account credentials for cashiers and chefs (registration is disabled to secure terminal logins).
* **Menu Editor:** Add new dishes, change active pricing, and edit category headers.
