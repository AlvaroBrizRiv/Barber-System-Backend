package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.CarritoDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el carrito de compras.
 *
 * Todos los endpoints requieren autenticación como CLIENTE.
 * El carrito está aislado por cliente_id + barberia_id (multi-tenant).
 *
 * GET    /api/carrito/{barberiaId}               → Ver ítems del carrito.
 * POST   /api/carrito/{barberiaId}               → Agregar ítem.
 * PUT    /api/carrito/{barberiaId}/{productoId}   → Actualizar cantidad.
 * DELETE /api/carrito/{barberiaId}/{productoId}   → Eliminar ítem.
 * POST   /api/carrito/{barberiaId}/checkout       → Finalizar compra (vacía el carrito).
 */
public class CarritoController {

    private CarritoController() {
        // Constructor privado para evitar instanciación
    }

    private static final String BARBERIA_ID_PARAM = "barberiaId";
    private static final CarritoDAO dao = new CarritoDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/{" + BARBERIA_ID_PARAM + "}",                  CarritoController::obtener);
        ApiBuilder.post("/{" + BARBERIA_ID_PARAM + "}",                 CarritoController::agregar);
        ApiBuilder.put("/{" + BARBERIA_ID_PARAM + "}/{productoId}",     CarritoController::actualizarCantidad);
        ApiBuilder.delete("/{" + BARBERIA_ID_PARAM + "}/{productoId}",  CarritoController::eliminarItem);
        ApiBuilder.post("/{" + BARBERIA_ID_PARAM + "}/checkout",        CarritoController::checkout);
    }

    /**
     * Obtiene todos los ítems del carrito de un cliente para una barbería específica.
     * 
     * @param ctx Contexto HTTP de Javalin con el ID de la barbería
     * @throws SQLException si ocurre un error en la base de datos
     * @throws io.javalin.http.ForbiddenResponse si el usuario no es CLIENTE
     */
    private static void obtener(Context ctx) throws SQLException {
        // Verifica que el rol sea exactamente CLIENTE (BUG-06)
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId  = SecurityMiddleware.getUserId(ctx);
        int barberiaId = Integer.parseInt(ctx.pathParam(BARBERIA_ID_PARAM));

        List<Map<String, Object>> items = dao.obtenerCarrito(clienteId, barberiaId);
        double total = items.stream()
                .mapToDouble(i -> (double) i.get("subtotal"))
                .sum();

        ctx.json(Map.of("items", items, "total", total, "cantidad", items.size()));
    }

    /**
     * Agrega un producto al carrito del cliente.
     * 
     * @param ctx Contexto HTTP de Javalin con los datos del producto
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si faltan datos o las cantidades son inválidas
     */
    @SuppressWarnings("unchecked")
    private static void agregar(Context ctx) throws SQLException {
        // Verifica que el rol sea exactamente CLIENTE (BUG-06)
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId  = SecurityMiddleware.getUserId(ctx);
        int barberiaId = Integer.parseInt(ctx.pathParam(BARBERIA_ID_PARAM));

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int productoId = leerEntero(body, "productoId", true, 0);
        int cantidad   = leerEntero(body, "cantidad", false, 1);
        if (productoId <= 0) throw new BadRequestResponse("productoId inválido.");
        if (cantidad   <= 0) throw new BadRequestResponse("La cantidad debe ser mayor a 0.");
        

        try {
            dao.agregarItem(clienteId, barberiaId, productoId, cantidad);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
        ctx.status(201).json(Map.of("mensaje", "Producto agregado al carrito."));
    }

    /**
     * Actualiza la cantidad de un producto ya existente en el carrito.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     */
    @SuppressWarnings("unchecked")
    private static void actualizarCantidad(Context ctx) throws SQLException {
        // Verifica que el rol sea exactamente CLIENTE (BUG-06)
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId  = SecurityMiddleware.getUserId(ctx);
        int barberiaId = Integer.parseInt(ctx.pathParam(BARBERIA_ID_PARAM));
        int productoId = Integer.parseInt(ctx.pathParam("productoId"));

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int cantidad = leerEntero(body, "cantidad", true, 0);

        dao.actualizarCantidad(clienteId, barberiaId, productoId, cantidad);
        ctx.json(Map.of("mensaje", "Cantidad actualizada."));
        
    }

    /**
     * Elimina un producto específico del carrito del cliente.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     */
    private static void eliminarItem(Context ctx) throws SQLException {
        // Verifica que el rol sea exactamente CLIENTE (BUG-06)
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId  = SecurityMiddleware.getUserId(ctx);
        int barberiaId = Integer.parseInt(ctx.pathParam(BARBERIA_ID_PARAM));
        int productoId = Integer.parseInt(ctx.pathParam("productoId"));

        dao.eliminarItem(clienteId, barberiaId, productoId);
        ctx.json(Map.of("mensaje", "Producto eliminado del carrito.")); 
    }

    /**
     * Finaliza la compra verificando stock y vaciando el carrito de forma atómica (SEC-04).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si el carrito está vacío
     */
    private static void checkout(Context ctx) throws SQLException {
        // Verifica que el rol sea exactamente CLIENTE (BUG-06)
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId  = SecurityMiddleware.getUserId(ctx);
        int barberiaId = Integer.parseInt(ctx.pathParam(BARBERIA_ID_PARAM));

        // Obtener el resumen antes de vaciar (para mostrar en la confirmación)
        List<Map<String, Object>> items = dao.obtenerCarrito(clienteId, barberiaId);
        if (items.isEmpty()) {
            throw new BadRequestResponse("El carrito está vacío.");
        }

        double total = items.stream().mapToDouble(i -> (double) i.get("subtotal")).sum();

        // Checkout atómico con verificación de stock (SEC-04)
        try {
            dao.checkout(clienteId, barberiaId);
        } catch (IllegalStateException e) {
            throw new ConflictResponse(e.getMessage());
        }

        ctx.status(200).json(Map.of("mensaje", "Compra realizada exitosamente.", "totalCobrado", total, "itemsComprados", items.size())); 
    }
    
    private static int leerEntero(Map<String, Object> body, String clave, boolean obligatorio, int porDefecto) {

        
        Object v = body.get(clave);
        if (v == null){
            if (obligatorio) throw new BadRequestResponse("Falta el campo: " + clave);
            return porDefecto;
        }
        if (!(v instanceof Number n)) {
            throw new BadRequestResponse("El campo " + clave + " debe ser un número.");
        }
        return n.intValue();
        
        }
       
        
       
    }
        
    




