<!--
Sync Impact Report
Version: N/A → 1.0.0
Modified Principles: (initial publication)
Added Sections:
- Core Principles
- System Constraints & Performance Safeguards
- Development Workflow & Quality Gates
- Governance
Removed Sections: None
Templates requiring updates:
⚠ .specify/templates/plan-template.md (plan checklist must mirror governance rules)
⚠ .specify/templates/spec-template.md (spec format must capture concurrency + testing commitments)
⚠ .specify/templates/tasks-template.md (tasks must ensure concurrency simulation + documentation steps)
Follow-up TODOs: None
-->

# TradeSystem OOAD Constitution

## Core Principles

### I. MVC Fidelity & Service-Centric Logic
Controllers SHALL only coordinate requests, translate DTOs, and delegate to services.
Every business invariant (eligibility, pricing, settlement) MUST live inside service
classes. Views remain dumb I/O adapters. Violations must be refactored before merge.

### II. In-Memory Persistence Discipline
Use Java collections as the sole persistence mechanism. Domain repositories may wrap
ConcurrentHashMap or CopyOnWriteArrayList, but no external database, ORM, or file
serialization is permitted. Data reset utilities must exist to keep test runs isolated.

### III. Concurrency Safety by Default
All shared state MUST be stored in thread-safe structures (ConcurrentHashMap,
ConcurrentLinkedQueue, AtomicInteger). Critical updates (investor balance, stock
inventory) must use synchronized sections or compare-and-set loops to prevent
overselling or negative balances. Code reviews reject non-thread-safe additions.

### IV. Verified Testing & Simulation
Each service requires unit tests plus a concurrency simulation that drives multiple
investor applications via ExecutorService to prove thread safety. Build scripts fail if
simulations are missing or flaky. Regression tests capture discovered race conditions.

### V. Documented Simplicity
Favor straightforward algorithms and explicit control flow. Public classes, services,
and concurrency helpers MUST include Javadoc describing purpose, thread guarantees,
and usage examples. Methods exceeding ~30 lines demand justification or refactor.

## System Constraints & Performance Safeguards

- Java 17 (or higher LTS) is the baseline runtime; only JDK libraries are allowed.
- All domain entities (Investor, Portfolio, Order) must be immutable or expose explicit
  synchronization to avoid incidental sharing.
- Service methods must be idempotent wherever possible so retries from controllers or
  concurrency harnesses do not corrupt state.
- Logging should include investor id, thread id, and order id to support race diagnosis.
- Configuration lives in in-memory constants; no externalized secrets or files.

## Development Workflow & Quality Gates

1. Requirements convert into sequence diagrams and service contracts before coding.
2. Implement services first, then controllers, then simple views/CLIs.
3. Add unit tests and the concurrency simulation (ExecutorService or virtual threads)
   before submitting a pull request.
4. Code review checklist: MVC adherence, thread safety, collection usage, test coverage,
   and Javadoc completeness.
5. CI pipeline runs formatting, unit tests, integration tests (if any), and concurrency
   simulations; merges blocked on any failure.

## Governance

- This constitution supersedes informal guidelines. Exceptions require written approval
  from the project mentor and a follow-up amendment.
- Amendments demand consensus from the student team plus mentor sign-off, a migration/
  remediation plan, and an updated version tag in this file.
- Versioning follows MAJOR.MINOR.PATCH as described in the workflow instructions.
- Compliance reviews occur at every sprint demo; non-compliance results in rework tasks
  that take priority over feature development.

**Version**: 1.0.0 | **Ratified**: 2025-12-13 | **Last Amended**: 2025-12-13
