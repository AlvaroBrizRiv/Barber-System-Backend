-- =====================================================================
-- SCHEMA DE BASE DE DATOS — SISTEMA DE BARBERÍA (SQL SERVER / SSMS)
-- Servidor: (localdb)\MSSQLLocalDB
-- Motor: Microsoft SQL Server (T-SQL)
-- Arquitectura: Multi-tenant (cada barbería aislada por barberia_id)
-- Escalabilidad: Multi-país (RUT/CPF/DNI, moneda, zona horaria, idioma)
-- =====================================================================

USE master;
GO

IF DB_ID(N'barberia_db') IS NULL
BEGIN
    CREATE DATABASE barberia_db;
END
GO

-- ─── CONFIGURACIÓN DE USUARIO PARA CONEXIÓN JDBC (.env) ──────────────
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'barberia_user')
BEGIN
    CREATE LOGIN [barberia_user]
        WITH PASSWORD = N'Barberia1234!',
             DEFAULT_DATABASE = [barberia_db],
             CHECK_EXPIRATION = OFF,
             CHECK_POLICY = ON;
END
ELSE
BEGIN
    ALTER LOGIN [barberia_user]
        WITH PASSWORD = N'Barberia1234!',
             DEFAULT_DATABASE = [barberia_db],
             CHECK_EXPIRATION = OFF,
             CHECK_POLICY = ON;
    ALTER LOGIN [barberia_user] ENABLE;
END
GO

USE barberia_db;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'barberia_user')
BEGIN
    CREATE USER [barberia_user] FOR LOGIN [barberia_user];
END
ALTER ROLE [db_datareader] ADD MEMBER [barberia_user];
ALTER ROLE [db_datawriter] ADD MEMBER [barberia_user];
GO

-- ─── LIMPIEZA PREVIA PARA RE-EJECUCIÓN SEGURA ───────────────────────
DROP TABLE IF EXISTS calificaciones;
DROP TABLE IF EXISTS cita_servicios;
DROP TABLE IF EXISTS citas;
DROP TABLE IF EXISTS carritos_items;
DROP TABLE IF EXISTS cliente_suscripciones;
DROP TABLE IF EXISTS suscripciones;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS servicios;
DROP TABLE IF EXISTS superadmins;
DROP TABLE IF EXISTS empleados;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS barberias;
DROP TABLE IF EXISTS personas;
GO

-- =====================================================================
-- TABLA: personas
-- Base para todos los usuarios del sistema (herencia de tabla concreta)
-- Multi-país: documento_identidad + tipo_documento
-- =====================================================================
CREATE TABLE personas (
    id                      INT IDENTITY(1,1) PRIMARY KEY,
    documento_identidad     NVARCHAR(20)    NOT NULL UNIQUE,
    tipo_documento          NVARCHAR(20)    NOT NULL DEFAULT 'RUT'
                            CONSTRAINT ck_personas_tipo_doc CHECK (tipo_documento IN ('RUT','CPF','DNI','PASAPORTE','OTRO')),
    nombre                  NVARCHAR(100)   NOT NULL,
    apellido                NVARCHAR(100)   NOT NULL,
    email                   NVARCHAR(150)   NOT NULL UNIQUE,
    telefono                NVARCHAR(20)    NULL,
    password_hash           NVARCHAR(255)   NOT NULL,
    foto_url                NVARCHAR(500)   NULL,
    idioma_preferido        NVARCHAR(5)     NOT NULL DEFAULT 'es',
    pais_codigo             CHAR(2)         NOT NULL DEFAULT 'CL',
    auth_provider           NVARCHAR(20)    NOT NULL DEFAULT 'LOCAL'
                            CONSTRAINT ck_personas_auth CHECK (auth_provider IN ('LOCAL','GOOGLE','GITHUB')),
    mfa_habilitado          BIT             NOT NULL DEFAULT 0,
    fecha_registro          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    activo                  BIT             NOT NULL DEFAULT 1
);

CREATE INDEX idx_personas_email ON personas(email);
CREATE INDEX idx_personas_doc   ON personas(documento_identidad);
GO

-- =====================================================================
-- TABLA: barberias
-- Cada registro representa una barbería independiente (tenant)
-- =====================================================================
CREATE TABLE barberias (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    nombre          NVARCHAR(200)   NOT NULL,
    direccion       NVARCHAR(300)   NULL,
    telefono        NVARCHAR(20)    NULL,
    email           NVARCHAR(150)   NULL,
    logo_url        NVARCHAR(500)   NULL,
    descripcion     NVARCHAR(MAX)   NULL,
    activa          BIT             NOT NULL DEFAULT 1,
    pais_codigo     CHAR(2)         NOT NULL DEFAULT 'CL',
    moneda_codigo   CHAR(3)         NOT NULL DEFAULT 'CLP',
    zona_horaria    NVARCHAR(50)    NOT NULL DEFAULT 'America/Santiago',
    -- Si es 1, la página de la barbería muestra la sección de suscripciones
    suscripciones_habilitadas BIT   NOT NULL DEFAULT 0,
    fecha_creacion  DATETIME2       NOT NULL DEFAULT SYSDATETIME()
);

CREATE INDEX idx_barberias_activa ON barberias(activa);
CREATE INDEX idx_barberias_pais   ON barberias(pais_codigo);
GO

-- =====================================================================
-- TABLA: clientes
-- =====================================================================
CREATE TABLE clientes (
    id                  INT PRIMARY KEY,
    puntos_fidelidad    INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_clientes_personas FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE ON UPDATE CASCADE
);
GO

-- =====================================================================
-- TABLA: empleados
-- =====================================================================
CREATE TABLE empleados (
    id              INT PRIMARY KEY,
    barberia_id     INT             NOT NULL,
    rol             NVARCHAR(20)    NOT NULL DEFAULT 'BARBERO'
                    CONSTRAINT ck_empleados_rol CHECK (rol IN ('BARBERO', 'ADMIN_BARBERIA')),
    activo          BIT             NOT NULL DEFAULT 1,
    CONSTRAINT fk_empleados_personas  FOREIGN KEY (id)          REFERENCES personas(id)  ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_empleados_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_empleados_barberia ON empleados(barberia_id);
CREATE INDEX idx_empleados_rol      ON empleados(rol);
GO

-- =====================================================================
-- TABLA: superadmins
-- =====================================================================
CREATE TABLE superadmins (
    id INT PRIMARY KEY,
    CONSTRAINT fk_superadmins_personas FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE ON UPDATE CASCADE
);
GO

-- =====================================================================
-- TABLA: servicios
-- =====================================================================
CREATE TABLE servicios (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    barberia_id         INT             NOT NULL,
    nombre              NVARCHAR(200)   NOT NULL,
    descripcion         NVARCHAR(MAX)   NULL,
    precio              DECIMAL(10,2)   NOT NULL,
    duracion_minutos    INT             NOT NULL DEFAULT 30,
    activo              BIT             NOT NULL DEFAULT 1,
    CONSTRAINT fk_servicios_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_servicios_barberia ON servicios(barberia_id);
GO

-- =====================================================================
-- TABLA: citas
-- =====================================================================
CREATE TABLE citas (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    barberia_id     INT             NOT NULL,
    cliente_id      INT             NOT NULL,
    empleado_id     INT             NOT NULL,
    fecha           DATE            NOT NULL,
    hora            TIME(0)         NOT NULL,
    estado          NVARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE'
                    CONSTRAINT ck_citas_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA')),
    precio_total    DECIMAL(10,2)   NULL,
    notas           NVARCHAR(MAX)   NULL,
    fecha_creacion  DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_citas_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE NO ACTION,
    CONSTRAINT fk_citas_clientes  FOREIGN KEY (cliente_id)  REFERENCES clientes(id)  ON DELETE NO ACTION,
    CONSTRAINT fk_citas_empleados FOREIGN KEY (empleado_id) REFERENCES empleados(id) ON DELETE NO ACTION
);

CREATE INDEX idx_citas_barberia_fecha ON citas(barberia_id, fecha);
CREATE INDEX idx_citas_empleado_fecha ON citas(empleado_id, fecha);
CREATE INDEX idx_citas_cliente        ON citas(cliente_id);
-- Reportes de ganancias: citas completadas por barbería/barbero y rango de fechas
CREATE INDEX idx_citas_ganancias      ON citas(barberia_id, estado, fecha) INCLUDE (empleado_id, precio_total);
-- SEC-04: Índice único anti doble reserva en el mismo horario para un barbero (excluye canceladas)
CREATE UNIQUE INDEX ux_citas_barbero_slot ON citas(empleado_id, fecha, hora) WHERE estado <> 'CANCELADA';
GO

-- =====================================================================
-- TABLA: cita_servicios
-- =====================================================================
CREATE TABLE cita_servicios (
    cita_id         INT NOT NULL,
    servicio_id     INT NOT NULL,
    PRIMARY KEY (cita_id, servicio_id),
    CONSTRAINT fk_cita_servicios_citas     FOREIGN KEY (cita_id)     REFERENCES citas(id)     ON DELETE CASCADE,
    CONSTRAINT fk_cita_servicios_servicios FOREIGN KEY (servicio_id) REFERENCES servicios(id) ON DELETE NO ACTION
);
GO

-- =====================================================================
-- TABLA: productos
-- =====================================================================
CREATE TABLE productos (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    barberia_id     INT             NOT NULL,
    nombre          NVARCHAR(200)   NOT NULL,
    descripcion     NVARCHAR(MAX)   NULL,
    precio          DECIMAL(10,2)   NOT NULL,
    stock           INT             NOT NULL DEFAULT 0,
    categoria       NVARCHAR(30)    NOT NULL DEFAULT 'OTRO'
                    CONSTRAINT ck_productos_categoria CHECK (categoria IN (
                        'SHAMPOO',
                        'BALSAMO',
                        'CREMA_BARBEAR',
                        'CREMA_MODELADORA',
                        'ACEITE_BARBA',
                        'CERA_CABELLO',
                        'LOCION',
                        'OTRO'
                    )),
    imagen_url      NVARCHAR(500)   NULL,
    activo          BIT             NOT NULL DEFAULT 1,
    CONSTRAINT fk_productos_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_productos_barberia  ON productos(barberia_id);
CREATE INDEX idx_productos_categoria ON productos(categoria);
GO

-- =====================================================================
-- TABLA: carritos_items
-- Carrito de compras persistente por cliente y barbería
-- =====================================================================
CREATE TABLE carritos_items (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id      INT         NOT NULL,
    barberia_id     INT         NOT NULL,
    producto_id     INT         NOT NULL,
    cantidad        INT         NOT NULL DEFAULT 1,
    fecha_agregado  DATETIME2   NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uk_carrito_item UNIQUE (cliente_id, barberia_id, producto_id),
    CONSTRAINT fk_carrito_clientes  FOREIGN KEY (cliente_id)  REFERENCES clientes(id)  ON DELETE CASCADE,
    CONSTRAINT fk_carrito_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE NO ACTION,
    CONSTRAINT fk_carrito_productos FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE INDEX idx_carrito_cliente_barberia ON carritos_items(cliente_id, barberia_id);
GO

-- =====================================================================
-- TABLA: suscripciones
-- =====================================================================
CREATE TABLE suscripciones (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    barberia_id         INT             NOT NULL,
    nombre              NVARCHAR(200)   NOT NULL,
    tipo                NVARCHAR(20)    NOT NULL
                        CONSTRAINT ck_suscripciones_tipo CHECK (tipo IN ('POR_CITA','MENSUAL','AMBAS')),
    precio_por_cita     DECIMAL(10,2)   NULL,
    precio_mensual      DECIMAL(10,2)   NULL,
    descripcion         NVARCHAR(MAX)   NULL,
    activa              BIT             NOT NULL DEFAULT 1,
    CONSTRAINT fk_suscripciones_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_suscripciones_barberia ON suscripciones(barberia_id);
GO

-- =====================================================================
-- TABLA: cliente_suscripciones
-- =====================================================================
CREATE TABLE cliente_suscripciones (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    cliente_id          INT     NOT NULL,
    suscripcion_id      INT     NOT NULL,
    fecha_inicio        DATE    NOT NULL,
    fecha_vencimiento   DATE    NULL,
    activa              BIT     NOT NULL DEFAULT 1,
    CONSTRAINT fk_cli_subs_clientes      FOREIGN KEY (cliente_id)     REFERENCES clientes(id)      ON DELETE CASCADE,
    CONSTRAINT fk_cli_subs_suscripciones FOREIGN KEY (suscripcion_id) REFERENCES suscripciones(id) ON DELETE NO ACTION
);

CREATE INDEX idx_cli_subs_cliente     ON cliente_suscripciones(cliente_id);
CREATE INDEX idx_cli_subs_suscripcion ON cliente_suscripciones(suscripcion_id);
GO

-- =====================================================================
-- TABLA: calificaciones
-- =====================================================================
CREATE TABLE calificaciones (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    cita_id             INT             NOT NULL UNIQUE,
    cliente_id          INT             NOT NULL,
    barberia_id         INT             NOT NULL,
    empleado_id         INT             NOT NULL,
    estrellas_barberia  TINYINT         NOT NULL CONSTRAINT ck_cal_barberia CHECK (estrellas_barberia BETWEEN 1 AND 5),
    estrellas_barbero   TINYINT         NOT NULL CONSTRAINT ck_cal_barbero  CHECK (estrellas_barbero  BETWEEN 1 AND 5),
    comentario          NVARCHAR(MAX)   NULL,
    fecha               DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_calificaciones_citas     FOREIGN KEY (cita_id)     REFERENCES citas(id)     ON DELETE NO ACTION,
    CONSTRAINT fk_calificaciones_clientes  FOREIGN KEY (cliente_id)  REFERENCES clientes(id)  ON DELETE NO ACTION,
    CONSTRAINT fk_calificaciones_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE NO ACTION,
    CONSTRAINT fk_calificaciones_empleados FOREIGN KEY (empleado_id) REFERENCES empleados(id) ON DELETE NO ACTION
);

CREATE INDEX idx_calificaciones_barberia ON calificaciones(barberia_id);
CREATE INDEX idx_calificaciones_empleado ON calificaciones(empleado_id);
GO

-- =====================================================================
-- DATOS INICIALES Y DE PRUEBA DEL SISTEMA
-- Contraseñas (BCrypt cost=12):
--   SuperAdmin: Admin1234!  → superadmin@barberiasystem.cl
--   Admin:      Admin123!   → admin@elcaballero.cl
--   Barberos:   Barbero123! → juan@elcaballero.cl / miguel@elcaballero.cl
--   Clientes:   Cliente123! → cliente@test.cl / cliente2@test.cl
-- =====================================================================

-- 1. SuperAdmin
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, pais_codigo)
VALUES (
    N'00000000-0', N'RUT',
    N'Super', N'Admin',
    N'superadmin@barberiasystem.cl',
    N'+56900000000',
    N'$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewzFxSSCiIb0u/3S',
    'CL'
);

INSERT INTO superadmins (id)
SELECT id FROM personas WHERE email = N'superadmin@barberiasystem.cl';

-- 2. Barbería Demo Principal (ID = 1)
SET IDENTITY_INSERT barberias ON;
INSERT INTO barberias (id, nombre, direccion, telefono, email, logo_url, descripcion, pais_codigo, moneda_codigo, zona_horaria)
VALUES (
    1,
    N'Barbería El Caballero',
    N'Av. Providencia 1234, Santiago',
    N'+56222345678',
    N'contacto@elcaballero.cl',
    N'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80',
    N'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.',
    'CL', 'CLP', N'America/Santiago'
);
SET IDENTITY_INSERT barberias OFF;
GO

-- 3. Personas de prueba (Admin, Barberos y Clientes)
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
VALUES
    (N'11111111-1', N'RUT', N'Carlos', N'Mendoza', N'admin@elcaballero.cl', N'+56911111111',
     N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK',
     N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'22222222-2', N'RUT', N'Juan', N'Pérez', N'juan@elcaballero.cl', N'+56922222222',
     N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'44444444-4', N'RUT', N'Miguel', N'Torres', N'miguel@elcaballero.cl', N'+56944444444',
     N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'33333333-3', N'RUT', N'Pedro', N'González', N'cliente@test.cl', N'+56933333333',
     N'$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     N'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'55555555-5', N'RUT', N'Ana', N'Martínez', N'cliente2@test.cl', N'+56955555555',
     N'$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     N'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', N'es', 'CL');
GO

-- 4. Asignación de Roles (Empleados y Clientes)
INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@elcaballero.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, N'BARBERO', 1 FROM personas p WHERE p.email = N'juan@elcaballero.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, N'BARBERO', 1 FROM personas p WHERE p.email = N'miguel@elcaballero.cl';

INSERT INTO clientes (id, puntos_fidelidad)
SELECT p.id, 120 FROM personas p WHERE p.email = N'cliente@test.cl';

INSERT INTO clientes (id, puntos_fidelidad)
SELECT p.id, 30 FROM personas p WHERE p.email = N'cliente2@test.cl';
GO

-- 5. Servicios de la Barbería
INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos)
VALUES
    (1, N'Corte Clásico',   N'Corte de cabello clásico con tijeras y máquina. Incluye lavado y secado con estilo.',  15000, 45),
    (1, N'Barba Completa',  N'Perfilado, recorte y arreglo de barba con toalla caliente, navaja y aceite de barba.',  12000, 30),
    (1, N'Combo Premium',   N'Corte de cabello + Barba completa. El pack más popular y completo de la casa.',        25000, 75),
    (1, N'Diseño de Cejas', N'Delineado y perfilado de cejas masculinas para un acabado limpio y definido.',          8000, 20);
GO

-- 6. Productos de la Tienda
INSERT INTO productos (barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url)
VALUES
    (1, N'Aceite de Barba Premium',
     N'Aceite natural de argán y jojoba para suavizar y nutrir la barba. 30ml.',
     18500, 25, N'ACEITE_BARBA',
     N'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80'),
    (1, N'Shampoo Anticaspa Pro',
     N'Fórmula de cuidado avanzado con zinc para control de caspa y cuero cabelludo graso.',
     14900, 40, N'SHAMPOO',
     N'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=400&q=80'),
    (1, N'Cera de Fijación Fuerte',
     N'Cera de acabado mate con fijación extrema durante todo el día. Ideal para peinados estructurados.',
     12500, 30, N'CERA_CABELLO',
     N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80'),
    (1, N'Crema de Afeitar Classic',
     N'Crema de afeitar con aloe vera y vitamina E. Para un afeitado suave y sin irritaciones.',
     9900, 50, N'CREMA_BARBEAR',
     N'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80'),
    (1, N'Bálsamo Post-Barba',
     N'Bálsamo calmante y refrescante con mentol y árbol de té para después del afeitado.',
     11000, 20, N'BALSAMO',
     N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80');
GO

-- 7. Suscripciones
INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa)
VALUES
    (1, N'Plan Mensual VIP', N'MENSUAL', NULL, 49900,
     N'Acceso ilimitado a cortes durante el mes. 20% de descuento en todos los productos de la tienda.',
     1),
    (1, N'Plan por Cita',    N'POR_CITA', 9990, NULL,
     N'Descuento fijo garantizado por cita. Ideal para clientes frecuentes sin cargo mensual.',
     1);

-- La barbería 1 ofrece suscripciones: se habilita la sección en su página
UPDATE barberias SET suscripciones_habilitadas = 1 WHERE id = 1;

INSERT INTO cliente_suscripciones (cliente_id, suscripcion_id, fecha_inicio, fecha_vencimiento, activa)
SELECT p.id, s.id, CAST(GETDATE() AS DATE), DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), 1
FROM personas p
INNER JOIN suscripciones s ON s.nombre = N'Plan Mensual VIP' AND s.barberia_id = 1
WHERE p.email = N'cliente@test.cl';
GO

-- 8. Carrito Persistente y Citas de Prueba
DECLARE @cli1_id     INT = (SELECT id FROM personas WHERE email = N'cliente@test.cl');
DECLARE @cli2_id     INT = (SELECT id FROM personas WHERE email = N'cliente2@test.cl');
DECLARE @barb_juan   INT = (SELECT id FROM personas WHERE email = N'juan@elcaballero.cl');
DECLARE @barb_miguel INT = (SELECT id FROM personas WHERE email = N'miguel@elcaballero.cl');

DECLARE @prod_aceite INT = (SELECT id FROM productos WHERE nombre = N'Aceite de Barba Premium' AND barberia_id = 1);
DECLARE @prod_cera   INT = (SELECT id FROM productos WHERE nombre = N'Cera de Fijación Fuerte' AND barberia_id = 1);

INSERT INTO carritos_items (cliente_id, barberia_id, producto_id, cantidad)
VALUES
    (@cli1_id, 1, @prod_aceite, 1),
    (@cli1_id, 1, @prod_cera,   2);

DECLARE @srv_corte INT = (SELECT id FROM servicios WHERE nombre = N'Corte Clásico'  AND barberia_id = 1);
DECLARE @srv_barba INT = (SELECT id FROM servicios WHERE nombre = N'Barba Completa' AND barberia_id = 1);
DECLARE @srv_combo INT = (SELECT id FROM servicios WHERE nombre = N'Combo Premium'  AND barberia_id = 1);

INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES
    (1, @cli1_id, @barb_juan,   DATEADD(DAY, 1, CAST(GETDATE() AS DATE)),  '10:00:00', N'CONFIRMADA', 15000, N'Prefiere tijeras, no máquina.'),
    (1, @cli2_id, @barb_juan,   CAST(GETDATE() AS DATE),                   '15:30:00', N'PENDIENTE',  25000, N'Primera visita. Combo Premium.'),
    (1, @cli1_id, @barb_miguel, CAST(GETDATE() AS DATE),                   '17:00:00', N'PENDIENTE',  12000, N'Perfilado de barba.'),
    (1, @cli1_id, @barb_juan,   DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), '11:00:00', N'COMPLETADA', 15000, N'Corte clásico.'),
    (1, @cli1_id, @barb_miguel, DATEADD(DAY, -7, CAST(GETDATE() AS DATE)), '09:30:00', N'CANCELADA',  0,     N'Cancelada por el cliente por viaje.');

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_corte FROM citas c WHERE c.notas = N'Prefiere tijeras, no máquina.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_combo FROM citas c WHERE c.notas = N'Primera visita. Combo Premium.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_barba FROM citas c WHERE c.notas = N'Perfilado de barba.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_corte FROM citas c WHERE c.notas = N'Corte clásico.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_combo FROM citas c WHERE c.notas LIKE N'Cancelada por el cliente%';

INSERT INTO calificaciones (cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario)
SELECT TOP 1 c.id, c.cliente_id, c.barberia_id, c.empleado_id, 5, 5, N'¡Excelente servicio! Juan es muy profesional, puntual y detallista.'
FROM citas c WHERE c.estado = N'COMPLETADA';
GO

SELECT N'Schema y datos iniciales de barberia_db creados exitosamente en SQL Server.' AS resultado;
GO
