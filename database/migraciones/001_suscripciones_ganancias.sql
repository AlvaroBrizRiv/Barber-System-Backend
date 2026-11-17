-- =====================================================================
-- MIGRACIÓN 001 — SQL Server (SSMS)
-- Aplica sobre una base barberia_db existente sin perder datos.
--   1. barberias.suscripciones_habilitadas: muestra/oculta la sección
--      de suscripciones en la página pública de cada barbería.
--   2. Índice para los reportes de ganancias (citas COMPLETADA por rango).
-- Idempotente: se puede ejecutar más de una vez.
-- =====================================================================
USE barberia_db;
GO

IF COL_LENGTH('dbo.barberias', 'suscripciones_habilitadas') IS NULL
BEGIN
    ALTER TABLE barberias
        ADD suscripciones_habilitadas BIT NOT NULL
            CONSTRAINT df_barberias_subs_habilitadas DEFAULT 0;
END
GO

-- Las barberías que ya tienen planes activos conservan la sección visible
UPDATE b SET suscripciones_habilitadas = 1
FROM barberias b
WHERE EXISTS (SELECT 1 FROM suscripciones s WHERE s.barberia_id = b.id AND s.activa = 1);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_citas_ganancias' AND object_id = OBJECT_ID('dbo.citas'))
BEGIN
    CREATE INDEX idx_citas_ganancias ON citas(barberia_id, estado, fecha) INCLUDE (empleado_id, precio_total);
END
GO
