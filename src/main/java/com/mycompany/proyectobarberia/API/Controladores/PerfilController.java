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

    private PerfilController() {
        // Constructor privado para evitar instanciación
    }

    private static final ClienteDAO clienteDAO = new ClienteDAO();

    private static final String CAMPO_ID = "id";
    private static final String CAMPO_NOMBRE = "nombre";
    private static final String CAMPO_APELLIDO = "apellido";
    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_TELEFONO = "telefono";
    private static final String CAMPO_FOTO_URL = "fotoUrl";
    private static final String CAMPO_IDIOMA = "idiomaPreferido";
    private static final String CAMPO_PAIS_CODIGO = "paisCodigo";
    private static final String CAMPO_PUNTOS_FIDELIDAD = "puntosFidelidad";
    private static final String CAMPO_TIPO_DOCUMENTO = "tipoDocumento";
    private static final String CAMPO_ROL = "rol";
    private static final String CAMPO_BARBERIA_ID = "barberiaId";

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
        com.mycompany.proyectobarberia.Logica.Empleado empleado = empDAO.buscarPorEmail("CAMPO_EMAIL");
        if (empleado == null) throw new NotFoundResponse("Perfil no encontrado.");

        Map<String, Object> perfil = new HashMap<>();
        perfil.put(CAMPO_ID,              empleado.getId());
        perfil.put(CAMPO_NOMBRE,          empleado.getNombre() != null ? empleado.getNombre() : "");
        perfil.put(CAMPO_APELLIDO,        empleado.getApellido() != null ? empleado.getApellido() : "");
        perfil.put(CAMPO_EMAIL,           empleado.getEmail());
        perfil.put(CAMPO_TELEFONO,        empleado.getTelefono() != null ? empleado.getTelefono() : "");
        perfil.put(CAMPO_FOTO_URL,         empleado.getFotoUrl() != null ? empleado.getFotoUrl() : "");
        perfil.put(CAMPO_IDIOMA, empleado.getIdiomaPreferido() != null ? empleado.getIdiomaPreferido() : "es");
        perfil.put(CAMPO_PAIS_CODIGO,      empleado.getPaisCodigo());
        perfil.put(CAMPO_ROL,             empleado.getRol().name());
        perfil.put(CAMPO_BARBERIA_ID,      empleado.getBarberiaId());
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

        Map<String, String> body = ctx.bodyAsClass(Map.class);

        Map<String, String> camposPermitidos = new HashMap<>();
        if (body.containsKey(CAMPO_NOMBRE))           camposPermitidos.put(CAMPO_NOMBRE, body.get(CAMPO_NOMBRE));
        if (body.containsKey(CAMPO_APELLIDO))         camposPermitidos.put(CAMPO_APELLIDO, body.get(CAMPO_APELLIDO));
        if (body.containsKey(CAMPO_TELEFONO))         camposPermitidos.put(CAMPO_TELEFONO, body.get(CAMPO_TELEFONO));
        if (body.containsKey(CAMPO_FOTO_URL))          camposPermitidos.put(CAMPO_FOTO_URL, body.get(CAMPO_FOTO_URL));
        if (body.containsKey(CAMPO_IDIOMA))  camposPermitidos.put(CAMPO_IDIOMA, body.get(CAMPO_IDIOMA));

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
        perfil.put(CAMPO_ID,              c.getId());
        perfil.put(CAMPO_NOMBRE,          c.getNombre() != null ? c.getNombre() : "");
        perfil.put(CAMPO_APELLIDO,        c.getApellido() != null ? c.getApellido() : "");
        perfil.put(CAMPO_EMAIL,           c.getEmail());
        perfil.put(CAMPO_TELEFONO,        c.getTelefono() != null ? c.getTelefono() : "");
        perfil.put(CAMPO_FOTO_URL,         c.getFotoUrl() != null ? c.getFotoUrl() : "");
        perfil.put(CAMPO_IDIOMA, c.getIdiomaPreferido() != null ? c.getIdiomaPreferido() : "es");
        perfil.put(CAMPO_PAIS_CODIGO,      c.getPaisCodigo());
        perfil.put(CAMPO_PUNTOS_FIDELIDAD, c.getPuntosFidelidad());
        perfil.put(CAMPO_TIPO_DOCUMENTO,   c.getTipoDocumento());
        perfil.put(CAMPO_ROL,             Rol.CLIENTE.name());
        return perfil;
    }
}
