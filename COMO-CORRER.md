# Cómo levantar el proyecto

## Estructura de carpetas importante

```
C:\proyecto\task-manager\task-manager\
│
├── src\main\resources\
│   ├── application.properties        ← configuración de conexión a MySQL
│   └── db\
│       └── inventario_mysql.sql      ← script para crear la base de datos
│
├── pom.xml                           ← dependencias del proyecto
└── mvnw.cmd                          ← comando para correr el proyecto
```

---

## PASO 1 — Crear la base de datos en XAMPP

1. Abre **XAMPP Control Panel**
2. Inicia **MySQL** (solo MySQL, Apache NO es necesario)
3. Abre el navegador y ve a:
   ```
   http://localhost/phpmyadmin
   ```
4. Haz clic en la pestaña **SQL** (barra superior)
5. Abre este archivo con el Bloc de notas:
   ```
   C:\proyecto\task-manager\task-manager\src\main\resources\db\inventario_mysql.sql
   ```
6. Selecciona todo (Ctrl+A), copia (Ctrl+C)
7. Pega en phpMyAdmin y haz clic en **Continuar**
8. Verás que se crea la base de datos **inventario** con todas sus tablas

---

## PASO 2 — Matar cualquier proceso Java anterior

Abre una terminal (cmd) y ejecuta:

```
for /f "tokens=5" %a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do taskkill /F /PID %a
```

Si dice "no se encontró ningún proceso", está bien — significa que el puerto ya está libre.

---

## PASO 3 — Correr el proyecto

En la terminal, navega a la carpeta del proyecto y arranca Spring Boot:

```
cd C:\proyecto\task-manager\task-manager
mvnw.cmd spring-boot:run
```

Espera hasta que veas esta línea (tarda ~15 segundos):

```
Started TaskManagerApplication in X.XXX seconds
```

---

## PASO 4 — Probar que funciona

Abre el navegador y ve a estas URLs:

| URL | Qué muestra |
|-----|-------------|
| http://localhost:8080/api/products | Lista de productos |
| http://localhost:8080/api/categories | Lista de categorías |
| http://localhost:8080/api/sales | Ventas registradas |
| http://localhost:8080/api/alerts/active | Alertas de stock bajo |
| http://localhost:8080/api/sales/summary | Resumen de ventas del día y mes |

---

## Resumen de lo que necesita estar activo

| Servicio | Estado necesario |
|----------|-----------------|
| XAMPP MySQL | ENCENDIDO |
| XAMPP Apache | APAGADO (no se necesita) |
| Spring Boot (mvnw.cmd spring-boot:run) | CORRIENDO en terminal |

---

## Si hay un error de conexión a MySQL

Verifica en `src\main\resources\application.properties`:
- `spring.datasource.username=root`
- `spring.datasource.password=`  ← déjalo vacío si tu MySQL no tiene contraseña
