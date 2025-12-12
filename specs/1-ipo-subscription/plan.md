# Technical Implementation Plan: TradeSystem IPO Simulation

## Technical Context

- **Runtime / Framework**: Java 17+, Spring Boot 3.x (web starter), Thymeleaf templates for server-rendered UI.  
- **Storage**: Pure in-memory `DataRepository` backed by `ConcurrentHashMap<String, Investor>`, `ConcurrentHashMap<String, IPOStock>`, and a thread-safe collection (`CopyOnWriteArrayList<IPORecord>`).  
- **ID Strategy**: `AtomicInteger` generators (per entity type) produce deterministic identifiers without collisions.  
- **Actors**: Administrators (publish IPO, execute draw) and Investors (apply, view results).  
- **Concurrency Hotspot**: `IPOService.applyForIPO` must handle dozens of simultaneous submissions for the same IPO.  
- **Testing**: JUnit 5 + a simulation test that spawns ≥50 threads against a limited-quantity IPO.

## Constitution Alignment

1. **MVC Fidelity & Service Logic**: Controllers only marshal requests; `IPOService`, `AdminService`, and `InvestorService` own business rules.  
2. **In-Memory Persistence Discipline**: `DataRepository` is the single source of truth; no external DBs, ORMs, or file I/O.  
3. **Concurrency Safety by Default**: Shared maps + lists are thread-safe; application and draw flows use `synchronized` blocks or `ReentrantLock`.  
4. **Verified Testing & Simulation**: JUnit suites plus an explicit concurrency simulation test ensure no overselling or balance corruption.  
5. **Documented Simplicity**: Services and helpers will include Javadoc; methods stay focused with clear responsibilities.

## Solution Outline

### Architecture & Modules

- **DataRepository**: Spring `@Component` encapsulating in-memory collections and ID generators. Provides CRUD-style helpers (fetch stock, upsert investor, append record).  
- **Services**:
  - `AdminService`: publish IPOs (validating inputs), trigger draws, generate allocation summary, optional refund logic.  
  - `IPOService`: investor-facing operations (list stocks, apply, view history). Responsible for balance locking, duplicate detection, and status transitions.  
  - `InvestorService`: manages investor onboarding/top-ups; assists IPOService with balance mutations.  
- **Controllers**:
  - `AdminController` (Thymeleaf forms for publish/draw).  
  - `IPOController` (list IPOs, handle apply POST, render investor results).  
- **Views**:
  - `ipo-list.html`, `ipo-apply.html`, `admin-publish.html`, `admin-draw.html`, `results.html`. Use simple CSS + Thymeleaf conditionals.

### Data Model & Validation

- `Investor`: immutable identifiers, `BigDecimal balance`, apply history reference; balance mutation methods synchronized internally.  
- `IPOStock`: metadata plus `deadline`, `totalQuantity`, `drawExecuted` flag, derived helpers `isOpen()` / `isExpired()`.  
- `IPORecord`: `recordId`, `investorId`, `stockId`, `quantity`, `applyTime`, `Status`. Includes transition helpers `markWon`, `markLost`, `markFailed(reason)`.

### Concurrency & Consistency

- `IPOService.applyForIPO(...)` flow:  
  1. Fetch investor + stock from `DataRepository`.  
  2. Validate deadline + duplicate using concurrent maps (`computeIfPresent` / `putIfAbsent`).  
  3. Enter synchronized section (or `ReentrantLock`) keyed by `investorId + stockId`. Inside: re-check balance, deduct funds, append `IPORecord` to synchronized list, set status `PENDING`.  
  4. Return DTO summarizing outcome.  
- Draw execution: `AdminService.executeDraw(stockId)` obtains all `PENDING` records, shuffles collections via `Collections.shuffle`, iterates while tracking remaining quantity, updates statuses, and optionally refunds in synchronized balance operations.  
- Logging: every transition logs investorId, stockId, thread, reason to aid concurrency audits.

### Error Handling & UX

- Controllers translate service exceptions to user-friendly Thymeleaf messages (`FAILED_FUNDS`, `FAILED_DUPLICATE`, `DEADLINE_PASSED`).  
- Input validation via Spring `@Valid` DTOs for publish/apply forms.  
- Global exception handler returns informative pages without stack traces.

## Testing Strategy

1. **Unit Tests (JUnit 5)**:  
   - `DataRepositoryTest`: ensures maps/lists behave as expected and ID generators are thread-safe.  
   - `AdminServiceTest`: publish validation, draw edge cases (no pending records, repeated draws).  
   - `IPOServiceTest`: duplicates, insufficient funds, deadline enforcement.  
2. **Simulation Test**: `IPOConcurrencySimulationTest` spawns ≥50 threads sending apply requests against a 20-lot IPO. Assertions ensure:  
   - Total WON ≤ quantity.  
   - No investor has more than one record per stock.  
   - Deducted balances equal locked funds for PENDING/WON investors; LOSERS refunded only if option enabled.  
3. **Integration Smoke Test**: Minimal MockMvc test verifying controller/service wiring and Thymeleaf view rendering.

## Delivery Phases

1. **Phase 0 – Environment & Repository Setup**: Scaffold Spring Boot, configure Thymeleaf, create constitution-compliant package structure, document assumptions.  
2. **Phase 1 – Data Layer**: Implement `DataRepository`, entity models, ID generators, and DTOs; include unit tests.  
3. **Phase 2 – Core Services**: Build `AdminService`, `InvestorService`, `IPOService` with concurrency controls and logging. Add service-level tests.  
4. **Phase 3 – Web Layer**: Implement controllers and Thymeleaf pages, wire validation + messaging. Provide mock data for demo.  
5. **Phase 4 – Draw Logic & Simulation**: Complete draw algorithm, optional refund toggle, and the multi-threaded simulation test; document results.  
6. **Phase 5 – Polish & Documentation**: Add Javadocs, quickstart instructions, and ensure constitution alignment checklist passes.

## Risks & Mitigations

- **Race Conditions During Apply**: mitigate with fine-grained locks per investor/stock combination plus thread-safe structures.  
- **Long Draw Execution for Large Record Sets**: use `CopyOnWriteArrayList` iteration plus streaming; limit to in-memory scale (10k) and document boundaries.  
- **Test Flakiness in Simulation**: seed randomization for reproducibility, provide retry mechanism + deterministic data setup. 
