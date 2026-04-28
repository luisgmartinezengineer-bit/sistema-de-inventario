# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
mvnw.cmd spring-boot:run              # Run the application (Windows)
mvnw.cmd test                         # Run all tests
mvnw.cmd test -Dtest=ClassName        # Run a single test class
mvnw.cmd clean package                # Build executable JAR
mvnw.cmd clean compile                # Compile only (fast check)
```

On Linux/Mac use `./mvnw` instead of `mvnw.cmd`.

## Architecture

Spring Boot 4.0.5 REST API (Java 17) — inventory management system with sales, stock control, and low-stock alerting.

**Stack**: Spring MVC (REST) · Spring Data JPA · H2 in-memory DB · Lombok · Jakarta Validation

**Base package**: `com.Luis.task_manager`

### Domain Model

| Entity | Description |
|--------|-------------|
| `Category` | Product groupings |
| `Product` | Items with price, stock, `minStock` threshold |
| `Sale` + `SaleItem` | A sale header with line items; creating a sale deducts stock automatically |
| `StockMovement` | Audit trail of every stock change (entry, exit, adjustment) |
| `StockAlert` | Auto-created when `product.stock <= product.minStock`; auto-resolved when stock recovers |

### Key Business Rules

- **Low-stock alerts** are checked automatically after every sale and every manual stock adjustment via `StockAlertService.checkAndAlert()`.
- **Sales are atomic**: if any item lacks sufficient stock, the whole sale is rejected with `InsufficientStockException` (HTTP 409).
- `Product.active = false` is used for soft-delete; deactivated products are excluded from all list queries.

### REST API Surface

```
GET/POST        /api/categories
GET/PUT/DELETE  /api/categories/{id}

GET/POST        /api/products           (?search=, ?categoryId=)
GET             /api/products/low-stock
GET/PUT/DELETE  /api/products/{id}
PATCH           /api/products/{id}/stock   body: {quantity, type, reason}  type: ENTRY|EXIT|ADJUSTMENT
GET             /api/products/{id}/movements

GET/POST        /api/sales              (?from=&to= ISO datetime)
GET             /api/sales/summary      (today + this month totals)
GET             /api/sales/{id}

GET             /api/alerts             (all)
GET             /api/alerts/active      (unresolved only)
PATCH           /api/alerts/{id}/resolve

GET             /api/movements          (last 50 global)
```

### Layered Structure

- `entity/` — JPA entities (`@Entity`)
- `repository/` — Spring Data JPA interfaces
- `dto/` — Request/response objects; each `*Response` has a static `from(Entity)` factory
- `service/` — Business logic; `StockAlertService` is injected into both `ProductService` and `SaleService`
- `controller/` — Thin REST controllers delegating to services
- `exception/` — `ResourceNotFoundException`, `InsufficientStockException`, `GlobalExceptionHandler`

### H2 Console

Access at `http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:mem:inventorydb` · User: `sa` · Password: *(empty)*

### Lombok

Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` freely. Configured via Maven Compiler Plugin annotation processor.

### Scalability Path

To move to production: replace H2 with PostgreSQL/MySQL by changing `application.properties` datasource settings — no code changes needed. For async low-stock notifications, replace the synchronous `StockAlertService` call with an application event or message broker publish.
