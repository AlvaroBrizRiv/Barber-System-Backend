# Barber System — Backend & Frontend

Plataforma multi-barbería: cada barbería tiene su página pública con barberos, servicios, tienda de productos y, si la barbería lo habilita, planes de suscripción. Los clientes buscan barberías, agendan citas, compran productos y gestionan (o eliminan) su cuenta. Cada barbería tiene un administrador y sus barberos, aislados por `barberiaId` en el JWT.

## Funcionalidades por rol

| Rol | Puede |
|-----|-------|
| **Cliente** | Registrarse, iniciar sesión, **recuperar su contraseña por correo**, editar su perfil y **eliminar su cuenta**; buscar barberías; elegir barbero y servicios; agendar citas; comprar productos. |
| **Barbero** | Ver **solo su propia agenda**, consultar el **stock** de productos (solo lectura) y ver **sus ganancias** diarias, semanales y mensuales. |
| **Admin de barbería** | Gestionar barberos, agenda, servicios, productos y suscripciones; mostrar u ocultar la sección de suscripciones; ver **ganancias** de cada barbero o de todos (día/semana/mes). |
| **Superadmin** | Crear y desactivar barberías. |

## Tecnologías

**Backend (Java 25):** Javalin 6, HikariCP, JDBC (SQL Server y PostgreSQL), JJWT, BCrypt, dotenv-java.
**Frontend:** HTML5, CSS3 y JavaScript ES6 sin frameworks; un JS y un CSS por vista (`js/pages/`, `css/pages/`).
**Pruebas:** JUnit 5 + Mockito (`src/test/java`).

## Estructura

```text
├── src/main/java/com/mycompany/proyectobarberia/
│   ├── API/            # Controladores REST, DTOs, middleware JWT y App (Javalin)
│   ├── Logica/         # Modelos de dominio y enums
│   └── Persistencia/   # DAOs (SQL estándar) y pool HikariCP
├── src/test/java/      # Pruebas unitarias
├── frontend/           # Vistas por rol (pages/admin, pages/barbero, ...)
└── database/
    ├── schema.sql, datos_prueba.sql       # SQL Server (SSMS)
    ├── migraciones/                       # Cambios incrementales para SSMS
    └── postgres/                          # Esquema y datos para Supabase/PostgreSQL
```

## Puesta en marcha (SQL Server / SSMS)

1. Ejecuta `database/schema.sql` en SSMS (crea `barberia_db`, el usuario y datos de prueba).
   Si ya tenías la base creada, ejecuta en su lugar las migraciones en orden:
   `database/migraciones/001_suscripciones_ganancias.sql` y `database/migraciones/002_recuperacion_password.sql`.
2. Copia `.env.example` a `.env` y ajusta valores (`CORS_ORIGIN` debe ser el origen exacto del frontend).
   Para enviar los códigos de recuperación configura `SMTP_*` (ver sección *Correo*).
3. Compila y ejecuta:
   ```bash
   mvn clean package
   ```
   ```bash
   java -jar target/ProyectoBarberia-1.0-SNAPSHOT.jar
   ```
4. Sirve `frontend/` con Live Server (VS Code) en el origen configurado en `CORS_ORIGIN`.

Usuarios de prueba: `admin@elcaballero.cl` / `Admin123!`, `juan@elcaballero.cl` / `Barbero123!`, `cliente@test.cl` / `Cliente123!`.

## Correo (recuperación de contraseña)

"¿Olvidaste tu contraseña?" envía un código de 6 dígitos (válido 15 minutos, máximo 5 intentos) al correo de la cuenta. Tras verificarlo, el usuario define una contraseña nueva.

- Configura `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` y `SMTP_FROM` en `.env` (Gmail necesita una *contraseña de aplicación*).
- Sin `SMTP_HOST`, en desarrollo el correo se muestra en la consola del servidor; en producción (`APP_ENV=production`) no se muestra ni se envía.

## Migración a Supabase (PostgreSQL)

El código Java no cambia: los DAO usan SQL estándar y el driver PostgreSQL ya está en `pom.xml`.

1. En Supabase → SQL Editor, ejecuta `database/postgres/schema.sql` y luego `database/postgres/datos_prueba.sql`.
2. En `.env`, reemplaza `DB_URL`, `DB_USER` y `DB_PASSWORD` por la cadena del *Session pooler* (ver `.env.example`).
3. El esquema activa RLS sin políticas en todas las tablas: la API pública de Supabase (anon key) no expone datos y el backend se conecta como `postgres`.

## Pruebas

```bash
mvn test
```

Si tu JDK local es anterior a 25 puedes ejecutar las pruebas con `mvn test -Dmaven.compiler.release=21` (el código no usa características exclusivas de Java 25).

## API principal

| Método | Ruta | Acceso |
|--------|------|--------|
| GET | `/api/barberias?q=texto` | Público (búsqueda opcional) |
| PATCH | `/api/barberias/{id}/suscripciones` | Admin de esa barbería |
| DELETE | `/api/perfil` | Cliente autenticado (requiere contraseña) |
| POST | `/api/auth/recuperar/solicitar` · `/verificar` · `/restablecer` | Público (código por correo, token de un solo uso) |
| GET | `/api/ganancias/barberia/{id}?periodo=DIA\|SEMANA\|MES&fecha=&barberoId=` | Admin de esa barbería |
| GET | `/api/ganancias/mias?periodo=&fecha=` | Barbero (sus propias ganancias) |

## Seguridad

- `SecurityMiddleware` valida el JWT en cada ruta no pública; el `barberiaId` se toma siempre del token.
- Contraseñas con BCrypt (cost 12), rate limiting en login, registro y eliminación de cuenta.
- Recuperación de contraseña: el código se guarda como hash BCrypt, la respuesta no revela si el correo existe y el token de recuperación (emisor distinto) no sirve como sesión.
- La eliminación de cuenta anonimiza los datos personales y conserva el historial contable de la barbería.
- El frontend escapa todo dato dinámico (`Utils.esc`) y valida URLs (`Utils.urlSegura`).

## Licencia
Propiedad Intelectual / Uso Restringido.
