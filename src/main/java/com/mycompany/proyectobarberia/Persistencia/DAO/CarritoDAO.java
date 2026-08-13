package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para el carrito de compras persistente.
 *
 * El carrito se aísla por (cliente_id + barberia_id) para garantizar que
 * cada cliente tenga un carrito independiente por barbería.
 */
public class CarritoDAO {

    private static final String SQL_AGREGAR =
            "INSERT INTO carritos_items (cliente_id, barberia_id, producto_id, cantidad) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE cantidad = cantidad + VALUES(cantidad)";

    private static final String SQL_ACTUALIZAR_CANTIDAD =
            "UPDATE carritos_items SET cantidad = ? " +
            "WHERE cliente_id = ? AND barberia_id = ? AND producto_id = ?";

    private static final String SQL_ELIMINAR_ITEM =
            "DELETE FROM carritos_items WHERE cliente_id = ? AND barberia_id = ? AND producto_id = ?";

    private static final String SQL_VACIAR_CARRITO =
            "DELETE FROM carritos_items WHERE cliente_id = ? AND barberia_id = ?";

    private static final String SQL_OBTENER_CARRITO =
            "SELECT ci.producto_id, ci.cantidad, " +
            "       p.nombre, p.precio, p.imagen_url, p.stock, p.activo " +
            "FROM carritos_items ci " +
            "INNER JOIN productos p ON ci.producto_id = p.id " +
            "WHERE ci.cliente_id = ? AND ci.barberia_id = ? AND p.activo = TRUE " +
            "ORDER BY ci.fecha_agregado";

    // ─── Escritura ────────────────────────────────────────────────────

    /**
     * Agrega un ítem al carrito. Si el producto ya existe para este cliente,
     * suma la cantidad a la ya existente mediante "ON DUPLICATE KEY UPDATE".
     * 
     * @param clienteId ID del cliente dueño del carrito
     * @param barberiaId ID de la barbería
     * @param productoId ID del producto a agregar
     * @param cantidad Cantidad a sumar
     * @throws SQLException si ocurre un error de BD
     */
    public void agregarItem(int clienteId, int barberiaId, int productoId, int cantidad)
            throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_AGREGAR)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, barberiaId);
            ps.setInt(3, productoId);
            ps.setInt(4, cantidad);
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza la cantidad de un ítem específico de forma directa.
     * Si la cantidad proporcionada es <= 0, elimina el producto del carrito.
     * 
     * @param clienteId ID del cliente
     * @param barberiaId ID de la barbería
     * @param productoId ID del producto en el carrito
     * @param cantidad Nueva cantidad asignada
     * @throws SQLException si ocurre un error de BD
     */
    public void actualizarCantidad(int clienteId, int barberiaId, int productoId, int cantidad)
            throws SQLException {
        if (cantidad <= 0) {
            eliminarItem(clienteId, barberiaId, productoId);
            return;
        }
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_CANTIDAD)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, clienteId);
            ps.setInt(3, barberiaId);
            ps.setInt(4, productoId);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un ítem específico del carrito del cliente.
     * 
     * @param clienteId ID del cliente
     * @param barberiaId ID de la barbería
     * @param productoId ID del producto a quitar
     * @throws SQLException si ocurre un error de BD
     */
    public void eliminarItem(int clienteId, int barberiaId, int productoId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ELIMINAR_ITEM)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, barberiaId);
            ps.setInt(3, productoId);
            ps.executeUpdate();
        }
    }

    /**
     * Vacía completamente el carrito de un cliente para una barbería determinada.
     * Normalmente se utiliza tras completar el proceso de pago (checkout).
     * 
     * @param clienteId ID del cliente
     * @param barberiaId ID de la barbería
     * @throws SQLException si ocurre un error de base de datos
     */
    public void vaciarCarrito(int clienteId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_VACIAR_CARRITO)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, barberiaId);
            ps.executeUpdate();
        }
    }

    // ─── Lectura ──────────────────────────────────────────────────────

    /**
     * Retorna los ítems del carrito de un cliente para una barbería específica,
     * realizando un JOIN con productos para traer nombre, precio e imágenes.
     * 
     * @param clienteId ID del cliente
     * @param barberiaId ID de la barbería
     * @return Lista de Map representando los items y su metadata
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Map<String, Object>> obtenerCarrito(int clienteId, int barberiaId)
            throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_OBTENER_CARRITO)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productoId",  rs.getInt("producto_id"));
                    item.put("nombre",      rs.getString("nombre"));
                    item.put("precio",      rs.getDouble("precio"));
                    item.put("imagenUrl",   rs.getString("imagen_url"));
                    item.put("cantidad",    rs.getInt("cantidad"));
                    item.put("stock",       rs.getInt("stock"));
                    item.put("subtotal",    rs.getDouble("precio") * rs.getInt("cantidad"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
