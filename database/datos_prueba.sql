-- =====================================================================
-- DATOS DE PRUEBA COMPLETOS — Sistema de Barbería (SQL SERVER / SSMS)
-- Servidor: (localdb)\MSSQLLocalDB
-- Datos inyectados:
--   - 5 Barberías (2 con suscripciones habilitadas y planes activos)
--   - 5 Administradores (1 por barbería, clave: Admin123!)
--   - 25 Barberos (5 por barbería, clave: Barbero123!)
--   - 50 Clientes (10 por día para 5 días consecutivos, clave: Cliente123!)
--   - 150 Productos (30 por barbería)
--   - 20 Servicios (4 por barbería)
--   - 50 Citas distribuidas en 5 días consecutivos (desde hoy en adelante)
-- =====================================================================

USE barberia_db;
GO

SET NOCOUNT ON;

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
-- PASO 1: Creación de 5 Barberías
-- =====================================================================
-- Barberías 1 y 2 tienen suscripciones_habilitadas = 1
-- Barberías 3, 4 y 5 tienen suscripciones_habilitadas = 0

DELETE FROM barberias;
DBCC CHECKIDENT ('barberias', RESEED, 0);

SET IDENTITY_INSERT barberias ON;
INSERT INTO barberias (id, nombre, direccion, telefono, email, logo_url, descripcion, activa, pais_codigo, moneda_codigo, zona_horaria, suscripciones_habilitadas)
VALUES
(1, N'Barbería El Caballero', N'Av. Providencia 1234, Santiago', N'+56222345678', N'contacto@elcaballero.cl', N'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80', N'Barbería de élite en Providencia. Especialistas en corte clásico y barba.', 1, 'CL', 'CLP', N'America/Santiago', 1),
(2, N'La Barbería Vintage', N'Av. Las Condes 5678, Las Condes', N'+56222456789', N'contacto@barberiavintage.cl', N'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=800&q=80', N'Estilo clásico y vintage con toallas calientes y afeitado tradicional.', 1, 'CL', 'CLP', N'America/Santiago', 1),
(3, N'The Gentleman Club', N'Calle Nueva Costanera 1020, Vitacura', N'+56222567890', N'info@gentlemanclub.cl', N'https://images.unsplash.com/photo-1621605815971-fbc98d665033?auto=format&fit=crop&w=800&q=80', N'Club exclusivo de barbería y cuidado masculino de alta gama.', 1, 'CL', 'CLP', N'America/Santiago', 0),
(4, N'Urban Cuts Studio', N'Av. Santa Isabel 450, Santiago Centro', N'+56222678901', N'hola@urbancuts.cl', N'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?auto=format&fit=crop&w=800&q=80', N'Cortes urbanos, degradados y diseños modernos para la juventud.', 1, 'CL', 'CLP', N'America/Santiago', 0),
(5, N'Classic Razor Barber Shop', N'Paseo Ahumada 312, Santiago Centro', N'+56222789012', N'contacto@classicrazor.cl', N'https://images.unsplash.com/photo-1512690459411-b9245aed614b?auto=format&fit=crop&w=800&q=80', N'Tradición, rapidez y navaja libre en el centro de la ciudad.', 1, 'CL', 'CLP', N'America/Santiago', 0);
SET IDENTITY_INSERT barberias OFF;
GO

-- =====================================================================
-- PASO 2: Suscripciones para Barberías 1 y 2
-- =====================================================================
INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa)
VALUES
    (1, N'Plan Mensual VIP Caballero', N'MENSUAL', NULL, 49900, N'Cortes ilimitados al mes + 20% descuento en productos.', 1),
    (1, N'Plan Frecuente Por Cita', N'POR_CITA', 9990, NULL, N'Descuento fijo en cada visita programada.', 1),
    (2, N'Club Vintage Mensual', N'MENSUAL', NULL, 45000, N'4 cortes al mes + perfilado de barba y toallas calientes.', 1),
    (2, N'Tarifa Vintage Por Cita', N'POR_CITA', 10500, NULL, N'Precio preferencial para miembros registrados por cita.', 1);
GO

-- =====================================================================
-- PASO 3: 5 Administradores (1 por cada barbería)
-- Clave: Admin123!
-- Hash: $2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK
-- =====================================================================
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
VALUES
    (N'11111111-1', N'RUT', N'Carlos', N'Mendoza', N'admin@elcaballero.cl', N'+56911111111', N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK', N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'11111112-2', N'RUT', N'Roberto', N'Silva', N'admin@barberiavintage.cl', N'+56911111112', N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK', N'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'11111113-3', N'RUT', N'Andrés', N'Larraín', N'admin@gentlemanclub.cl', N'+56911111113', N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK', N'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'11111114-4', N'RUT', N'Matías', N'Vargas', N'admin@urbancuts.cl', N'+56911111114', N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK', N'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'11111115-5', N'RUT', N'Gonzalo', N'Reyes', N'admin@classicrazor.cl', N'+56911111115', N'$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK', N'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=200&q=80', N'es', 'CL');

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@elcaballero.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 2, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@barberiavintage.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 3, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@gentlemanclub.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 4, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@urbancuts.cl';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 5, N'ADMIN_BARBERIA', 1 FROM personas p WHERE p.email = N'admin@classicrazor.cl';
GO

-- =====================================================================
-- PASO 4: 25 Barberos (5 por cada Barbería)
-- Clave: Barbero123!
-- Hash: $2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.
-- =====================================================================
INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
VALUES
    -- Barbería 1
    (N'21000001-1', N'RUT', N'Juan', N'Pérez', N'juan@elcaballero.cl', N'+56921000001', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'21000002-2', N'RUT', N'Miguel', N'Torres', N'miguel@elcaballero.cl', N'+56921000002', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'21000003-3', N'RUT', N'David', N'Castro', N'david@elcaballero.cl', N'+56921000003', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'21000004-4', N'RUT', N'Felipe', N'Morales', N'felipe@elcaballero.cl', N'+56921000004', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'21000005-5', N'RUT', N'Sebastián', N'Navarro', N'sebastian@elcaballero.cl', N'+56921000005', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbería 2
    (N'22000001-1', N'RUT', N'Ignacio', N'Rojas', N'ignacio@barberiavintage.cl', N'+56922000001', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'22000002-2', N'RUT', N'Claudio', N'Gómez', N'claudio@barberiavintage.cl', N'+56922000002', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'22000003-3', N'RUT', N'Pablo', N'Bravo', N'pablo@barberiavintage.cl', N'+56922000003', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'22000004-4', N'RUT', N'Esteban', N'Paredes', N'esteban@barberiavintage.cl', N'+56922000004', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'22000005-5', N'RUT', N'Rodrigo', N'Fuentes', N'rodrigo@barberiavintage.cl', N'+56922000005', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbería 3
    (N'23000001-1', N'RUT', N'Alejandro', N'Soto', N'alejandro@gentlemanclub.cl', N'+56923000001', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'23000002-2', N'RUT', N'Cristian', N'Tapia', N'cristian@gentlemanclub.cl', N'+56923000002', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'23000003-3', N'RUT', N'Jorge', N'Araya', N'jorge@gentlemanclub.cl', N'+56923000003', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'23000004-4', N'RUT', N'Lucas', N'Sepúlveda', N'lucas@gentlemanclub.cl', N'+56923000004', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'23000005-5', N'RUT', N'Gabriel', N'Flores', N'gabriel@gentlemanclub.cl', N'+56923000005', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbería 4
    (N'24000001-1', N'RUT', N'Benjamín', N'Espinoza', N'benjamin@urbancuts.cl', N'+56924000001', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'24000002-2', N'RUT', N'Mauricio', N'Valenzuela', N'mauricio@urbancuts.cl', N'+56924000002', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'24000003-3', N'RUT', N'Álvaro', N'Castillo', N'alvaro@urbancuts.cl', N'+56924000003', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'24000004-4', N'RUT', N'Emilio', N'Cárdenas', N'emilio@urbancuts.cl', N'+56924000004', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'24000005-5', N'RUT', N'Vicente', N'Miranda', N'vicente@urbancuts.cl', N'+56924000005', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),

    -- Barbería 5
    (N'25000001-1', N'RUT', N'Nicolás', N'Carrasco', N'nicolas@classicrazor.cl', N'+56925000001', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'25000002-2', N'RUT', N'Tomás', N'Vergara', N'tomas@classicrazor.cl', N'+56925000002', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'25000003-3', N'RUT', N'Martín', N'Herrera', N'martin@classicrazor.cl', N'+56925000003', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'25000004-4', N'RUT', N'Francisco', N'Salazar', N'francisco@classicrazor.cl', N'+56925000004', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80', N'es', 'CL'),
    (N'25000005-5', N'RUT', N'Guillermo', N'Guzmán', N'guillermo@classicrazor.cl', N'+56925000005', N'$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.', N'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=200&q=80', N'es', 'CL');

-- Asociar barberos a sus respectivas barberías
INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 1, N'BARBERO', 1 FROM personas p WHERE p.email LIKE N'%@elcaballero.cl' AND p.email NOT LIKE N'admin%';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 2, N'BARBERO', 1 FROM personas p WHERE p.email LIKE N'%@barberiavintage.cl' AND p.email NOT LIKE N'admin%';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 3, N'BARBERO', 1 FROM personas p WHERE p.email LIKE N'%@gentlemanclub.cl' AND p.email NOT LIKE N'admin%';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 4, N'BARBERO', 1 FROM personas p WHERE p.email LIKE N'%@urbancuts.cl' AND p.email NOT LIKE N'admin%';

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, 5, N'BARBERO', 1 FROM personas p WHERE p.email LIKE N'%@classicrazor.cl' AND p.email NOT LIKE N'admin%';
GO

-- =====================================================================
-- PASO 5: Servicios para cada una de las 5 barberías
-- =====================================================================
INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos)
VALUES
    -- Barbería 1
    (1, N'Corte Clásico Caballero', N'Lavado, corte tradicional a tijera o máquina y peinado.', 15000, 45),
    (1, N'Barba Completa con Toalla', N'Perfilado, navaja libre y toallas humeantes con aceites.', 12000, 30),
    (1, N'Combo Corte + Barba VIP', N'Servicio integral con asesoría de imagen y lavado premium.', 25000, 75),
    (1, N'Perfilado de Cejas y Barba', N'Definición geométrica con navaja y tratamiento calmante.', 9000, 20),

    -- Barbería 2
    (2, N'Corte Vintage Pompadour', N'Estilo años 50 con fijador mate y peinado con secador.', 16000, 45),
    (2, N'Afeitado Tradicional Navaja', N'Espuma tibia, navaja de hoja completa y bálsamo refrescante.', 13000, 30),
    (2, N'Pack Vintage Integral', N'Corte vintage + arreglo de bigote/barba + loción.', 26000, 75),
    (2, N'Tratamiento Capilar Anticaída', N'Masaje craneal con tónico revitalizante y vapor.', 14000, 30),

    -- Barbería 3
    (3, N'Gentleman Executive Haircut', N'Corte de precisión ejecutiva con lavado de menta.', 22000, 45),
    (3, N'Royal Beard Grooming', N'Esculpido de barba de alta densidad con aceites aromáticos.', 18000, 40),
    (3, N'The Gentleman Ritual', N'Experiencia completa: corte, barba, exfoliación facial y café de grano.', 38000, 90),
    (3, N'Camuflaje de Canas Barba', N'Tonalización natural sutil para barba y patillas.', 15000, 30),

    -- Barbería 4
    (4, N'Urban Skin Fade / Taper', N'Degradado a cero o navaja con líneas ultra nítidas.', 14000, 40),
    (4, N'Diseño Freestyle / Hair Tattoo', N'Líneas y patrones geométricos personalizados.', 10000, 25),
    (4, N'Fade + Arreglo de Barba Urbana', N'Degradado y perfilado de barba con tintura de bordes.', 22000, 60),
    (4, N'Lavado y Ondulación Texturizada', N'Lavado clarificante y producto estilizador de ondas.', 11000, 25),

    -- Barbería 5
    (5, N'Corte Rápido Tradicional', N'Corte clásico sin demoras, prolijo y profesional.', 11000, 30),
    (5, N'Afeitado Rápido de Navaja', N'Afeitado limpio al ras con navaja esterilizada.', 8500, 20),
    (5, N'Dúo Navaja Express', N'Corte con máquina/tijera y afeitado completo.', 18000, 50),
    (5, N'Recorte de Barba Simple', N'Rebaje y contornos con máquina para mantenimiento semanal.', 7000, 15);
GO

-- =====================================================================
-- PASO 6: 30 Productos por Barbería (Total 150 Productos)
-- Categorías válidas: SHAMPOO, BALSAMO, CREMA_BARBEAR, CREMA_MODELADORA,
--                      ACEITE_BARBA, CERA_CABELLO, LOCION, OTRO
-- =====================================================================
DECLARE @b INT = 1;
WHILE @b <= 5
BEGIN
    INSERT INTO productos (barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url, activo)
    VALUES
        (@b, N'Shampoo Anticaspa Pro ' + CAST(@b AS NVARCHAR(5)), N'Fórmula de cuidado avanzado con zinc para control de caspa y cuero cabelludo.', 14900, 35, N'SHAMPOO', N'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Shampoo Fortificante Biotina ' + CAST(@b AS NVARCHAR(5)), N'Shampoo enriquecido con biotina y queratina para fortalecer la raíz.', 16500, 30, N'SHAMPOO', N'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Shampoo Uso Diario Herbal ' + CAST(@b AS NVARCHAR(5)), N'Extractos de romero y mentol para una limpieza fresca diaria.', 12900, 40, N'SHAMPOO', N'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Shampoo Purificante Carbón ' + CAST(@b AS NVARCHAR(5)), N'Carbón activado para desintoxicar residuos de pomadas y gel.', 15900, 25, N'SHAMPOO', N'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Bálsamo Post-Afeitado Ice ' + CAST(@b AS NVARCHAR(5)), N'Efecto frío calmante para evitar irritación post navaja.', 11900, 40, N'BALSAMO', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Bálsamo Acondicionador Barba ' + CAST(@b AS NVARCHAR(5)), N'Manteca de karité y cera de abeja para moldear e hidratar la barba.', 13900, 35, N'BALSAMO', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Bálsamo Regenerador Dérmico ' + CAST(@b AS NVARCHAR(5)), N'Vitamina B5 y caléndula para pieles ultra sensibles.', 14500, 20, N'BALSAMO', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Bálsamo Hidratante Capilar ' + CAST(@b AS NVARCHAR(5)), N'Acondicionador para suavizar cabello rebelde y seco.', 12500, 30, N'BALSAMO', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Crema de Afeitar Eucalipto ' + CAST(@b AS NVARCHAR(5)), N'Espuma densa y lubricación superior para afeitado suave.', 9900, 50, N'CREMA_BARBEAR', N'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Crema de Afeitar Piel Sensible ' + CAST(@b AS NVARCHAR(5)), N'Sin alcohol ni fragancias artificiales con extracto de avena.', 10900, 45, N'CREMA_BARBEAR', N'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Gel de Afeitado Transparente ' + CAST(@b AS NVARCHAR(5)), N'Permite ver los contornos de la barba durante el afeitado con navaja.', 11500, 40, N'CREMA_BARBEAR', N'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Jabón de Afeitar Tradicional ' + CAST(@b AS NVARCHAR(5)), N'Pastilla de jabón artesanal para aplicar con brocha clásica.', 8900, 30, N'CREMA_BARBEAR', N'https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Crema Modeladora Efecto Mate ' + CAST(@b AS NVARCHAR(5)), N'Fijación flexible sin brillo para estilos naturales desenfadados.', 13900, 35, N'CREMA_MODELADORA', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Crema Texturizadora de Rizos ' + CAST(@b AS NVARCHAR(5)), N'Define rizos y ondas masculinas eliminando el frizz.', 14500, 25, N'CREMA_MODELADORA', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Grooming Cream Clásica ' + CAST(@b AS NVARCHAR(5)), N'Crema ligera para peinar cabellos finos aportando cuerpo.', 12900, 30, N'CREMA_MODELADORA', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Pasta Fibrosa Modeladora ' + CAST(@b AS NVARCHAR(5)), N'Fibras elásticas que añaden textura y fijación media duradera.', 15200, 20, N'CREMA_MODELADORA', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Aceite Barba Argán & Jojoba ' + CAST(@b AS NVARCHAR(5)), N'Hidratación profunda para evitar la resequedad y picazón (30ml).', 18500, 40, N'ACEITE_BARBA', N'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Aceite Barba Esencia Cedro ' + CAST(@b AS NVARCHAR(5)), N'Fragancia maderosa masculina que nutre folículos gruesos (30ml).', 19500, 30, N'ACEITE_BARBA', N'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Aceite Barba Cítrico Energizante ' + CAST(@b AS NVARCHAR(5)), N'Notas de lima y naranja para una barba fresca y brillante.', 17900, 25, N'ACEITE_BARBA', N'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Serum Crecimiento de Barba ' + CAST(@b AS NVARCHAR(5)), N'Activos naturales que estimulan las zonas con menor densidad.', 22900, 15, N'ACEITE_BARBA', N'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Cera Fijación Extrema Mate ' + CAST(@b AS NVARCHAR(5)), N'Fijación resistente todo el día, ideal para climas calurosos.', 12500, 45, N'CERA_CABELLO', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Pomada Base Agua Brillo Alto ' + CAST(@b AS NVARCHAR(5)), N'Estilo clásico pompadour o slick back, se remueve con agua fácil.', 13900, 50, N'CERA_CABELLO', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Polvo Voluminizador Capilar ' + CAST(@b AS NVARCHAR(5)), N'Polvo matificante para dar volumen instantáneo en la raíz.', 11900, 35, N'CERA_CABELLO', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Cera Arcilla Moldeable Clay ' + CAST(@b AS NVARCHAR(5)), N'Acabado semi-mate con textura fuerte retrabajable.', 14900, 25, N'CERA_CABELLO', N'https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Loción Aftershave Colonia Clásica ' + CAST(@b AS NVARCHAR(5)), N'Aroma barbero clásico con alcohol astringente y antiséptico.', 13900, 30, N'LOCION', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Tónico Capilar Mentolado ' + CAST(@b AS NVARCHAR(5)), N'Estimulante del cuero cabelludo para aplicar antes del peinado.', 12900, 25, N'LOCION', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Loción Facial Matificante ' + CAST(@b AS NVARCHAR(5)), N'Controla el exceso de grasa en la frente y zona T durante el día.', 11500, 20, N'LOCION', N'https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80', 1),

        (@b, N'Cepillo Barba Cerdas Jabalí ' + CAST(@b AS NVARCHAR(5)), N'Cepillo de madera natural para desenredar y distribuir aceites.', 9900, 25, N'OTRO', N'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Peine Metálico Antiestático ' + CAST(@b AS NVARCHAR(5)), N'Dientes anchos y finos pulidos para cortes de precisión.', 7900, 40, N'OTRO', N'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=400&q=80', 1),
        (@b, N'Neceser de Aseo Masculino ' + CAST(@b AS NVARCHAR(5)), N'Bolso de viaje impermeable con compartimentos para productos.', 18900, 15, N'OTRO', N'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=400&q=80', 1);

    SET @b = @b + 1;
END;
GO

-- =====================================================================
-- PASO 7: 50 Clientes (10 clientes por cada uno de los 5 días)
-- Clave: Cliente123!
-- Hash: $2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi
-- =====================================================================
DECLARE @i INT = 1;
WHILE @i <= 50
BEGIN
    DECLARE @doc NVARCHAR(20) = N'300000' + RIGHT('00' + CAST(@i AS NVARCHAR(2)), 2) + N'-' + CAST((@i % 9) + 1 AS NVARCHAR(1));
    DECLARE @mail NVARCHAR(150) = N'cliente' + CAST(@i AS NVARCHAR(3)) + N'@test.cl';
    DECLARE @nombre NVARCHAR(100) = CASE (@i % 10)
        WHEN 0 THEN N'Fernando' WHEN 1 THEN N'Matías' WHEN 2 THEN N'Joaquín'
        WHEN 3 THEN N'Ignacio'  WHEN 4 THEN N'Lucas'  WHEN 5 THEN N'Gabriel'
        WHEN 6 THEN N'Vicente'  WHEN 7 THEN N'Martín' WHEN 8 THEN N'Rodrigo'
        ELSE N'Eduardo' END;
    DECLARE @apellido NVARCHAR(100) = CASE (@i % 10)
        WHEN 0 THEN N'Garrido'  WHEN 1 THEN N'Riquelme' WHEN 2 THEN N'Bustamante'
        WHEN 3 THEN N'Poblete'  WHEN 4 THEN N'Salinas'  WHEN 5 THEN N'Concha'
        WHEN 6 THEN N'Henríquez' WHEN 7 THEN N'Vergara' WHEN 8 THEN N'Aravena'
        ELSE N'Cornejo' END;
    DECLARE @tel NVARCHAR(20) = N'+569300000' + RIGHT('00' + CAST(@i AS NVARCHAR(2)), 2);

    INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo)
    VALUES (@doc, N'RUT', @nombre, @apellido, @mail, @tel, N'$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi', N'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80', N'es', 'CL');

    INSERT INTO clientes (id, puntos_fidelidad)
    SELECT p.id, (@i * 10) % 250 FROM personas p WHERE p.email = @mail;

    SET @i = @i + 1;
END;
GO

-- =====================================================================
-- PASO 8: Inyección de 50 Citas (10 citas por día durante 5 días consecutivos)
-- Rango de fechas: CAST(GETDATE() AS DATE) hasta DATEADD(DAY, 4, CAST(GETDATE() AS DATE))
-- Cada barbero atiende en horarios distintos evitando colisión de ux_citas_barbero_slot
-- =====================================================================
DECLARE @diaOffset INT = 0;
DECLARE @citaNum INT = 1;

WHILE @diaOffset < 5
BEGIN
    DECLARE @fechaCita DATE = DATEADD(DAY, @diaOffset, CAST(GETDATE() AS DATE));
    DECLARE @cEnDia INT = 1;

    WHILE @cEnDia <= 10
    BEGIN
        -- Determinar cliente (1 a 50)
        DECLARE @clienteEmail NVARCHAR(150) = N'cliente' + CAST(@citaNum AS NVARCHAR(3)) + N'@test.cl';
        DECLARE @clienteId INT = (SELECT id FROM personas WHERE email = @clienteEmail);

        -- Distribuir entre las 5 barberías
        DECLARE @barbId INT = ((@cEnDia - 1) % 5) + 1;

        -- Seleccionar uno de los 5 barberos de esa barbería
        DECLARE @barberoId INT;
        SELECT TOP 1 @barberoId = e.id
        FROM empleados e
        WHERE e.barberia_id = @barbId AND e.rol = N'BARBERO'
        ORDER BY e.id;

        -- Ajustar el barbero específico según slot para asegurar diversidad
        IF (@cEnDia > 5)
        BEGIN
            SELECT @barberoId = e.id
            FROM (SELECT e.id, ROW_NUMBER() OVER(ORDER BY e.id) as rn FROM empleados e WHERE e.barberia_id = @barbId AND e.rol = N'BARBERO') e
            WHERE rn = 2;
        END

        -- Horario de la cita (de 09:30 a 18:30)
        DECLARE @horaCita TIME(0) = CASE @cEnDia
            WHEN 1 THEN '09:30:00'
            WHEN 2 THEN '10:30:00'
            WHEN 3 THEN '11:30:00'
            WHEN 4 THEN '12:30:00'
            WHEN 5 THEN '14:00:00'
            WHEN 6 THEN '15:00:00'
            WHEN 7 THEN '16:00:00'
            WHEN 8 THEN '17:00:00'
            WHEN 9 THEN '18:00:00'
            ELSE '18:30:00' END;

        -- Estado de la cita: Si es hoy, algunas confirmadas o pendientes; días siguientes confirmadas/pendientes
        DECLARE @estadoCita NVARCHAR(20) = CASE 
            WHEN @diaOffset = 0 AND @cEnDia <= 3 THEN N'COMPLETADA'
            WHEN @cEnDia % 3 = 0 THEN N'PENDIENTE'
            ELSE N'CONFIRMADA' END;

        -- Servicio de la barbería correspondiente
        DECLARE @servId INT = (SELECT TOP 1 id FROM servicios WHERE barberia_id = @barbId ORDER BY id);
        DECLARE @precioSrv DECIMAL(10,2) = (SELECT precio FROM servicios WHERE id = @servId);

        -- Insertar cita
        INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
        VALUES (@barbId, @clienteId, @barberoId, @fechaCita, @horaCita, @estadoCita, @precioSrv, N'Cita programada día ' + CAST(@diaOffset + 1 AS NVARCHAR(2)) + N' servicio ' + CAST(@servId AS NVARCHAR(5)));

        DECLARE @nuevaCitaId INT = SCOPE_IDENTITY();

        -- Relacionar servicio con cita
        INSERT INTO cita_servicios (cita_id, servicio_id)
        VALUES (@nuevaCitaId, @servId);

        -- Si quedó completada, agregar calificación de prueba
        IF @estadoCita = N'COMPLETADA'
        BEGIN
            INSERT INTO calificaciones (cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario)
            VALUES (@nuevaCitaId, @clienteId, @barbId, @barberoId, 5, 5, N'Excelente servicio y puntualidad.');
        END;

        SET @cEnDia = @cEnDia + 1;
        SET @citaNum = @citaNum + 1;
    END;

    SET @diaOffset = @diaOffset + 1;
END;
GO

-- =====================================================================
-- PASO 9: Suscripción activa para Cliente 1 en Barbería 1
-- =====================================================================
DECLARE @c1 INT = (SELECT id FROM personas WHERE email = N'cliente1@test.cl');
DECLARE @s1 INT = (SELECT TOP 1 id FROM suscripciones WHERE barberia_id = 1 AND nombre LIKE N'%VIP%');

IF @c1 IS NOT NULL AND @s1 IS NOT NULL
BEGIN
    INSERT INTO cliente_suscripciones (cliente_id, suscripcion_id, fecha_inicio, fecha_vencimiento, activa)
    VALUES (@c1, @s1, CAST(GETDATE() AS DATE), DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), 1);
END;
GO

SELECT 
    (SELECT COUNT(*) FROM barberias) AS total_barberias,
    (SELECT COUNT(*) FROM barberias WHERE suscripciones_habilitadas = 1) AS barberias_con_suscripcion,
    (SELECT COUNT(*) FROM empleados WHERE rol = 'ADMIN_BARBERIA') AS total_admins,
    (SELECT COUNT(*) FROM empleados WHERE rol = 'BARBERO') AS total_barberos,
    (SELECT COUNT(*) FROM clientes) AS total_clientes,
    (SELECT COUNT(*) FROM productos) AS total_productos,
    (SELECT COUNT(*) FROM citas) AS total_citas;
GO
