package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.SuperAdmin;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Acceso a datos para el inicio de sesión y gestión de SuperAdministradores.
 */
public class SuperAdminDAO {

    private static final String SQL_BUSCAR_POR_EMAIL =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, p.fecha_registro, p.activo " +
            "FROM personas p " +
            "INNER JOIN superadmins s ON p.id = s.id " +
            "WHERE p.email = ? AND p.activo = 1";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, p.fecha_registro, p.activo " +
            "FROM personas p " +
            "INNER JOIN superadmins s ON p.id = s.id " +
            "WHERE p.id = ? AND p.activo = 1";

    public SuperAdmin buscarPorEmail(String email) throws SQLException {
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(SQL_BUSCAR_POR_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapearFila(rs);
            }
        }
    }

    public SuperAdmin buscarPorId(int id) throws SQLException {
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapearFila(rs);
            }
        }
    }

    private SuperAdmin mapearFila(ResultSet rs) throws SQLException {
        return new SuperAdmin(
                rs.getInt("id"),
                rs.getString("documento_identidad"),
                rs.getString("tipo_documento"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getString("password_hash"),
                rs.getString("foto_url"),
                rs.getString("idioma_preferido"),
                rs.getString("pais_codigo"),
                rs.getString("auth_provider"),
                rs.getBoolean("mfa_habilitado"),
                rs.getTimestamp("fecha_registro").toLocalDateTime(),
                rs.getBoolean("activo")
        );
    }
}
