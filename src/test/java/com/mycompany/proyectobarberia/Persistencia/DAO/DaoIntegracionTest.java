package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.API.dto.GananciaBarbero;
import com.mycompany.proyectobarberia.Logica.Barberia;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de los DAO nuevos contra una base real con los datos de prueba
 * (database/schema.sql en SQL Server o database/postgres/*.sql en PostgreSQL/Supabase).
 *
 * Se ejecutan solo si RUN_DB_TESTS=true y DB_URL/DB_USER/DB_PASSWORD apuntan a una base
 * de PRUEBAS (crean y eliminan un cliente temporal):
 *   RUN_DB_TESTS=true DB_URL=... DB_USER=... DB_PASSWORD=... mvn test
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class DaoIntegracionTest {

    @AfterAll
    static void cerrar() {
        Conexion.cerrarPool();
    }

    @Test
    void busquedaDeBarberiasEsInsensibleAMayusculas() throws Exception {
        BarberiaDAO dao = new BarberiaDAO();
        List<Barberia> r = dao.buscarActivas("CABALLERO");
        assertFalse(r.isEmpty());
        assertTrue(r.get(0).getNombre().contains("Caballero"));
        assertTrue(dao.buscarActivas("providencia").size() >= 1, "busca también por dirección");
        assertTrue(dao.buscarActivas("%").isEmpty(), "el comodín se trata como literal");
        assertEquals(dao.listarActivas().size(), dao.buscarActivas("  ").size());
    }

    @Test
    void toggleDeSuscripcionesPersiste() throws Exception {
        BarberiaDAO dao = new BarberiaDAO();
        Barberia b = dao.listarActivas().get(0);
        boolean original = b.isSuscripcionesHabilitadas();
        try {
            assertTrue(dao.actualizarSuscripcionesHabilitadas(b.getId(), !original));
            assertEquals(!original, dao.buscarPorId(b.getId()).isSuscripcionesHabilitadas());
        } finally {
            dao.actualizarSuscripcionesHabilitadas(b.getId(), original);
        }
        assertFalse(dao.actualizarSuscripcionesHabilitadas(-1, true));
    }

    @Test
    void gananciasCuentanSoloCitasCompletadas() throws Exception {
        int barberiaId = new BarberiaDAO().listarActivas().get(0).getId();
        LocalDate hoy = LocalDate.now();
        GananciaDAO dao = new GananciaDAO();

        // Datos de prueba: una cita COMPLETADA de 15000 hace 3 días (Juan); el resto pendientes/canceladas
        List<GananciaBarbero> mes = dao.porBarbero(barberiaId, hoy.minusDays(10), hoy.plusDays(10), null);
        BigDecimal total = mes.stream().map(GananciaBarbero::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("15000").compareTo(total));
        assertEquals(1, mes.stream().mapToInt(GananciaBarbero::citas).sum());
        assertTrue(mes.size() >= 2, "incluye barberos activos aunque tengan 0");

        GananciaBarbero top = mes.get(0);
        assertEquals("Juan", top.nombre());
        assertEquals(1, dao.porBarbero(barberiaId, hoy.minusDays(10), hoy, top.empleadoId()).size());

        var serie = dao.serieDiaria(barberiaId, hoy.minusDays(10), hoy, null);
        assertEquals(1, serie.size());
        assertEquals(hoy.minusDays(3), serie.get(0).fecha());

        assertTrue(dao.serieDiaria(barberiaId, hoy, hoy, null).isEmpty(), "hoy solo hay citas pendientes");
    }

    @Test
    void recuperacionDeContrasenaDeUnSoloUso() throws Exception {
        ClienteDAO clientes = new ClienteDAO();
        RecuperacionDAO dao = new RecuperacionDAO();
        String sufijo = String.valueOf(System.nanoTime());
        String email = "recupera" + sufijo + "@test.cl";
        int id = clientes.registrarCliente(new Cliente("R" + sufijo.substring(sufijo.length() - 9), "OTRO",
                "Rita", "Prueba", email, null, "$2a$12$viejo", "CL"));

        assertNull(dao.buscarPersonaActiva("noexiste" + sufijo + "@test.cl"));
        RecuperacionDAO.PersonaRecuperable p = dao.buscarPersonaActiva(email);
        assertEquals(id, p.id());

        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        int primero = dao.crearCodigo(id, "$2a$10$hash1", ahora, ahora.plusMinutes(15));
        int segundo = dao.crearCodigo(id, "$2a$10$hash2", ahora, ahora.plusMinutes(15));
        assertEquals(2, dao.contarSolicitudesDesde(id, ahora.minusMinutes(1)));
        assertEquals(0, dao.contarSolicitudesDesde(id, ahora.plusMinutes(1)));

        RecuperacionDAO.Codigo vigente = dao.buscarVigente(id);
        assertEquals(segundo, vigente.id(), "pedir otro código invalida el anterior");
        assertEquals("$2a$10$hash2", vigente.codigoHash());
        assertEquals(ahora.plusMinutes(15), vigente.expira());

        dao.registrarIntentoFallido(segundo);
        assertEquals(1, dao.buscarVigente(id).intentos());

        assertFalse(dao.restablecerPassword(primero, id, "$2a$12$nuevo"), "el código reemplazado no sirve");
        assertFalse(dao.restablecerPassword(segundo, id + 1, "$2a$12$nuevo"), "código de otra persona");
        assertTrue(dao.restablecerPassword(segundo, id, "$2a$12$nuevo"));
        assertFalse(dao.restablecerPassword(segundo, id, "$2a$12$otro"), "un solo uso");
        assertNull(dao.buscarVigente(id));
        assertEquals("$2a$12$nuevo", clientes.buscarPorEmail(email).getPasswordHash());

        // Una cuenta eliminada no puede recuperar contraseña
        clientes.eliminarCuenta(id, LocalDate.now());
        assertNull(dao.buscarPersonaActiva(email));
    }

    @Test
    void eliminarCuentaAnonimizaYCancelaCitasFuturas() throws Exception {
        ClienteDAO clientes = new ClienteDAO();
        String sufijo = String.valueOf(System.nanoTime());
        String email = "temporal" + sufijo + "@test.cl";
        int id = clientes.registrarCliente(new Cliente("T" + sufijo.substring(sufijo.length() - 9), "OTRO",
                "Temporal", "Prueba", email, "+56900000000", "$2a$12$abc", "CL"));

        int barberiaId = new BarberiaDAO().listarActivas().get(0).getId();
        int empleadoId = new GananciaDAO().porBarbero(barberiaId, LocalDate.now(), LocalDate.now(), null).get(0).empleadoId();
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, precio_total) VALUES (?,?,?,?,?, 'PENDIENTE', 1000)")) {
            ps.setInt(1, barberiaId);
            ps.setInt(2, id);
            ps.setInt(3, empleadoId);
            ps.setDate(4, java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
            ps.setTime(5, java.sql.Time.valueOf("08:15:00"));
            ps.executeUpdate();
        }

        assertTrue(clientes.eliminarCuenta(id, LocalDate.now()));
        assertNull(clientes.buscarPorEmail(email), "ya no puede iniciar sesión");
        assertNull(clientes.buscarPorId(id));
        assertFalse(clientes.eliminarCuenta(id, LocalDate.now()), "segunda eliminación no hace nada");

        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT p.email, p.nombre, p.telefono, p.activo, ci.estado FROM personas p JOIN citas ci ON ci.cliente_id = p.id WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("eliminado+" + id + "@cuenta.invalid", rs.getString("email"));
                assertEquals("Usuario", rs.getString("nombre"));
                assertNull(rs.getString("telefono"));
                assertEquals(0, rs.getInt("activo"));
                assertEquals("CANCELADA", rs.getString("estado"));
            }
        }
        // El email original queda libre para volver a registrarse
        int nuevo = clientes.registrarCliente(new Cliente("N" + sufijo.substring(sufijo.length() - 9), "OTRO",
                "Temporal", "Nuevo", email, null, "$2a$12$abc", "CL"));
        assertNotEquals(id, nuevo);
    }
}
