-- =====================================================================
-- MIGRACIÓN 004 — SQL Server (SSMS)
-- Horario de atención por barbería y día de la semana (1 = lunes … 7 = domingo).
-- Las barberías sin filas usan el horario por defecto (09:00 a 20:00 todos los días);
-- el administrador lo cambia desde su panel → "Horario".
-- Idempotente: se puede ejecutar más de una vez.
-- =====================================================================
USE barberia_db;
GO

IF OBJECT_ID('dbo.horarios_barberia', 'U') IS NULL
BEGIN
    CREATE TABLE horarios_barberia (
        barberia_id     INT         NOT NULL,
        dia_semana      TINYINT     NOT NULL CONSTRAINT ck_horarios_dia CHECK (dia_semana BETWEEN 1 AND 7),
        abierto         BIT         NOT NULL DEFAULT 1,
        hora_apertura   TIME(0)     NULL,
        hora_cierre     TIME(0)     NULL,
        CONSTRAINT pk_horarios_barberia PRIMARY KEY (barberia_id, dia_semana),
        CONSTRAINT fk_horarios_barberias FOREIGN KEY (barberia_id) REFERENCES barberias(id) ON DELETE CASCADE,
        CONSTRAINT ck_horarios_rango CHECK (abierto = 0 OR (hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL AND hora_apertura < hora_cierre))
    );
END
GO
