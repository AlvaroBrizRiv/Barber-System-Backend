package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Producto;
import com.mycompany.proyectobarberia.Logica.enums.CategoriaProducto;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Producto (tienda de aseo masculino).
 * Todas las operaciones filtran por barberia_id para aislamiento multi-tenant.
 */
public class ProductoDAO {

    private static final String SQL_LISTAR_POR_BARBERIA =
            "SELECT id, barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url, activo " +
            "FROM productos WHERE barberia_id = ? AND activo = TRUE ORDER BY categoria, nombre";

    private static final String SQL_LISTAR_POR_CATEGORIA =
            "SELECT id, barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url, activo " +
            "FROM productos WHERE barberia_id = ? AND categoria = ? AND activo = TRUE ORDER BY nombre";

    private static final String SQL_INSERT =
            "INSERT INTO productos (barberia_id, nombre, descripcion, precio, stock, categoria, imagen_url) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE productos SET nombre=?, descripcion=?, precio=?, stock=?, categoria=?, imagen_url=? " +
            "WHERE id=? AND barberia_id=?";

    private static final String SQL_REDUCIR_STOCK =
            "UPDATE productos SET stock = stock - 1 WHERE id = ? AND barberia_id = ? AND stock > 0";

    private static final String SQL_DESACTIVAR =
            "UPDATE productos SET activo = FALSE WHERE id = ? AND barberia_id = ?";

    // ─────────────────────────────────────────────────────────────────

    /**
     * Lista todos los productos activos asociados a una barbería.
     * 
     * @param barberiaId ID de la barbería
     * @return Lista de productos
     * @throws SQLException si ocurre un error en la base de datos
     */
    public List<Producto> listarPorBarberia(int barberiaId) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_BARBERIA)) {

            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Filtra y lista productos de una barbería según su categoría.
     * Utilizado para filtros en la tienda.
     * 
     * @param barberiaId ID de la barbería
     * @param categoria Categoría a buscar
     * @return Lista de productos coincidentes
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Producto> listarPorCategoria(int barberiaId, CategoriaProducto categoria) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_CATEGORIA)) {

            ps.setInt(1, barberiaId);
            ps.setString(2, categoria.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo producto en el catálogo de la barbería.
     * 
     * @param producto Objeto producto a crear
     * @return El ID generado en base de datos
     * @throws SQLException si ocurre un error de inserción
     */
    public int crear(Producto producto) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, producto.getBarberiaId());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setDouble(4, producto.getPrecio());
            ps.setInt(5, producto.getStock());
            ps.setString(6, producto.getCategoria().name());
            ps.setString(7, producto.getImagenUrl());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se generó ID para el producto.");
    }

    /**
     * Actualiza los datos de un producto.
     * El barberia_id en la consulta WHERE garantiza seguridad multi-tenant.
     * 
     * @param producto Objeto Producto modificado
     * @return true si la actualización fue exitosa, false de lo contrario
     * @throws SQLException si ocurre un error en la BD
     */
    public boolean actualizar(Producto producto) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio());
            ps.setInt(4, producto.getStock());
            ps.setString(5, producto.getCategoria().name());
            ps.setString(6, producto.getImagenUrl());
            ps.setInt(7, producto.getId());
            ps.setInt(8, producto.getBarberiaId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Reduce el stock de un producto en 1. La condición 'stock > 0' en el WHERE
     * es una protección a nivel SQL contra stock negativo (race condition safe).
     * 
     * @param productoId ID del producto
     * @param barberiaId ID de la barbería dueña del producto
     * @return true si el stock se redujo; false si no había stock o no se encontró.
     * @throws SQLException si ocurre un error de BD
     */
    public boolean venderUnidad(int productoId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_REDUCIR_STOCK)) {

            ps.setInt(1, productoId);
            ps.setInt(2, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Desactiva un producto del catálogo lógicamente (soft-delete).
     * 
     * @param productoId ID del producto
     * @param barberiaId ID de la barbería
     * @return true si el producto se desactivó, false en caso contrario
     * @throws SQLException si ocurre un error de BD
     */
    public boolean desactivar(int productoId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DESACTIVAR)) {

            ps.setInt(1, productoId);
            ps.setInt(2, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Mapea una fila obtenida en ResultSet al objeto Producto.
     * 
     * @param rs ResultSet con la fila actual
     * @return Objeto Producto poblado
     * @throws SQLException si existe algún problema leyendo los campos
     */
    private Producto mapearFila(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id"),
                rs.getInt("barberia_id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getInt("stock"),
                CategoriaProducto.valueOf(rs.getString("categoria")),
                rs.getString("imagen_url"),
                rs.getBoolean("activo")
        );
    }
}
