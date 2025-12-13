# Research Notes: TradeSystem IPO Simulation

## Concurrency Handling
- **Thread Safety**: Use `ConcurrentHashMap` for investors and IPO stocks to allow lock-free reads while permitting safe concurrent writes.
- **Fine-Grained Locking**: For balance deductions and duplicate prevention, apply `ReentrantLock` keyed per `investorId + stockId` combination. This avoids global locks while ensuring each investor can only hold one live application per stock.
- **Collections.shuffle**: Java's built-in shuffle is sufficient for random draw ordering when paired with `SecureRandom` seed injection for reproducibility in tests.

## Validation Strategies
- **IPO Publication**: Validate deadline is in the future, `totalQuantity` > 0, and price is positive BigDecimal. Use Bean Validation annotations (`@Future`, `@Positive`).
- **Apply Flow**: Re-check deadline, existing IPORecord presence, and investor balance inside the synchronized block to prevent TOCTOU issues.
- **Draw Execution**: Require `drawExecuted=false` and `deadline` <= now; guard with atomic flag flips to avoid duplicate draws.

## Simulation & Testing Insights
- Multi-threaded tests can use `ExecutorService` with `CountDownLatch` to coordinate simultaneous submissions.
- For deterministic verifications, capture each thread's outcome and assert invariants post-execution (total WON == quantity, no duplicate investor-stock pairs).

## UX & Feedback
- Thymeleaf templates should surface statuses with color tags: `PENDING` (amber), `WON` (green), `LOST` (gray), `FAILED_*` (red).
- Admin draw page needs a dry-run summary (counts per status) before final execution to reduce operator errors.

## Limitations & Assumptions
- In-memory storage only; all data resets on application restart.
- Authentication mocked; assume admin/investor context injected via test harness or simple session attribute.
- Refund behavior toggled by configuration property `ipo.refundOnLoss=true|false`.
