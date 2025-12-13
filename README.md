# IPO Simulation System (High-Concurrency Trade System)

[**Live Demo – Coming Soon**](#)

A Spring Boot 3 / Java 17 demonstration platform that simulates a high-demand Initial Public Offering (IPO) window. The application exposes investor- and admin-facing flows, enforces a strict "one lot per user" policy, and relies on thread-safe in-memory data structures so you can stress-test IPO logic without provisioning an external database.

## Key Features

- **User System (In-Memory Auth):** Minimal registration/login backed by `InvestorService`, bootstrapped with demo investors for quick trials.
- **IPO Application Flow:** Investors browse open offerings and submit a single-lot application per IPO. Duplicate submissions are rejected before and after locking to prevent race conditions.
- **Admin Dashboard:** Admins publish new IPOs, inspect live order books, trigger lottery draws, and optionally refund non-winning bids via `AdminController` + `AdminService`.
- **High-Concurrency Protection:** `IPOService` coordinates per-investor-per-stock mutexes, `DataRepository` uses `ConcurrentHashMap`/`AtomicInteger` for reservations, and the `Investor` entity synchronizes balance deductions to prevent overselling or double spending.
- **Responsive UX/UI:** Thymeleaf templates styled with Bootstrap deliver a simple dashboard-style experience for both investors and admins.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.x (Web, Validation)
- **Frontend:** Thymeleaf templates with Bootstrap components
- **Storage:** In-memory `ConcurrentHashMap` + `CopyOnWriteArrayList` (no external DB required)
- **Build & Packaging:** Maven, Spring Boot Maven Plugin
- **Containerization:** Docker (multi-stage build) & Docker Compose ready

## Getting Started (Local Run)

### Prerequisites

- Java 17+ runtime (Temurin / OpenJDK recommended)
- Maven 3.9+

### Run the app

```bash
# Clone the repository
git clone https://github.com/<your-account>/TradeSystem.git
cd TradeSystem

# Launch with Spring Boot's Maven plugin
mvn spring-boot:run
```

Once the server starts, open `http://localhost:8080` to access the investor portal and `http://localhost:8080/admin` (after login) for admin tooling.

## Docker Deployment

Use the provided multi-stage `Dockerfile` to build a compact image.

```bash
# Build image
docker build -t tradesystem .

# Run container on port 8080
docker run --rm -p 8080:8080 tradesystem
```

For Compose-based deployments, reference the same image tag and expose port 8080 to your preferred host port.

## Architecture Highlights

- **In-Memory, Stateless Core:** `DataRepository` seeds demo investors, IPO listings, and maintains records in collections backed by `ConcurrentHashMap`, `CopyOnWriteArrayList`, and `AtomicInteger`. This keeps the system fast, stateless (per pod/container), and ideal for demo environments without external persistence.
- **Per-Request Synchronization:** `IPOService.apply(...)` builds a composite key (`investorId:stockId`) and synchronizes on a mutex stored in a `ConcurrentHashMap`, ensuring that duplicate submissions and reservation math remain consistent even with 50+ concurrent applicants.
- **Investor-Level Thread Safety:** The `Investor` model exposes synchronized `getBalance`, `addBalance`, and especially `deductBalance` methods so that monetary operations are atomic across threads.
- **Admin-Orchestrated Draws:** `AdminService.executeDraw(...)` shuffles pending applications, marks win/loss states, and optionally refunds losers—mirroring real-world allocation rounds while keeping the code approachable for study.
- **Layered Services:** Controllers (investor + admin) remain thin, delegating business rules to `IPOService`/`AdminService`, which in turn rely on the repository for storage concerns. This separation keeps the codebase testable and extendable.

---

Feel free to fork the project, connect it to a persistent datastore, or plug in real authentication to evolve it into a production-ready allocation engine.
