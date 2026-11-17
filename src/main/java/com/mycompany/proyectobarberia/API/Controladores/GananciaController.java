package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.API.dto.GananciaBarbero;
import com.mycompany.proyectobarberia.API.dto.GananciaDia;
import com.mycompany.proyectobarberia.API.dto.ReporteGanancias;
import com.mycompany.proyectobarberia.Logica.enums.PeriodoReporte;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.GananciaDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Reportes de ganancias (citas COMPLETADA) por día, semana o mes.
 *
 * GET /api/ganancias/barberia/{id}?periodo=DIA|SEMANA|MES&fecha=YYYY-MM-DD&barberoId=N
 *     → ADMIN_BARBERIA de esa barbería (o SUPERADMIN): total y desglose por barbero.
 * GET /api/ganancias/mias?periodo=...&fecha=...
 *     → BARBERO / ADMIN_BARBERIA: solo sus propias ganancias (ID tomado del JWT).
 */
public class GananciaController {

    private GananciaController() { }

    private static GananciaDAO dao = new GananciaDAO();

    /** Permite inyectar un DAO simulado en los tests. */
    static void setDao(GananciaDAO nuevo) { dao = nuevo; }

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}", GananciaController::deBarberia);
        ApiBuilder.get("/mias",          GananciaController::mias);
    }

    static void deBarberia(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        SecurityMiddleware.requerirAdminDeBarberia(ctx, barberiaId);

        String barberoParam = ctx.queryParam("barberoId");
        Integer barberoId = (barberoParam == null || barberoParam.isBlank()) ? null : Integer.parseInt(barberoParam);

        ctx.json(generar(ctx, barberiaId, barberoId));
    }

    static void mias(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRolExacto(ctx, Rol.BARBERO, Rol.ADMIN_BARBERIA);
        int barberiaId = SecurityMiddleware.requerirBarberiaId(ctx);
        int userId = SecurityMiddleware.getUserId(ctx);
        ctx.json(generar(ctx, barberiaId, userId));
    }

    private static ReporteGanancias generar(Context ctx, int barberiaId, Integer barberoId) throws SQLException {
        PeriodoReporte periodo = parsePeriodo(ctx.queryParam("periodo"));
        LocalDate fecha = parseFecha(ctx.queryParam("fecha"));
        PeriodoReporte.Rango rango = periodo.rango(fecha);

        List<GananciaBarbero> porBarbero = dao.porBarbero(barberiaId, rango.desde(), rango.hasta(), barberoId);
        List<GananciaDia> serie = dao.serieDiaria(barberiaId, rango.desde(), rango.hasta(), barberoId);
        return ReporteGanancias.construir(periodo, rango, porBarbero, serie);
    }

    static PeriodoReporte parsePeriodo(String valor) {
        try {
            return PeriodoReporte.desde(valor);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Periodo inválido. Usa DIA, SEMANA o MES.");
        }
    }

    static LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse("Fecha inválida. Usa el formato YYYY-MM-DD.");
        }
    }
}
