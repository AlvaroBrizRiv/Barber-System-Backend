package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Producto;
import com.mycompany.proyectobarberia.Logica.enums.CategoriaProducto;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.ProductoDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para la tienda de productos de aseo masculino.
 *
 * GET    /api/productos/barberia/{id}              → Catálogo completo (público).
 * GET    /api/productos/barberia/{id}/categoria/{c}→ Filtrado por categoría (público).
 * POST   /api/productos                            → Agregar producto (ADMIN).
 * PUT    /api/productos/{id}                       → Actualizar producto (ADMIN).
 * DELETE /api/productos/{id}                       → Desactivar producto (ADMIN).
 */
public class ProductoController {

    private static final ProductoDAO dao = new ProductoDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}",                    ProductoController::listar);
        ApiBuilder.get("/barberia/{id}/categoria/{cat}",    ProductoController::listarPorCategoria);
        ApiBuilder.post("/",                                ProductoController::crear);
        ApiBuilder.put("/{id}",                             ProductoController::actualizar);
        ApiBuilder.delete("/{id}",                          ProductoController::desactivar);
    }

    /**
     * Devuelve el catálogo público completo de productos de una barbería.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de base de datos
     */
    private static void listar(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(dao.listarPorBarberia(barberiaId));
    }

    /**
     * Devuelve el catálogo filtrado por una categoría específica.
     * Utilizado para filtros en la tienda.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de base de datos
     * @throws BadRequestResponse si la categoría proveída es inválida
     */
    private static void listarPorCategoria(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        CategoriaProducto cat;
        try {
            cat = CategoriaProducto.valueOf(ctx.pathParam("cat").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Categoría inválida.");
        }
        ctx.json(dao.listarPorCategoria(barberiaId, cat));
    }

    /**
     * Agrega un nuevo producto al catálogo. Operación exclusiva de ADMIN.
     * El barberiaId proviene de manera segura desde el JWT.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error al insertar
     * @throws BadRequestResponse si el nombre o el precio son inválidos
     */
    private static void crear(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        Producto producto = ctx.bodyAsClass(Producto.class);
        producto.setBarberiaId(barberiaId);

        if (producto.getNombre() == null || producto.getPrecio() <= 0) {
            throw new BadRequestResponse("Nombre y precio son requeridos.");
        }

        int id = dao.crear(producto);
        ctx.status(201).json(Map.of("id", id, "mensaje", "Producto agregado al catálogo."));
    }

    /**
     * Actualiza los detalles de un producto existente en el catálogo.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de base de datos
     * @throws NotFoundResponse si el producto a actualizar no existe o no pertenece a la barbería
     */
    private static void actualizar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);
        int productoId = Integer.parseInt(ctx.pathParam("id"));

        Producto producto = ctx.bodyAsClass(Producto.class);
        producto.setId(productoId);
        producto.setBarberiaId(barberiaId);

        if (!dao.actualizar(producto)) throw new NotFoundResponse("Producto no encontrado.");
        ctx.json(Map.of("mensaje", "Producto actualizado correctamente."));
    }

    /**
     * Desactiva un producto del catálogo de la barbería lógicamente (soft-delete).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws NotFoundResponse si no se encuentra el producto
     */
    private static void desactivar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        int productoId = Integer.parseInt(ctx.pathParam("id"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        if (!dao.desactivar(productoId, barberiaId))
            throw new NotFoundResponse("Producto no encontrado.");
        ctx.json(Map.of("mensaje", "Producto desactivado del catálogo."));
    }
}
