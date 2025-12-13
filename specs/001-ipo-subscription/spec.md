# Feature Specification: TradeSystem IPO Simulation

**Feature Branch**: `1-ipo-subscription`  
**Created**: 2025-12-13  
**Status**: Draft  
**Input**: User description: "/speckit.specify Based on the attached UML diagrams and the requirements below, define the specifications for the \"TradeSystem\"..."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Publish IPO Window (Priority: P1)

As an administrator, I publish a new IPO by defining stock name, symbol, price, total quantity, deadline, and issuer.

**Why this priority**: Without IPO listings nothing else functions; every other flow depends on accurate IPO metadata.

**Independent Test**: Mock admin inputs a listing, saves it, and investors immediately see it in the IPO list with configured attributes.

**Acceptance Scenarios**:

1. **Given** an authenticated admin, **When** they enter valid IPO details, **Then** the IPO is saved with `drawExecuted=false` and is exposed to investors until the deadline.
2. **Given** conflicting deadlines or missing fields, **When** the admin submits, **Then** the system blocks publication and shows the validation errors.

---

### User Story 2 - Concurrent Investor Applications (Priority: P1)

Multiple investors simultaneously apply for the same IPO before the deadline, each locking funds immediately while the system prevents duplicates.

**Why this priority**: IPO subscription volume spikes at deadline; concurrency safety is non-negotiable to avoid overselling or incorrect balances.

**Independent Test**: Launch a concurrency simulation where 100+ investor threads submit `applyForIpo` at the same second; verify balances decrement once, application state is `PENDING`, and duplicates are rejected with `FAILED_DUPLICATE`.

**Acceptance Scenarios**:

1. **Given** an investor who has not applied and has sufficient balance, **When** they submit before the deadline, **Then** the system deducts funds atomically and stores an `IPORecord` with status `PENDING`.
2. **Given** an investor who already has a record for the IPO, **When** they attempt to apply again (even concurrently), **Then** the system denies it and logs `FAILED_DUPLICATE` without changing balance.

---

### User Story 3 - Execute Draw & Publish Results (Priority: P2)

After the deadline, an administrator executes the draw to randomly assign available quantity and investors view their results.

**Why this priority**: Determining final allocation is the project goal; results must be transparent and auditable.

**Independent Test**: Admin triggers draw; system shuffles all `PENDING` records, marks `WON` until quantity exhausted, `LOST` otherwise, and (if configured) refunds `LOST` balances.

**Acceptance Scenarios**:

1. **Given** the IPO deadline has passed and quantity is 100 lots, **When** the admin executes draw, **Then** exactly 100 `PENDING` records become `WON`, remaining records become `LOST` and optionally refunded.
2. **Given** draw already executed, **When** the admin attempts another draw, **Then** the system blocks it and surfaces the prior results.

---

### Edge Cases

- Deadline reached while investors still submitting; late requests must be rejected with `FAILED_FUNDS` untouched.
- Investor balance becomes insufficient between validation and deduction; system must re-check atomically and log `FAILED_FUNDS`.
- Network retries causing duplicate submissions; dedup by investorId/stockId key.
- Admin attempts draw before any `PENDING` records exist; system should warn and require confirmation.
- Optional refund logic toggled off; ensure `LOST` accounts remain deducted without double refunds.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Admins MUST be able to create IPOStock entries with stock name, symbol, price, total quantity, deadline, issuer, and `drawExecuted` flag.
- **FR-002**: The system MUST expose only open IPOs (deadline in future, `drawExecuted=false`) to investors.
- **FR-003**: Investors MUST authenticate before applying and the system MUST load Investor profile and balance.
- **FR-004**: When applying, the system MUST validate deadline, duplicate submissions, and balance before creating an `IPORecord`.
- **FR-005**: The application flow MUST deduct the IPO price * quantity atomically using thread-safe structures so multiple investors can apply at the exact same time without race conditions or overselling.
- **FR-006**: Failed validations MUST log `FAILED_FUNDS` or `FAILED_DUPLICATE` status on the corresponding `IPORecord`, leaving balance untouched.
- **FR-007**: Administrators MUST be able to execute the draw after deadline, using `Collections.shuffle` to randomize the `PENDING` list and updating statuses to `WON` or `LOST` until quantity exhausted.
- **FR-008**: (Optional configuration) The system MUST support refunding locked funds to `LOST` investors during draw execution.
- **FR-009**: Investors MUST be able to view their application history, including status transitions and timestamps, immediately after draw execution.
- **FR-010**: The system MUST store audit logs for every application attempt, including investorId, stockId, timestamp, status, and reason codes.

### Key Entities *(include if feature involves data)*

- **Investor**: Represents a user investing in IPOs; attributes include `userId`, `displayName`, `balance`, immutable apply history; methods to add/deduct balance thread-safely.
- **IPOStock**: IPO listing with `stockId`, descriptive metadata, `price`, `totalQuantity`, `deadline`, flags `isExpired`, `isOpen`, `drawExecuted`.
- **IPORecord**: Application record linking `investorId` and `stockId`, capturing `applyTime`, `quantity`, and status transitions (`PENDING`, `WON`, `LOST`, `FAILED_FUNDS`, `FAILED_DUPLICATE`).
- **Administrator**: System actor capable of publishing IPOStock entries and executing draws; owns audit responsibilities.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: IPO publication takes under 1 minute for admins, with 100% of required fields validated before saving.
- **SC-002**: The system supports at least 200 concurrent investor apply requests hitting the same IPO without creating inconsistent balances or over-allocations.
- **SC-003**: 99% of investor applications receive definitive status (PENDING/FAILED) within 1 second of submission.
- **SC-004**: Draw execution completes within 5 seconds for up to 10,000 `PENDING` records and produces an auditable allocation report.
- **SC-005**: 95% of investors can retrieve their IPO results page without manual support, demonstrating clarity of statuses and refund outcomes.
