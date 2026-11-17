package com.mycompany.proyectobarberia.API.Seguridad;

import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/** JWT_SECRET de pruebas se define en la configuración de surefire (pom.xml). */
class JwtUtilsTest {

    private static final String SECRET_PRUEBAS = "clave_de_pruebas_unitarias_con_mas_de_32_caracteres";

    @Test
    void generaYValidaTokenDeEmpleado() {
        String token = JwtUtils.generarToken(7, Rol.ADMIN_BARBERIA, 3);
        Claims c = JwtUtils.validarToken(token);
        assertEquals(7, JwtUtils.getUserId(c));
        assertEquals(Rol.ADMIN_BARBERIA, JwtUtils.getRol(c));
        assertEquals(3, JwtUtils.getBarberiaId(c));
    }

    @Test
    void clienteNoTieneBarberia() {
        Claims c = JwtUtils.validarToken(JwtUtils.generarToken(5, Rol.CLIENTE, null));
        assertNull(JwtUtils.getBarberiaId(c));
        assertEquals(Rol.CLIENTE, JwtUtils.getRol(c));
    }

    @Test
    void rechazaTokenManipulado() {
        String token = JwtUtils.generarToken(5, Rol.CLIENTE, null);
        String[] partes = token.split("\\.");
        // Cambia el payload por uno que dice SUPERADMIN manteniendo la firma original
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"barber-system\",\"sub\":\"5\",\"rol\":\"SUPERADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String manipulado = partes[0] + "." + payloadFalso + "." + partes[2];
        assertThrows(JwtException.class, () -> JwtUtils.validarToken(manipulado));
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        String ajeno = Jwts.builder().issuer("barber-system").subject("1").claim("rol", "SUPERADMIN")
                .signWith(Keys.hmacShaKeyFor("otra_clave_distinta_de_al_menos_32_bytes!!".getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThrows(JwtException.class, () -> JwtUtils.validarToken(ajeno));
    }

    @Test
    void rechazaEmisorDistinto() {
        String token = Jwts.builder().issuer("otro-sistema").subject("1").claim("rol", "CLIENTE")
                .signWith(Keys.hmacShaKeyFor(SECRET_PRUEBAS.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThrows(JwtException.class, () -> JwtUtils.validarToken(token));
    }

    @Test
    void rechazaTokenExpirado() {
        String token = Jwts.builder().issuer("barber-system").subject("1").claim("rol", "CLIENTE")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET_PRUEBAS.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThrows(JwtException.class, () -> JwtUtils.validarToken(token));
    }

    @Test
    void rechazaBasuraYTokenVacio() {
        assertThrows(Exception.class, () -> JwtUtils.validarToken("no.es.jwt"));
        assertThrows(Exception.class, () -> JwtUtils.validarToken(""));
    }
}
