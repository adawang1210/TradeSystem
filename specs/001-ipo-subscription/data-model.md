# Data Model: TradeSystem IPO Simulation

## Investor
| Field | Type | Description |
|-------|------|-------------|
| `investorId` | `String` | Unique identifier assigned via `AtomicInteger` sequence and stored as string for map keys. |
| `displayName` | `String` | Human-readable name shown in IPO lists, history, and allocation results. |
| `email` | `String` | Optional notification channel for confirmations (kept in-memory only). |
| `balance` | `BigDecimal` | Available cash for IPO applications; guarded by synchronized mutation helpers to prevent race conditions. |
| `lockedBalance` | `BigDecimal` | Tracks funds reserved during `PENDING` applications when refunds are deferred. |
| `createdAt` | `Instant` | Audit timestamp for onboarding. |
| `applyHistory` | `List<IPORecord>` reference | Pointer to immutable list view of the investor's IPO applications for fast history rendering. |

### Invariants
- `balance + lockedBalance` never drops below zero.
- `applyHistory` maintains chronological order for traceability.

## IPOStock
| Field | Type | Description |
|-------|------|-------------|
| `stockId` | `String` | Generated identifier, used as key across services and records. |
| `name` | `String` | Public-friendly company name. |
| `symbol` | `String` | Short ticker used in UI and logging (e.g., `TSIM`). |
| `issuer` | `String` | Issuing organization for audit. |
| `price` | `BigDecimal` | Per-lot subscription price charged to investors. |
| `totalQuantity` | `int` | Maximum lots available for allocation. |
| `deadline` | `Instant` | Cutoff for accepting `applyForIPO` requests; investors must submit before this timestamp. |
| `drawExecuted` | `boolean` | Indicates whether allocation draw already ran (prevents duplicates). |
| `createdAt` | `Instant` | Publish time used in admin history table. |
| `updatedAt` | `Instant` | Last modification (e.g., admin editing metadata pre-deadline). |

### Derived Helpers
- `isOpen()` returns `true` when `deadline` is in the future and `drawExecuted` is `false`.
- `isExpired()` flips `true` once deadline passes; used to block late applications.

## IPORecord
| Field | Type | Description |
|-------|------|-------------|
| `recordId` | `String` | Unique per application for auditing. |
| `investorId` | `String` | Foreign key linking back to Investor. |
| `stockId` | `String` | Foreign key linking to IPOStock. |
| `quantity` | `int` | Lots requested by investor; validated against available funds. |
| `applyTime` | `Instant` | Timestamp captured at submission for ordering and deadline enforcement. |
| `status` | `IPOStatus` enum | Possible values: `PENDING`, `WON`, `LOST`, `FAILED_FUNDS`, `FAILED_DUPLICATE`. |
| `statusReason` | `String` | Optional human-readable explanation (e.g., `DEADLINE_PASSED`). |
| `history` | `List<StatusTransition>` | Records each status change with timestamp for audit. |
| `lockedAmount` | `BigDecimal` | Amount reserved from investor balance while status is `PENDING`. |

### Transitions
- `PENDING → WON/LOST` during draw execution. 
- `PENDING → FAILED_*` during validation failures.
- `LOST` may trigger refund to release `lockedAmount` depending on configuration.
