-- =====================================================================
-- DATOS DE PRUEBA COMPLETOS — Sistema de Barbería
-- Propósito: Poblar la base de datos para probar el flujo completo:
--   - Login de Admin, Barberos y Clientes
--   - Agenda del Barbero y Panel de Administrador
--   - Catálogo de Productos y Carrito Persistente
--   - Suscripciones activas
--   - Calificaciones y promedio de estrellas
--
-- Contraseñas (BCrypt generadas con cost=12):
--   Admin123!   → admin@elcaballero.cl
--   Barbero123! → juan@elcaballero.cl / miguel@elcaballero.cl
--   Cliente123! → cliente@test.cl / cliente2@test.cl
-- =====================================================================

USE barberia_db;

SET FOREIGN_KEY_CHECKS = 0;
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
DELETE FROM superadmins;
DELETE FROM personas WHERE email != 'superadmin@barberiasystem.cl';
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- PASO 1: Personas (Usuarios del sistema)
-- =====================================================================
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
VALUES
    -- Admin de la barbería
    ('11111111-1', 'RUT', 'Carlos', 'Mendoza', 'admin@elcaballero.cl', '+56911111111',
     '$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK',
     'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', 'es', 'CL'),

    -- Barbero 1
    ('22222222-2', 'RUT', 'Juan', 'Pérez', 'juan@elcaballero.cl', '+56922222222',
     '$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', 'es', 'CL'),

    -- Barbero 2
    ('44444444-4', 'RUT', 'Miguel', 'Torres', 'miguel@elcaballero.cl', '+56944444444',
     '$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.',
     'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', 'es', 'CL'),

    -- Cliente 1
    ('33333333-3', 'RUT', 'Pedro', 'González', 'cliente@test.cl', '+56933333333',
     '$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', 'es', 'CL'),

    -- Cliente 2
    ('55555555-5', 'RUT', 'Ana', 'Martínez', 'cliente2@test.cl', '+56955555555',
     '$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi',
     'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80', 'es', 'CL');

-- =====================================================================
-- PASO 2: Barbería (Asegurar Barbería El Caballero con ID 1)
-- =====================================================================
INSERT INTO barberias (id, nombre, direccion, telefono, email, logo_url, descripcion, pais_codigo, moneda_codigo, zona_horaria)
VALUES (1, 'Barbería El Caballero', 'Av. Providencia 1234, Santiago',
        '+56222345678', 'contacto@elcaballero.cl',
        'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80',
        'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.',
        'CL', 'CLP', 'America/Santiago')
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    direccion = VALUES(direccion),
    logo_url = VALUES(logo_url),
    descripcion = VALUES(descripcion);

-- =====================================================================
-- PASO 3: Roles (Empleados y Clientes)
-- =====================================================================
INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, 'ADMIN_BARBERIA', TRUE FROM personas p WHERE p.email = 'admin@elcaballero.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, 'BARBERO', TRUE FROM personas p WHERE p.email = 'juan@elcaballero.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, 'BARBERO', TRUE FROM personas p WHERE p.email = 'miguel@elcaballero.cl';

INSERT INTO clientes (id, puntos_fidelidad)
SELECT p.id, 120 FROM personas p WHERE p.email = 'cliente@test.cl';

INSERT INTO clientes (id, puntos_fidelidad)
SELECT p.id, 30 FROM personas p WHERE p.email = 'cliente2@test.cl';

-- =====================================================================
-- PASO 4: Servicios de la Barbería
-- =====================================================================
INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos)
VALUES
    (1, 'Corte Clásico',     'Corte de cabello clásico con tijeras y máquina. Incluye lavado y secado con estilo.',  15000, 45),
    (1, 'Barba Completa',    'Perfilado, recorte y arreglo de barba con toalla caliente, navaja y aceite de barba.',  12000, 30),
    (1, 'Combo Premium',     'Corte de cabello + Barba completa. El pack más popular y completo de la casa.',       25000, 75),
    (1, 'Diseño de Cejas',   'Delineado y perfilado de cejas masculinas para un acabado limpio y definido.',          8000, 20);

-- =====================================================================
-- PASO 5: Productos de la Tienda
-- =====================================================================
INSERT INTO productos (barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url)
VALUES
    (1, 'Aceite de Barba Premium',
     'Aceite natural de argán y jojoba para suavizar y nutrir la barba. 30ml.',
     18500, 25, 'ACEITE_BARBA',
     'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80'),

    (1, 'Shampoo Anticaspa Pro',
     'Fórmula de cuidado avanzado con zinc para control de caspa y cuero cabelludo graso.',
     14900, 40, 'SHAMPOO',
     'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=400&q=80'),

    (1, 'Cera de Fijación Fuerte',
     'Cera de acabado mate con fijación extrema durante todo el día. Ideal para peinados estructurados.',
     12500, 30, 'CERA_CABELLO',
     'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80'),

    (1, 'Crema de Afeitar Classic',
     'Crema de afeitar con aloe vera y vitamina E. Para un afeitado suave y sin irritaciones.',
     9900, 50, 'CREMA_BARBEAR',
     'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80'),

    (1, 'Bálsamo Post-Barba',
     'Bálsamo calmante y refrescante con mentol y árbol de té para después del afeitado.',
     11000, 20, 'BALSAMO',
     'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80');

-- =====================================================================
-- PASO 6: Suscripciones de la Barbería y del Cliente
-- =====================================================================
INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa)
VALUES
    (1, 'Plan Mensual VIP', 'MENSUAL', NULL, 49900,
     'Acceso ilimitado a cortes durante el mes. 20% de descuento en todos los productos de la tienda.',
     TRUE),
    (1, 'Plan por Cita',    'POR_CITA', 9990, NULL,
     'Descuento fijo garantizado por cita. Ideal para clientes frecuentes sin cargo mensual.',
     TRUE);

-- Suscripción activa para Cliente 1 (Pedro)
INSERT INTO cliente_suscripciones (cliente_id, suscripcion_id, fecha_inicio, fecha_vencimiento, activa)
SELECT p.id, s.id, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), TRUE
FROM personas p
JOIN suscripciones s ON s.nombre = 'Plan Mensual VIP' AND s.barberia_id = 1
WHERE p.email = 'cliente@test.cl';

-- =====================================================================
-- PASO 7: Carrito de Compras Persistente (Cliente 1 en Barbería 1)
-- =====================================================================
SET @cli1_id = (SELECT id FROM personas WHERE email = 'cliente@test.cl');
SET @prod_aceite = (SELECT id FROM productos WHERE nombre = 'Aceite de Barba Premium' AND barberia_id = 1);
SET @prod_cera   = (SELECT id FROM productos WHERE nombre = 'Cera de Fijación Fuerte' AND barberia_id = 1);

INSERT INTO carritos_items (cliente_id, barberia_id, producto_id, cantidad)
VALUES
    (@cli1_id, 1, @prod_aceite, 1),
    (@cli1_id, 1, @prod_cera,   2);

-- =====================================================================
-- PASO 8: Citas de Prueba en Múltiples Estados
-- =====================================================================
SET @cli2_id = (SELECT id FROM personas WHERE email = 'cliente2@test.cl');
SET @barb_juan   = (SELECT id FROM personas WHERE email = 'juan@elcaballero.cl');
SET @barb_miguel = (SELECT id FROM personas WHERE email = 'miguel@elcaballero.cl');

SET @srv_corte = (SELECT id FROM servicios WHERE nombre = 'Corte Clásico' AND barberia_id = 1);
SET @srv_barba = (SELECT id FROM servicios WHERE nombre = 'Barba Completa' AND barberia_id = 1);
SET @srv_combo = (SELECT id FROM servicios WHERE nombre = 'Combo Premium' AND barberia_id = 1);

-- Cita 1: CONFIRMADA (Mañana 10:00 — Pedro con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_juan, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', 'CONFIRMADA', 15000, 'Prefiere tijeras, no máquina.');

-- Cita 2: PENDIENTE (Hoy 15:30 — Ana con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli2_id, @barb_juan, CURDATE(), '15:30:00', 'PENDIENTE', 25000, 'Primera visita. Combo Premium.');

-- Cita 3: PENDIENTE (Hoy 17:00 — Pedro con Miguel)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_miguel, CURDATE(), '17:00:00', 'PENDIENTE', 12000, 'Perfilado de barba.');

-- Cita 4: COMPLETADA (Hace 3 días 11:00 — Pedro con Juan)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_juan, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:00:00', 'COMPLETADA', 15000, 'Corte clásico.');

-- Cita 5: CANCELADA (Hace 7 días 09:30 — Pedro con Miguel)
INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
VALUES (1, @cli1_id, @barb_miguel, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:30:00', 'CANCELADA', 0, 'Cancelada por el cliente por viaje.');

-- =====================================================================
-- PASO 9: Relación Cita - Servicios (cita_servicios)
-- =====================================================================
INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_corte FROM citas c WHERE c.notas = 'Prefiere tijeras, no máquina.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_combo FROM citas c WHERE c.notas = 'Primera visita. Combo Premium.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_barba FROM citas c WHERE c.notas = 'Perfilado de barba.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_corte FROM citas c WHERE c.notas = 'Corte clásico.';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, @srv_combo FROM citas c WHERE c.notas LIKE 'Cancelada por el cliente%';

-- =====================================================================
-- PASO 10: Calificación de la Cita Completada
-- =====================================================================
INSERT INTO calificaciones (cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario)
SELECT c.id, c.cliente_id, c.barberia_id, c.empleado_id, 5, 5, '¡Excelente servicio! Juan es muy profesional, puntual y detallista.'
FROM citas c WHERE c.estado = 'COMPLETADA' LIMIT 1;

SELECT 'Datos de prueba cargados exitosamente en barberia_db.' AS resultado;
