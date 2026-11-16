-- =====================================================================
-- DATOS DE PRUEBA COMPLETOS — Sistema de Barbería (SQL SERVER / SSMS)
-- Servidor: (localdb)\MSSQLLocalDB
-- Propósito: Poblar la base de datos para probar el flujo completo:
--   - Login de Admin, Barberos y Clientes
--   - Agenda del Barbero y Panel de Administrador
--   - Catálogo de Productos y Carrito Persistente
--   - Suscripciones activas
--   - Calificaciones y promedio de estrellas
--
-- Contraseñas (BCrypt generadas con cost=12):
--   admin@elcaballero.cl   → admin@elcaballero.cl
--   Barbero123! → juan@elcaballero.cl / miguel@elcaballero.cl
--   Cliente123! → cliente@test.cl / cliente2@test.cl
-- =====================================================================

USE barberia_db;
GO

-- ─── LIMPIEZA DE DATOS DE PRUEBA PREVIOS ────────────────────────────
DELETE FROM calificaciones;
DELETE FROM cita_servicios;
DELETE FROM citas;
DELETE FROM carritos_items;
DELETE FROM cliente_suscripciones;
DELETE FROM suscripciones;
DELETE FROM productos;
DELETE FROM servicios;
DELETE FROM empleados;
DELETE FROM clientes;
DELETE FROM personas WHERE email != N'superadmin@barberiasystem.cl';
GO

-- =====================================================================
-- PASO 1: Personas (Usuarios del sistema)
-- =====================================================================
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
VALUES
    -- Admin de la barbería
    (N'11111111-1', N'RUT', N'Carlos', N'Mendoza', N'admin@elcaballero.cl', N'+56911111111',
     N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK',
     N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbero 1
    (N'22222222-2', N'RUT', N'Juan', N'Pérez', N'juan@elcaballero.cl', N'+56922222222',
     N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbero 2
    (N'44444444-4', N'RUT', N'Miguel', N'Torres', N'miguel@elcaballero.cl', N'+56944444444',
     N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Cliente 1
    (N'33333333-3', N'RUT', N'Pedro', N'González', N'cliente@test.cl', N'+56933333333',
     N'$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     N'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Cliente 2
    (N'55555555-5', N'RUT', N'Ana', N'Martínez', N'cliente2@test.cl', N'+56955555555',
     N'$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     N'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', N'es', 'CL');
GO

-- =====================================================================
-- PASO 2: Barbería (Asegurar Barbería El Caballero con ID 1)
-- =====================================================================
IF EXISTS (SELECT 1 FROM barberias WHERE id = 1)
BEGIN
    UPDATE barberias
    SET nombre      = N'Barbería El Caballero',
        direccion   = N'Av. Providencia 1234, Santiago',
        telefono    = N'+56222345678',
        email       = N'contacto@elcaballero.cl',
        logo_url    = N'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80',
        descripcion = N'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.'
    WHERE id = 1;
END
ELSE
BEGIN
    SET IDENTITY_INSERT barberias ON;
    INSERT INTO barberias (id, nombre, direccion, telefono, email, logo_url, descripcion, pais_codigo, moneda_codigo, zona_horaria)
    VALUES (1, N'Barbería El Caballero', N'Av. Providencia 1234, Santiago',
            N'+56222345678', N'contacto@elcaballero.cl',
            N'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80',
            N'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.',
            'CL', 'CLP', N'America/Santiago');
    SET IDENTITY_INSERT barberias OFF;
END
GO

-- =====================================================================
-- PASO 3: Roles (Empleados y Clientes)
-- =====================================================================
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

-- =====================================================================
-- PASO 4: Servicios de la Barbería
-- =====================================================================
INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos)
VALUES
    (1, N'Corte Clásico',   N'Corte de cabello clásico con tijeras y máquina. Incluye lavado y secado con estilo.',  15000, 45),
    (1, N'Barba Completa',  N'Perfilado, recorte y arreglo de barba con toalla caliente, navaja y aceite de barba.',  12000, 30),
    (1, N'Combo Premium',   N'Corte de cabello + Barba completa. El pack más popular y completo de la casa.',        25000, 75),
    (1, N'Diseño de Cejas', N'Delineado y perfilado de cejas masculinas para un acabado limpio y definido.',          8000, 20);
GO

-- =====================================================================
-- PASO 5: Productos de la Tienda
-- =====================================================================
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

-- =====================================================================
-- PASO 6: Suscripciones de la Barbería y del Cliente
-- =====================================================================
INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa)
VALUES
    (1, N'Plan Mensual VIP', N'MENSUAL', NULL, 49900,
     N'Acceso ilimitado a cortes durante el mes. 20% de descuento en todos los productos de la tienda.',
     1),
    (1, N'Plan por Cita',    N'POR_CITA', 9990, NULL,
     N'Descuento fijo garantizado por cita. Ideal para clientes frecuentes sin cargo mensual.',
     1);

-- Suscripción activa para Cliente 1 (Pedro)
INSERT INTO cliente_suscripciones (cliente_id, suscripcion_id, fecha_inicio, fecha_vencimiento, activa)
SELECT p.id, s.id, CAST(GETDATE() AS DATE), DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), 1
FROM personas p
INNER JOIN suscripciones s ON s.nombre = N'Plan Mensual VIP' AND s.barberia_id = 1
WHERE p.email = N'cliente@test.cl';
GO

-- =====================================================================
-- PASO 7: Carrito de Compras Persistente (Cliente 1 en Barbería 1)
-- =====================================================================
DECLARE @cli1_id     INT = (SELECT id FROM personas WHERE email = N'cliente@test.cl');
DECLARE @prod_aceite INT = (SELECT id FROM productos WHERE nombre = N'Aceite de Barba Premium' AND barberia_id = 1);
DECLARE @prod_cera   INT = (SELECT id FROM productos WHERE nombre = N'Cera de Fijación Fuerte' AND barberia_id = 1);

INSERT INTO carritos_items (cliente_id, barberia_id, producto_id, cantidad)
VALUES
    (@cli1_id, 1, @prod_aceite, 1),
    (@cli1_id, 1, @prod_cera,   2);
GO

-- =====================================================================
-- PASO 8: Citas de Prueba en Múltiples Estados
-- =====================================================================
DECLARE @cli1_id     INT = (SELECT id FROM personas WHERE email = N'cliente@test.cl');
DECLARE @cli2_id     INT = (SELECT id FROM personas WHERE email = N'cliente2@test.cl');
DECLARE @barb_juan   INT = (SELECT id FROM personas WHERE email = N'juan@elcaballero.cl');
DECLARE @barb_miguel INT = (SELECT id FROM personas WHERE email = N'miguel@elcaballero.cl');

DECLARE @srv_corte INT = (SELECT id FROM servicios WHERE nombre = N'Corte Clásico'  AND barberia_id = 1);
DECLARE @srv_barba INT = (SELECT id FROM servicios WHERE nombre = N'Barba Completa' AND barberia_id = 1);
DECLARE @srv_combo INT = (SELECT id FROM servicios WHERE nombre = N'Combo Premium'  AND barberia_id = 1);

-- Cita 1: CONFIRMADA (Mañana 10:00 — Pedro con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_juan, DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '10:00:00', N'CONFIRMADA', 15000, N'Prefiere tijeras, no máquina.');

-- Cita 2: PENDIENTE (Hoy 15:30 — Ana con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli2_id, @barb_juan, CAST(GETDATE() AS DATE), '15:30:00', N'PENDIENTE', 25000, N'Primera visita. Combo Premium.');

-- Cita 3: PENDIENTE (Hoy 17:00 — Pedro con Miguel)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_miguel, CAST(GETDATE() AS DATE), '17:00:00', N'PENDIENTE', 12000, N'Perfilado de barba.');

-- Cita 4: COMPLETADA (Hace 3 días 11:00 — Pedro con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_juan, DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), '11:00:00', N'COMPLETADA', 15000, N'Corte clásico.');

-- Cita 5: CANCELADA (Hace 7 días 09:30 — Pedro con Miguel)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_miguel, DATEADD(DAY, -7, CAST(GETDATE() AS DATE)), '09:30:00', N'CANCELADA', 0, N'Cancelada por el cliente por viaje.');

-- =====================================================================
-- PASO 9: Relación Cita - Servicios (cita_servicios)
-- =====================================================================
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

-- =====================================================================
-- PASO 10: Calificación de la Cita Completada
-- =====================================================================
INSERT INTO calificaciones (cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario)
SELECT TOP 1 c.id, c.cliente_id, c.barberia_id, c.empleado_id, 5, 5, N'¡Excelente servicio! Juan es muy profesional, puntual y detallista.'
FROM citas c WHERE c.estado = N'COMPLETADA';
GO

SELECT N'Datos de prueba cargados exitosamente en barberia_db (SQL Server).' AS resultado;
GO
