-- =====================================================================
-- DATOS DE PRUEBA — PostgreSQL / Supabase
-- Mismos datos que database/schema.sql (SQL Server). Ejecutar después de
-- database/postgres/schema.sql.
-- Contraseñas: Admin1234! (superadmin) · Admin123! (admin)
--              Barbero123! (barberos) · Cliente123! (clientes)
-- =====================================================================

INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, email, telefono, password_hash, foto_url, idioma_preferido, pais_codigo) VALUES
 ('00000000-0','RUT','Super','Admin','superadmin@barberiasystem.cl','+56900000000','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewzFxSSCiIb0u/3S',NULL,'es','CL'),
 ('11111111-1','RUT','Carlos','Mendoza','admin@elcaballero.cl','+56911111111','$2a$12$ePbmiaknwKsaKlkXcqhoiuqeiSkXOCjBCU6NJW/fp/snT9M.0t7pK','https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80','es','CL'),
 ('22222222-2','RUT','Juan','Pérez','juan@elcaballero.cl','+56922222222','$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.','https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80','es','CL'),
 ('44444444-4','RUT','Miguel','Torres','miguel@elcaballero.cl','+56944444444','$2a$12$H6wFvpMmYymA61eCUjXBzOGJ0INQZXVI9.Sg/HFtPGb.ivMou7HN.','https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=200&q=80','es','CL'),
 ('33333333-3','RUT','Pedro','González','cliente@test.cl','+56933333333','$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi','https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80','es','CL'),
 ('55555555-5','RUT','Ana','Martínez','cliente2@test.cl','+56955555555','$2a$12$SIPp0pwKN2op7ecwTlW3Xeagb3ak7dTk8aNC8H8HFSzoYVVPNnxRi','https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80','es','CL');

INSERT INTO superadmins (id) SELECT id FROM personas WHERE email = 'superadmin@barberiasystem.cl';

INSERT INTO barberias (nombre, direccion, telefono, email, logo_url, descripcion, pais_codigo, moneda_codigo, zona_horaria, suscripciones_habilitadas)
VALUES ('Barbería El Caballero','Av. Providencia 1234, Santiago','+56222345678','contacto@elcaballero.cl',
        'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80',
        'Barbería de élite en el corazón de Providencia. Especialistas en corte clásico, barba y tratamientos capilares masculinos.',
        'CL','CLP','America/Santiago',1);

INSERT INTO empleados (id, barberia_id, rol, activo)
SELECT p.id, b.id, x.rol, 1
FROM (VALUES ('admin@elcaballero.cl','ADMIN_BARBERIA'),('juan@elcaballero.cl','BARBERO'),('miguel@elcaballero.cl','BARBERO')) AS x(email, rol)
JOIN personas p ON p.email = x.email
JOIN barberias b ON b.nombre = 'Barbería El Caballero';

INSERT INTO clientes (id, puntos_fidelidad)
SELECT p.id, x.pts FROM (VALUES ('cliente@test.cl',120),('cliente2@test.cl',30)) AS x(email, pts)
JOIN personas p ON p.email = x.email;

INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos)
SELECT b.id, x.* FROM barberias b, (VALUES
 ('Corte Clásico','Corte de cabello clásico con tijeras y máquina. Incluye lavado y secado con estilo.',15000,45),
 ('Barba Completa','Perfilado, recorte y arreglo de barba con toalla caliente, navaja y aceite de barba.',12000,30),
 ('Combo Premium','Corte de cabello + Barba completa. El pack más popular y completo de la casa.',25000,75),
 ('Diseño de Cejas','Delineado y perfilado de cejas masculinas para un acabado limpio y definido.',8000,20)
) AS x(nombre, descripcion, precio, duracion)
WHERE b.nombre = 'Barbería El Caballero';

INSERT INTO productos (barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url)
SELECT b.id, x.* FROM barberias b, (VALUES
 ('Aceite de Barba Premium','Aceite natural de argán y jojoba para suavizar y nutrir la barba. 30ml.',18500,25,'ACEITE_BARBA','https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=400&q=80'),
 ('Shampoo Anticaspa Pro','Fórmula de cuidado avanzado con zinc para control de caspa y cuero cabelludo graso.',14900,40,'SHAMPOO','https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=400&q=80'),
 ('Cera de Fijación Fuerte','Cera de acabado mate con fijación extrema durante todo el día. Ideal para peinados estructurados.',12500,30,'CERA_CABELLO','https://images.unsplash.com/photo-1585945411064-3f2093b5aa48?auto=format&fit=crop&w=400&q=80'),
 ('Crema de Afeitar Classic','Crema de afeitar con aloe vera y vitamina E. Para un afeitado suave y sin irritaciones.',9900,50,'CREMA_BARBEAR','https://images.unsplash.com/photo-1613987876445-fcb353b91da9?auto=format&fit=crop&w=400&q=80'),
 ('Bálsamo Post-Barba','Bálsamo calmante y refrescante con mentol y árbol de té para después del afeitado.',11000,20,'BALSAMO','https://images.unsplash.com/photo-1590439471364-192aa70c0b53?auto=format&fit=crop&w=400&q=80')
) AS x(nombre, descripcion, precio, stock, categoria, imagen_url)
WHERE b.nombre = 'Barbería El Caballero';

INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa)
SELECT b.id, x.* FROM barberias b, (VALUES
 ('Plan Mensual VIP','MENSUAL',NULL::NUMERIC,49900::NUMERIC,'Acceso ilimitado a cortes durante el mes. 20% de descuento en todos los productos de la tienda.',1),
 ('Plan por Cita','POR_CITA',9990::NUMERIC,NULL::NUMERIC,'Descuento fijo garantizado por cita. Ideal para clientes frecuentes sin cargo mensual.',1)
) AS x(nombre, tipo, ppc, pm, descripcion, activa)
WHERE b.nombre = 'Barbería El Caballero';

INSERT INTO cliente_suscripciones (cliente_id, suscripcion_id, fecha_inicio, fecha_vencimiento, activa)
SELECT p.id, s.id, CURRENT_DATE, CURRENT_DATE + 30, 1
FROM personas p JOIN suscripciones s ON s.nombre = 'Plan Mensual VIP'
WHERE p.email = 'cliente@test.cl';

INSERT INTO carritos_items (cliente_id, barberia_id, producto_id, cantidad)
SELECT p.id, pr.barberia_id, pr.id, x.cant
FROM (VALUES ('Aceite de Barba Premium',1),('Cera de Fijación Fuerte',2)) AS x(prod, cant)
JOIN productos pr ON pr.nombre = x.prod
JOIN personas p ON p.email = 'cliente@test.cl';

INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total, notas)
SELECT b.id, cli.id, emp.id, CURRENT_DATE + x.dias, x.hora::TIME, x.estado, x.precio, x.notas
FROM (VALUES
 ('cliente@test.cl','juan@elcaballero.cl',1,'10:00','CONFIRMADA',15000,'Prefiere tijeras, no máquina.'),
 ('cliente2@test.cl','juan@elcaballero.cl',0,'15:30','PENDIENTE',25000,'Primera visita. Combo Premium.'),
 ('cliente@test.cl','miguel@elcaballero.cl',0,'17:00','PENDIENTE',12000,'Perfilado de barba.'),
 ('cliente@test.cl','juan@elcaballero.cl',-3,'11:00','COMPLETADA',15000,'Corte clásico.'),
 ('cliente@test.cl','miguel@elcaballero.cl',-7,'09:30','CANCELADA',0,'Cancelada por el cliente por viaje.')
) AS x(cli, emp, dias, hora, estado, precio, notas)
JOIN personas cli ON cli.email = x.cli
JOIN personas emp ON emp.email = x.emp
JOIN barberias b ON b.nombre = 'Barbería El Caballero';

INSERT INTO cita_servicios (cita_id, servicio_id)
SELECT c.id, s.id
FROM (VALUES
 ('Prefiere tijeras, no máquina.','Corte Clásico'),
 ('Primera visita. Combo Premium.','Combo Premium'),
 ('Perfilado de barba.','Barba Completa'),
 ('Corte clásico.','Corte Clásico'),
 ('Cancelada por el cliente por viaje.','Combo Premium')
) AS x(notas, servicio)
JOIN citas c ON c.notas = x.notas
JOIN servicios s ON s.nombre = x.servicio;

INSERT INTO calificaciones (cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario)
SELECT c.id, c.cliente_id, c.barberia_id, c.empleado_id, 5, 5, '¡Excelente servicio! Juan es muy profesional, puntual y detallista.'
FROM citas c WHERE c.estado = 'COMPLETADA' LIMIT 1;
