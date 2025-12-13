# Implementation Tasks: TradeSystem IPO Simulation

## 1. Project Setup & Models

1. **Bootstrapped Spring Boot project** with Web + Thymeleaf starters, Java 17 toolchain, and constitution-aligned package layout (`controller`, `service`, `repository`, `model`, `view`).
2. **Define domain models** (`Investor`, `IPOStock`, `IPORecord`, `Status` enum) with Javadoc, validation helpers (`isOpen`, `markWon`, etc.), and immutable identifiers.
3. **Create DTOs/Form objects** for publish IPO, apply IPO, and draw execution; configure Bean Validation annotations for required fields.
4. **Configure Thymeleaf layout** scaffolding (shared header/footer, static CSS) to support later pages.

## 2. In-Memory Repository (Thread-Safe Implementation)

5. **Implement `DataRepository` component** containing `ConcurrentHashMap<String, Investor>` and `ConcurrentHashMap<String, IPOStock>` plus a `CopyOnWriteArrayList<IPORecord>` (or `Collections.synchronizedList`).
6. **Add `AtomicInteger` ID generators** for investors, stocks, and records with helper methods (`nextInvestorId()`, etc.).
7. **Provide repository APIs** for fetching/upserting investors/stocks, checking duplicates, retrieving pending records, and resetting data for tests.
8. **Unit-test repository behavior** to ensure atomic ID generation and concurrent-safe read/write access.

## 3. Services (with Synchronized Logic)

9. **Build `InvestorService`** handling balance adjustments (synchronized per investor), history retrieval, and demo data seeding.
10. **Implement `AdminService`** for IPO publication validation, draw execution (using `Collections.shuffle`), optional refunds, and outcome summaries.
11. **Implement `IPOService.applyForIPO`** with deadline + duplicate validation, synchronized/locking block around balance deduction + record creation, and status handling for `FAILED_FUNDS` / `FAILED_DUPLICATE`.
12. **Introduce service-level DTOs/responses** to encapsulate success/error messages for controllers and views.
13. **Write targeted service tests** (JUnit 5 + Mockito) verifying publication rules, draw logic, and concurrent-safe application flow (small-scale).

## 4. Controllers & Views

14. **Create `InvestorController`/`IPOController`** exposing IPO list, application form, and results page; ensure validation + error messaging.
15. **Create `AdminController`** for publish/draw flows, surfacing warnings when drawing prematurely or re-running draws.
16. **Build Thymeleaf templates** (`ipo-list.html`, `ipo-apply.html`, `admin-publish.html`, `admin-draw.html`, `results.html`) highlighting statuses (`PENDING`, `WON`, `LOST`, `FAILED_*`).
17. **Add global exception + success message handling** (e.g., `@ControllerAdvice`, flash attributes) to keep UX simple and traceable.
18. **Smoke-test controllers** with Spring MockMvc to ensure routing and view rendering succeeds.

## 5. Critical Verification Task

19. **Implement `ConcurrencyTest` (JUnit)** that seeds an IPO with limited quantity, spawns ≥50 threads applying simultaneously via `ExecutorService`, and asserts:
    - no investor receives duplicate records;
    - total `WON` ≤ total quantity;
    - balances deducted/refunded match record statuses.
20. **Execute verification command**:
    - Run `./mvnw test` from the project root.
    - If any test fails (especially `ConcurrencyTest`), inspect the stack trace, fix the offending code or test, and rerun `./mvnw test`.
    - Repeat until the entire suite passes; document failures and resolutions in commit notes or README.
