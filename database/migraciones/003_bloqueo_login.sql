-- =====================================================================
-- MIGRACIÓN 003 — SQL Server (SSMS)
-- Control de intentos fallidos de inicio de sesión por cuenta:
--   5 fallos → espera de 15 s, 10 fallos → 30 s, 15 fallos → cuenta bloqueada
--   hasta restablecer la contraseña con "¿Olvidaste tu contraseña?".
-- Idempotente: se puede ejecutar más de una vez.
-- =====================================================================
USE barberia_db;
GO

IF COL_LENGTH('dbo.personas', 'intentos_fallidos') IS NULL
    ALTER TABLE personas ADD intentos_fallidos INT NOT NULL CONSTRAINT df_personas_intentos DEFAULT 0;
GO
IF COL_LENGTH('dbo.personas', 'bloqueado_hasta') IS NULL
    ALTER TABLE personas ADD bloqueado_hasta DATETIME2 NULL;
GO
IF COL_LENGTH('dbo.personas', 'bloqueada') IS NULL
    ALTER TABLE personas ADD bloqueada BIT NOT NULL CONSTRAINT df_personas_bloqueada DEFAULT 0;
GO
