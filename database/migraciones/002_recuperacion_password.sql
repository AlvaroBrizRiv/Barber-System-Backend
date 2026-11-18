-- =====================================================================
-- MIGRACIÓN 002 — SQL Server (SSMS)
-- Tabla para la recuperación de contraseña por código enviado al correo.
-- Idempotente: se puede ejecutar más de una vez.
-- =====================================================================
USE barberia_db;
GO

IF OBJECT_ID('dbo.recuperaciones_password', 'U') IS NULL
BEGIN
    CREATE TABLE recuperaciones_password (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        persona_id      INT             NOT NULL,
        codigo_hash     NVARCHAR(100)   NOT NULL,
        expira          DATETIME2       NOT NULL,
        intentos        INT             NOT NULL DEFAULT 0,
        usado           BIT             NOT NULL DEFAULT 0,
        fecha_creacion  DATETIME2       NOT NULL,
        CONSTRAINT fk_recuperaciones_personas FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_recuperaciones_persona ON recuperaciones_password(persona_id, usado);
END
GO
