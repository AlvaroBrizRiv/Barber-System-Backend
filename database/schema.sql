-- =====================================================================
-- SCHEMA DE BASE DE DATOS — SISTEMA DE BARBERÍA
-- Motor: MySQL 8.x / MariaDB
-- Codificación: UTF-8mb4 (soporta emojis y caracteres especiales)
-- Arquitectura: Multi-tenant (cada barbería aislada por barberia_id)
-- Escalabilidad: Multi-país (RUT/CPF/DNI, moneda, zona horaria, idioma)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS barberia_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE barberia_db;

-- ─── LIMPIEZA PREVIA PARA RE-EJECUCIÓN SEGURA ───────────────────────
SET FOREIGN_KEY_CHECKS = 0;
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
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- TABLA: personas
-- Base para todos los usuarios del sistema (herencia de tabla concreta)
-- Multi-país: documento_identidad + tipo_documento
-- =====================================================================
CREATE TABLE personas (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    documento_identidad     VARCHAR(20)     NOT NULL UNIQUE COMMENT 'RUT, CPF, DNI, Pasaporte, etc.',
    tipo_documento          ENUM('RUT','CPF','DNI','PASAPORTE','OTRO')
                                            NOT NULL DEFAULT 'RUT',
    nombre                  VARCHAR(100)    NOT NULL,
    apellido                VARCHAR(100)    NOT NULL,
    email                   VARCHAR(150)    NOT NULL UNIQUE,
    telefono                VARCHAR(20)     COMMENT 'Formato internacional: +56912345678',
    password_hash           VARCHAR(255)    NOT NULL COMMENT 'BCrypt hash, nunca texto plano',
    foto_url                VARCHAR(500)    COMMENT 'URL externa de la foto de perfil',
    idioma_preferido        CHAR(5)         NOT NULL DEFAULT 'es'   COMMENT 'es | pt-BR',
    pais_codigo             CHAR(2)         NOT NULL DEFAULT 'CL'   COMMENT 'ISO 3166-1 alpha-2',
    -- Preparación SSO/MFA (estructura de datos)
    auth_provider           ENUM('LOCAL','GOOGLE','GITHUB') NOT NULL DEFAULT 'LOCAL',
    mfa_habilitado          BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_registro          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    INDEX idx_personas_email (email),
    INDEX idx_personas_doc   (documento_identidad)
) ENGINE=InnoDB COMMENT='Tabla base para todos los usuarios del sistema';

-- =====================================================================
-- TABLA: barberias
-- Cada registro representa una barbería independiente (tenant)
-- =====================================================================
CREATE TABLE barberias (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(200)    NOT NULL,
    direccion       VARCHAR(300),
    telefono        VARCHAR(20),
    email           VARCHAR(150),
    logo_url        VARCHAR(500),
    descripcion     TEXT,
    activa          BOOLEAN         NOT NULL DEFAULT TRUE,
    pais_codigo     CHAR(2)         NOT NULL DEFAULT 'CL'  COMMENT 'País de la barbería',
    moneda_codigo   CHAR(3)         NOT NULL DEFAULT 'CLP' COMMENT 'ISO 4217 (CLP, BRL, USD...)',
    zona_horaria    VARCHAR(50)     NOT NULL DEFAULT 'America/Santiago',
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_barberias_activa (activa),
    INDEX idx_barberias_pais   (pais_codigo)
) ENGINE=InnoDB COMMENT='Cada fila es una barbería independiente (tenant)';

-- =====================================================================
-- TABLA: clientes
-- =====================================================================
CREATE TABLE clientes (
    id                  INT PRIMARY KEY COMMENT 'FK a personas.id',
    puntos_fidelidad    INT         NOT NULL DEFAULT 0,
    FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Clientes registrados en la plataforma';

-- =====================================================================
-- TABLA: empleados
-- =====================================================================
CREATE TABLE empleados (
    id              INT PRIMARY KEY COMMENT 'FK a personas.id',
    barberia_id     INT             NOT NULL,
    rol             ENUM('BARBERO', 'ADMIN_BARBERIA') NOT NULL DEFAULT 'BARBERO',
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_empleados_barberia (barberia_id),
    INDEX idx_empleados_rol (rol)
) ENGINE=InnoDB COMMENT='Empleados (barberos y admins) vinculados a una barbería';

-- =====================================================================
-- TABLA: superadmins
-- =====================================================================
CREATE TABLE superadmins (
    id  INT PRIMARY KEY COMMENT 'FK a personas.id',
    FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Superadministradores con acceso total a la plataforma';

-- =====================================================================
-- TABLA: servicios
-- =====================================================================
CREATE TABLE servicios (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    barberia_id         INT             NOT NULL,
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         TEXT,
    precio              DECIMAL(10,2)   NOT NULL,
    duracion_minutos    INT             NOT NULL DEFAULT 30,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_servicios_barberia (barberia_id)
) ENGINE=InnoDB COMMENT='Servicios ofrecidos por cada barbería';

-- =====================================================================
-- TABLA: citas
-- =====================================================================
CREATE TABLE citas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    barberia_id     INT             NOT NULL,
    cliente_id      INT             NOT NULL,
    empleado_id     INT             NOT NULL,
    fecha           DATE            NOT NULL,
    hora            TIME            NOT NULL,
    estado          ENUM('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA')
                                    NOT NULL DEFAULT 'PENDIENTE',
    precio_total    DECIMAL(10,2),
    notas           TEXT,
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    INDEX idx_citas_barberia_fecha (barberia_id, fecha),
    INDEX idx_citas_empleado_fecha (empleado_id, fecha),
    INDEX idx_citas_cliente (cliente_id)
) ENGINE=InnoDB COMMENT='Citas/reservas';

-- =====================================================================
-- TABLA: cita_servicios
-- =====================================================================
CREATE TABLE cita_servicios (
    cita_id         INT NOT NULL,
    servicio_id     INT NOT NULL,
    PRIMARY KEY (cita_id, servicio_id),
    FOREIGN KEY (cita_id) REFERENCES citas(id) ON DELETE CASCADE,
    FOREIGN KEY (servicio_id) REFERENCES servicios(id)
) ENGINE=InnoDB COMMENT='Servicios incluidos en cada cita';

-- =====================================================================
-- TABLA: productos
-- =====================================================================
CREATE TABLE productos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    barberia_id     INT             NOT NULL,
    nombre          VARCHAR(200)    NOT NULL,
    descripcion     TEXT,
    precio          DECIMAL(10,2)   NOT NULL,
    stock           INT             NOT NULL DEFAULT 0,
    categoria       ENUM(
                        'SHAMPOO',
                        'BALSAMO',
                        'CREMA_BARBEAR',
                        'CREMA_MODELADORA',
                        'ACEITE_BARBA',
                        'CERA_CABELLO',
                        'LOCION',
                        'OTRO'
                    ) NOT NULL DEFAULT 'OTRO',
    imagen_url      VARCHAR(500),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_productos_barberia (barberia_id),
    INDEX idx_productos_categoria (categoria)
) ENGINE=InnoDB COMMENT='Catálogo de productos de aseo masculino por barbería';

-- =====================================================================
-- TABLA: carritos_items
-- Carrito de compras persistente por cliente y barbería
-- =====================================================================
CREATE TABLE carritos_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      INT         NOT NULL,
    barberia_id     INT         NOT NULL,
    producto_id     INT         NOT NULL,
    cantidad        INT         NOT NULL DEFAULT 1,
    fecha_agregado  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_carrito_item (cliente_id, barberia_id, producto_id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE,
    INDEX idx_carrito_cliente_barberia (cliente_id, barberia_id)
) ENGINE=InnoDB COMMENT='Ítems del carrito de compras por cliente y barbería';

-- =====================================================================
-- TABLA: suscripciones
-- =====================================================================
CREATE TABLE suscripciones (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    barberia_id         INT             NOT NULL,
    nombre              VARCHAR(200)    NOT NULL,
    tipo                ENUM('POR_CITA','MENSUAL','AMBAS') NOT NULL,
    precio_por_cita     DECIMAL(10,2),
    precio_mensual      DECIMAL(10,2),
    descripcion         TEXT,
    activa              BOOLEAN         NOT NULL DEFAULT TRUE,
    FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_suscripciones_barberia (barberia_id)
) ENGINE=InnoDB COMMENT='Planes de suscripción definidos por cada barbería';

-- =====================================================================
-- TABLA: cliente_suscripciones
-- =====================================================================
CREATE TABLE cliente_suscripciones (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id          INT         NOT NULL,
    suscripcion_id      INT         NOT NULL,
    fecha_inicio        DATE        NOT NULL,
    fecha_vencimiento   DATE,
    activa              BOOLEAN     NOT NULL DEFAULT TRUE,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones(id),
    INDEX idx_cli_subs_cliente (cliente_id),
    INDEX idx_cli_subs_suscripcion (suscripcion_id)
) ENGINE=InnoDB COMMENT='Suscripciones activas de cada cliente';

-- =====================================================================
-- TABLA: calificaciones
-- =====================================================================
CREATE TABLE calificaciones (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    cita_id             INT         NOT NULL UNIQUE,
    cliente_id          INT         NOT NULL,
    barberia_id         INT         NOT NULL,
    empleado_id         INT         NOT NULL,
    estrellas_barberia  TINYINT     NOT NULL CHECK (estrellas_barberia BETWEEN 1 AND 5),
    estrellas_barbero   TINYINT     NOT NULL CHECK (estrellas_barbero  BETWEEN 1 AND 5),
    comentario          TEXT,
    fecha               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cita_id)     REFERENCES citas(id),
    FOREIGN KEY (cliente_id)  REFERENCES clientes(id),
    FOREIGN KEY (barberia_id) REFERENCES barberias(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    INDEX idx_calificaciones_barberia (barberia_id),
    INDEX idx_calificaciones_empleado (empleado_id)
) ENGINE=InnoDB COMMENT='Calificaciones de barberías y barberos por clientes';

-- =====================================================================
-- DATOS INICIALES DEL SISTEMA
-- =====================================================================

-- 1. SuperAdmin (Contraseña: Admin1234!)
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, pais_codigo)
VALUES (
    '00000000-0', 'RUT',
    'Super', 'Admin',
    'superadmin@barberiasystem.cl',
    '+56900000000',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewzFxSSCiIb0u/3S',
    'CL'
);

INSERT INTO superadmins (id)
SELECT id FROM personas WHERE email = 'superadmin@barberiasystem.cl';

-- 2. Barbería Demo Principal (ID = 1)
INSERT INTO barberias (id, nombre, direccion, telefono, email, descripcion, pais_codigo, moneda_codigo, zona_horaria)
VALUES (1, 'Barbería El Caballero', 'Av. Providencia 1234, Santiago', '+56222345678',
        'contacto@elcaballero.cl', 'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.',
        'CL', 'CLP', 'America/Santiago');

SELECT 'Schema barberia_db creado exitosamente.' AS resultado;
