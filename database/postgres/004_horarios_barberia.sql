-- =====================================================================
-- MIGRACIÓN 004 — PostgreSQL / Supabase
-- Horario de atención por barbería y día de la semana (1 = lunes … 7 = domingo).
-- Las barberías sin filas usan el horario por defecto (09:00 a 20:00 todos los días).
-- Idempotente: se puede ejecutar más de una vez.
-- =====================================================================
CREATE TABLE IF NOT EXISTS horarios_barberia (
    barberia_id   INT      NOT NULL REFERENCES barberias(id) ON DELETE CASCADE,
    dia_semana    SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    abierto       SMALLINT NOT NULL DEFAULT 1 CHECK (abierto IN (0,1)),
    hora_apertura TIME     NULL,
    hora_cierre   TIME     NULL,
    PRIMARY KEY (barberia_id, dia_semana),
    CHECK (abierto = 0 OR (hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL AND hora_apertura < hora_cierre))
);
