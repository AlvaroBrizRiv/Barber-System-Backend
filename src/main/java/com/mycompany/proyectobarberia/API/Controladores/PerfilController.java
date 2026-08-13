package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.ClienteDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión del perfil del usuario autenticado.
 *
 * GET /api/perfil  → Datos del perfil propio (sin passwordHash).
 * PUT /api/perfil  → Actualizar nombre, apellido, teléfono, foto, idioma.
 *
 * Nota de seguridad PII:
 *   El passwordHash y documento_identidad nunca se devuelven en la respuesta.
 *   El email tampoco se puede modificar por este endpoint (requiere flujo
 *   aparte con verificación por correo).
 */
public class PerfilController {

    private static final ClienteDAO clienteDAO = new ClienteDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/",  PerfilController::obtenerPerfil);
        ApiBuilder.put("/",  PerfilController::actualizarPerfil);
    }

    /**
     * Devuelve el perfil del usuario autenticado (Cliente, Empleado o Admin).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en base de datos
     * @throws NotFoundResponse si no se encuentra el perfil en la base de datos
     */
    private static void obtenerPerfil(Context ctx) throws SQLException {
        SecurityMiddleware.validar(ctx);
        int userId = SecurityMiddleware.getUserId(ctx);
        Rol rol = SecurityMiddleware.getRol(ctx);

        if (rol == Rol.CLIENTE) {
            Cliente cliente = clienteDAO.buscarPorId(userId);
            if (cliente == null) throw new NotFoundResponse("Perfil no encontrado.");
            ctx.json(perfilPublico(cliente));
            return;
        }

        // Si es Empleado / Admin
        com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO empDAO = new com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO();
        com.mycompany.proyectobarberia.Logica.Empleado empleado = empDAO.buscarPorEmail("email");
        if (empleado == null) throw new NotFoundResponse("Perfil no encontrado.");

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id",              empleado.getId());
        perfil.put("nombre",          empleado.getNombre() != null ? empleado.getNombre() : "");
        perfil.put("apellido",        empleado.getApellido() != null ? empleado.getApellido() : "");
        perfil.put("email",           empleado.getEmail());
        perfil.put("telefono",        empleado.getTelefono() != null ? empleado.getTelefono() : "");
        perfil.put("fotoUrl",         empleado.getFotoUrl() != null ? empleado.getFotoUrl() : "");
        perfil.put("idiomaPreferido", empleado.getIdiomaPreferido() != null ? empleado.getIdiomaPreferido() : "es");
        perfil.put("paisCodigo",      empleado.getPaisCodigo());
        perfil.put("rol",             empleado.getRol().name());
        perfil.put("barberiaId",      empleado.getBarberiaId());
        ctx.json(perfil);
    }

    /**
     * Actualiza campos seguros del perfil del usuario autenticado.
     * Solo permite editar ciertos campos no sensibles.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de base de datos
     */
    @SuppressWarnings("unchecked")
    private static void actualizarPerfil(Context ctx) throws SQLException {
        SecurityMiddleware.validar(ctx);
        int userId = SecurityMiddleware.getUserId(ctx);
        Rol rol = SecurityMiddleware.getRol(ctx);

        Map<String, String> body = ctx.bodyAsClass(Map.class);

        Map<String, String> camposPermitidos = new HashMap<>();
        if (body.containsKey("nombre"))           camposPermitidos.put("nombre", body.get("nombre"));
        if (body.containsKey("apellido"))         camposPermitidos.put("apellido", body.get("apellido"));
        if (body.containsKey("telefono"))         camposPermitidos.put("telefono", body.get("telefono"));
        if (body.containsKey("fotoUrl"))          camposPermitidos.put("fotoUrl", body.get("fotoUrl"));
        if (body.containsKey("idiomaPreferido"))  camposPermitidos.put("idiomaPreferido", body.get("idiomaPreferido"));

        if (!camposPermitidos.isEmpty()) {
            clienteDAO.actualizarPerfil(userId, camposPermitidos);
        }

        // Devolver el perfil actualizado
        obtenerPerfil(ctx);
    }

    /**
     * Construye el mapa de respuesta del perfil de cliente SIN datos sensibles como contraseña.
     * 
     * @param c Cliente a mapear
     * @return Map con las propiedades públicas
     */
    private static Map<String, Object> perfilPublico(Cliente c) {
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id",              c.getId());
        perfil.put("nombre",          c.getNombre() != null ? c.getNombre() : "");
        perfil.put("apellido",        c.getApellido() != null ? c.getApellido() : "");
        perfil.put("email",           c.getEmail());
        perfil.put("telefono",        c.getTelefono() != null ? c.getTelefono() : "");
        perfil.put("fotoUrl",         c.getFotoUrl() != null ? c.getFotoUrl() : "");
        perfil.put("idiomaPreferido", c.getIdiomaPreferido() != null ? c.getIdiomaPreferido() : "es");
        perfil.put("paisCodigo",      c.getPaisCodigo());
        perfil.put("puntosFidelidad", c.getPuntosFidelidad());
        perfil.put("tipoDocumento",   c.getTipoDocumento());
        perfil.put("rol",             Rol.CLIENTE.name());
        return perfil;
    }
}
