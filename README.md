# Barber System - Backend & Frontend

Este repositorio contiene el sistema completo de gestión de barberías. Está construido con una arquitectura monolítica (servidor y cliente en el mismo proyecto para desarrollo) y separación estricta de responsabilidades.

## Tecnologías Utilizadas

### Backend (Java)
- **Javalin 6:** Framework web ligero y rápido para la API REST.
- **HikariCP:** Gestión eficiente del pool de conexiones a la base de datos.
- **MySQL / JDBC:** Persistencia de datos.
- **JJWT (JSON Web Tokens):** Autenticación sin estado (stateless) y segura basada en roles.
- **BCrypt:** Hasheo seguro de contraseñas.
- **Dotenv:** Gestión de credenciales mediante archivo `.env`.

### Frontend (Vanilla)
- **HTML5 & CSS3:** Interfaces modernas, adaptables y modulares.
- **JavaScript ES6:** Lógica asíncrona (`async/await`) estructurada en módulos por dominio.
- **Arquitectura Limpia:** Eliminación total de código espagueti. Cada vista HTML tiene su propia hoja de estilos (`css/pages/`) y su propio controlador JavaScript (`js/pages/`).

## Estructura del Proyecto

```text
ProyectoBarberia/
├── src/main/java/             # Código fuente del Backend (Java)
│   ├── API/                   # Controladores REST, Middleware JWT y configuración Javalin
│   ├── Logica/                # Modelos de Dominio y Enums
│   └── Persistencia/          # DAOs y configuración HikariCP
├── frontend/                  # Código fuente del Frontend
│   ├── css/                   # global.css y módulos por vista
│   ├── js/                    # api.js, auth.js y módulos por vista
│   └── pages/                 # Vistas HTML organizadas por rol (admin, barbero, etc.)
├── database/                  # Scripts SQL (schema.sql y datos_prueba.sql)
└── pom.xml                    # Dependencias de Maven
```

## Seguridad y Buenas Prácticas

- **Protección de Rutas:** El middleware `SecurityMiddleware.java` intercepta todas las solicitudes al backend y verifica las firmas de los tokens JWT antes de llegar a los controladores.
- **Autorización a Nivel de Barbería:** Los administradores de barberías no pueden acceder ni modificar información de otras barberías. La asignación se valida estrictamente a nivel de base de datos y token.
- **Archivos Sensibles Ignorados:** Las credenciales y llaves de acceso están protegidas. El archivo `.env` está en `.gitignore`.

## Configuración y Despliegue Local

1. Clona el repositorio:
   ```bash
   git clone https://github.com/AlvaroBrizRiv/Barber-System-Backend.git
   ```
2. Importa la base de datos MySQL usando los scripts en la carpeta `database/`.
3. Crea un archivo `.env` en la raíz del proyecto basándote en un archivo de configuración estándar:
   ```env
   DB_URL=jdbc:mysql://localhost:3306/barberia_db
   DB_USER=root
   DB_PASSWORD=tu_password
   JWT_SECRET=tu_clave_secreta_de_al_menos_32_caracteres
   ```
4. Abre el proyecto en tu IDE (ej. Apache NetBeans) y compila con Maven (`Clean & Build`).
5. Inicia la aplicación corriendo la clase principal `ProyectoBarberia.java`.
6. (Opcional) Usa una extensión como Live Server en Visual Studio Code para levantar la carpeta `frontend/` en tu navegador.

## Licencia
Propiedad Intelectual / Uso Restringido.
