# Sistema de Gestión de Inventario

API REST desarrollada con Spring Boot para la gestión de inventario, ventas, control de stock y facturación electrónica.

## Tecnologías

- **Java 17** + **Spring Boot 3**
- **Spring Security** — autenticación con sesiones y roles (ADMIN, SUPERVISOR, VENDEDOR)
- **Spring Data JPA** — persistencia con MySQL (desarrollo local con H2)
- **Lombok** — reducción de código repetitivo
- **Jakarta Validation** — validación de entradas
- **Maven** — gestión de dependencias y build

## Requisitos

- Java 17+
- Maven 3.8+ (incluido el wrapper `mvnw`)
- MySQL 8+ (solo para perfil de producción)

## Cómo ejecutar

```bash
# Ejecutar en modo desarrollo (base de datos H2 en memoria)
./mvnw spring-boot:run              # Linux / Mac
mvnw.cmd spring-boot:run            # Windows

# Compilar JAR ejecutable
./mvnw clean package

# Ejecutar tests
./mvnw test
```

La aplicación inicia en `http://localhost:8080`  
Credenciales por defecto: `admin / admin123`

## Consola H2 (desarrollo)

URL: `http://localhost:8080/h2-console`  
JDBC URL: `jdbc:h2:mem:inventorydb`  
Usuario: `sa` | Contraseña: *(vacía)*

## Estructura del proyecto

```
src/main/java/com/Luis/task_manager/
├── controller/       # Controladores REST
├── service/          # Lógica de negocio
├── entity/           # Entidades JPA (modelo de dominio)
├── dto/              # Objetos de transferencia de datos
├── repository/       # Interfaces Spring Data JPA
├── security/         # Configuración de Spring Security
└── exception/        # Excepciones personalizadas y manejador global
```

## Modelo de dominio

| Entidad | Descripción |
|---------|-------------|
| `Category` | Categorías de productos |
| `Product` | Productos con precio, stock y umbral mínimo (`minStock`) |
| `Sale` + `SaleItem` | Cabecera de venta con ítems; crear una venta descuenta el stock automáticamente |
| `StockMovement` | Auditoría de cada movimiento de stock (entrada, salida, ajuste) |
| `StockAlert` | Alerta creada automáticamente cuando `stock <= minStock`; resuelta al recuperarse el stock |
| `Customer` | Clientes asociados a ventas |
| `Supplier` | Proveedores para órdenes de compra |
| `CashRegister` | Cajas registradoras con control de apertura/cierre |
| `CompanyConfig` | Configuración de la empresa (NIT, resolución DIAN, prefijo de factura) |
| `AuditLog` | Registro de auditoría de todas las acciones del sistema |

## API REST

### Categorías
```
GET    /api/categories          Lista todas las categorías
POST   /api/categories          Crea una categoría
GET    /api/categories/{id}     Obtiene una categoría
PUT    /api/categories/{id}     Actualiza una categoría
DELETE /api/categories/{id}     Elimina una categoría
```

### Productos
```
GET    /api/products            Lista productos (filtros: ?search=, ?categoryId=)
POST   /api/products            Crea un producto
GET    /api/products/low-stock  Productos por debajo del stock mínimo
GET    /api/products/{id}       Obtiene un producto
PUT    /api/products/{id}       Actualiza un producto
DELETE /api/products/{id}       Elimina (soft-delete) un producto
PATCH  /api/products/{id}/stock Ajusta el stock  body: {quantity, type, reason}
GET    /api/products/{id}/movements  Historial de movimientos
```

### Ventas
```
GET    /api/sales               Lista ventas (filtros: ?from=&to= ISO datetime)
POST   /api/sales               Registra una venta
GET    /api/sales/summary       Resumen del día y mes actual
GET    /api/sales/{id}          Obtiene una venta
```

### Alertas de stock
```
GET    /api/alerts              Todas las alertas
GET    /api/alerts/active       Solo alertas activas (sin resolver)
PATCH  /api/alerts/{id}/resolve Marca una alerta como resuelta
```

### Movimientos
```
GET    /api/movements           Últimos 50 movimientos globales
```

## Reglas de negocio principales

- Las ventas son **atómicas**: si algún ítem no tiene stock suficiente, toda la venta es rechazada (HTTP 409).
- Las alertas de bajo stock se verifican automáticamente tras cada venta y cada ajuste manual de stock.
- El borrado de productos es **lógico** (`active = false`); los productos inactivos no aparecen en los listados.
- Los números de factura se generan automáticamente con prefijo configurable.

## Configuración para producción

Para conectar a MySQL, modificar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventario
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update
```

No se requieren cambios en el código.
