/**
 * =====================================================================
 * api.js — Cliente HTTP Centralizado del Sistema de Barberías
 * =====================================================================
 *
 * RESPONSABILIDAD:
 *   Este archivo actúa como la capa de comunicación entre el frontend
 *   (HTML/JS) y el backend (API REST en Java/Javalin).
 *
 * CARACTERÍSTICAS:
 *   - Agrega automáticamente el token JWT en el header Authorization.
 *   - Maneja errores 401 (sesión expirada) redirigiendo al login.
 *   - Provee métodos HTTP genéricos: get(), post(), put(), patch(), delete().
 *   - Contiene wrappers semánticos para cada recurso de la API
 *     (barberias, citas, productos, carrito, etc.).
 *   - Sanitiza la ruta para evitar prefijos /api duplicados.
 *
 * USO TÍPICO:
 *   const barberias = await Api.barberias.listar();
 *   const cita = await Api.citas.agendar({ barberiaId: 1, ... });
 *
 * =====================================================================
 */

// URL base de todos los endpoints del backend (configurable por entorno)
const API_BASE = 'http://localhost:7070/api';

/**
 * Objeto principal de la API.
 * Todos los métodos son async y lanzan un Error si la respuesta no es 2xx.
 */
const Api = {

    // ─── SECCIÓN 1: Gestión del Token JWT ────────────────────────────

    /**
     * Obtiene el token JWT guardado en localStorage.
     * @returns {string|null} El token JWT o null si no existe.
     */
    getToken() {
        return localStorage.getItem('token');
    },

    /**
     * Guarda el token JWT en localStorage para persistirlo entre recargas.
     * @param {string} token - El JWT recibido del backend tras autenticar.
     */
    setToken(token) {
        localStorage.setItem('token', token);
    },

    /**
     * Elimina el token JWT y todos los datos de sesión del localStorage.
     * Se llama al cerrar sesión o cuando el token expira (401).
     */
    clearToken() {
        localStorage.removeItem('token');
        localStorage.removeItem('usuario');
        localStorage.removeItem('rol');
    },

    // ─── SECCIÓN 2: Método Base de Request ───────────────────────────

    /**
     * Realiza una petición HTTP autenticada al backend.
     * Este método es la base de todos los demás (get, post, put, etc.).
     *
     * FLUJO:
     *   1. Obtiene el token de localStorage
     *   2. Limpia la ruta (elimina el prefijo /api si ya existe)
     *   3. Agrega el token en el header Authorization: Bearer
     *   4. Hace el fetch al backend
     *   5. Si recibe 401 → elimina la sesión y redirige al login
     *   6. Si recibe otro error → lanza un Error con el mensaje del backend
     *   7. Si es exitoso → retorna los datos JSON
     *
     * @param {string} endpoint - La ruta del endpoint (ej: '/barberias')
     * @param {object} options - Opciones adicionales de fetch (method, body, headers)
     * @returns {Promise<object>} Los datos JSON de la respuesta
     * @throws {Error} Si la conexión falla o el servidor responde con error
     */
    async request(endpoint, options = {}) {
        // Obtener el token JWT almacenado en el navegador
        const token = this.getToken();

        // Sanitizar la ruta: si viene con /api al inicio, quitarlo
        // (evita URLs como http://localhost:7070/api/api/barberias)
        const cleanEndpoint = endpoint.startsWith('/api')
            ? endpoint.substring(4)
            : (endpoint.startsWith('/') ? endpoint : '/' + endpoint);

        // Construir los headers de la petición
        const headers = {
            'Content-Type': 'application/json',   // Enviar y recibir JSON
            // Si hay token, agregar el header de autorización
            ...(token && { 'Authorization': `Bearer ${token}` }),
            ...options.headers  // Permitir headers adicionales por llamada
        };

        try {
            // Realizar la petición HTTP al backend
            const response = await fetch(`${API_BASE}${cleanEndpoint}`, {
                ...options,
                headers
            });

            // Si el servidor responde 401 (No autorizado), la sesión expiró
            if (response.status === 401) {
                this.clearToken();  // Limpiar datos de sesión obsoletos

                // Determinar la ruta base correcta según la ubicación actual
                let base = '';
                if (typeof Auth !== 'undefined' && Auth.getBasePath) {
                    base = Auth.getBasePath();
                } else {
                    const path = window.location.pathname;
                    const index = path.indexOf('/frontend/');
                    if (index !== -1) base = path.substring(0, index + 9);
                }

                // Redirigir al login solo si no estamos ya en el login
                if (!window.location.pathname.includes('login')) {
                    window.location.href = base + '/pages/login.html';
                }
                throw new Error('Sesión expirada. Inicia sesión nuevamente.');
            }

            // Intentar parsear la respuesta como JSON
            // Si no es JSON válido, retorna un objeto vacío
            const data = await response.json().catch(() => ({}));

            // Si la respuesta no fue exitosa (código 4xx o 5xx), lanzar error
            if (!response.ok) {
                throw new Error(data.error || data.mensaje || `Error ${response.status}`);
            }

            // Todo bien: retornar los datos de la respuesta
            return data;

        } catch (err) {
            // Si es un error de red (backend apagado, sin conexión, etc.)
            if (err.name === 'TypeError') {
                throw new Error('No se pudo conectar con el servidor. ¿Está iniciado el backend?');
            }
            // Re-lanzar cualquier otro error (401 manejado arriba, errores del servidor, etc.)
            throw err;
        }
    },

    // ─── SECCIÓN 3: Métodos HTTP Genéricos ───────────────────────────

    /**
     * Realiza una petición GET (lectura de datos).
     * @param {string} endpoint - La ruta del recurso a consultar.
     * @returns {Promise<object>} Los datos retornados por el servidor.
     */
    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    /**
     * Realiza una petición POST (creación de recursos).
     * @param {string} endpoint - La ruta donde crear el recurso.
     * @param {object} body - El objeto a enviar serializado como JSON.
     * @returns {Promise<object>} La respuesta del servidor (ej: ID generado).
     */
    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    /**
     * Realiza una petición PUT (actualización completa de un recurso).
     * @param {string} endpoint - La ruta del recurso a actualizar.
     * @param {object} body - Los datos actualizados del recurso.
     * @returns {Promise<object>} La respuesta del servidor.
     */
    put(endpoint, body) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    /**
     * Realiza una petición PATCH (actualización parcial de un recurso).
     * @param {string} endpoint - La ruta del recurso a modificar parcialmente.
     * @param {object} body - Solo los campos que se van a modificar.
     * @returns {Promise<object>} La respuesta del servidor.
     */
    patch(endpoint, body) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: JSON.stringify(body)
        });
    },

    /**
     * Realiza una petición DELETE (eliminación o desactivación de un recurso).
     * @param {string} endpoint - La ruta del recurso a eliminar.
     * @returns {Promise<object>} La respuesta del servidor.
     */
    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    },

    // ─── SECCIÓN 4: Wrappers Semánticos por Recurso ──────────────────
    // Cada objeto agrupa los endpoints relacionados con un recurso.
    // Esto hace el código del frontend más legible y explícito:
    //   Api.barberias.listar()  en vez de  Api.get('/barberias')

    /**
     * Endpoints de autenticación.
     * No requieren token JWT (son públicos).
     */
    auth: {
        // Inicia sesión con email y contraseña → retorna token + datos del usuario
        login:   (email, password) => Api.post('/auth/login', { email, password }),
        // Registra un nuevo cliente en el sistema
        registro: (datos)          => Api.post('/auth/registro', datos),
        // STUB: SSO Google (retorna 501 — pendiente de implementar)
        ssoGoogle:  () => Api.get('/auth/sso/google'),
        // STUB: Verificación MFA (retorna 501 — pendiente de implementar)
        mfaVerify:  (codigo) => Api.post('/auth/mfa/verify', { codigo }),
    },

    /**
     * Endpoints de barberías.
     * Listar y obtener son públicos. Crear/actualizar requieren rol ADMIN.
     */
    barberias: {
        listar:    ()              => Api.get('/barberias'),              // Lista todas las barberías activas
        obtener:   (id)            => Api.get(`/barberias/${id}`),        // Detalle de una barbería
        crear:     (datos)         => Api.post('/barberias', datos),      // Solo SUPERADMIN
        actualizar: (id, datos)    => Api.put(`/barberias/${id}`, datos), // Solo ADMIN_BARBERIA o SUPERADMIN
    },

    /**
     * Endpoints de empleados (barberos y administradores de barbería).
     * Requieren autenticación. Crear/desactivar requieren rol ADMIN.
     */
    empleados: {
        listar:         (barberiaId) => Api.get(`/empleados/barberia/${barberiaId}`),  // Todos los empleados de una barbería
        listarBarberos: (barberiaId) => Api.get(`/empleados/barberos/${barberiaId}`),  // Solo los barberos activos
        crear:          (datos)      => Api.post('/empleados', datos),                 // Registrar nuevo empleado
        desactivar:     (id)         => Api.delete(`/empleados/${id}`),                // Baja lógica (no elimina de BD)
    },

    /**
     * Endpoints de citas.
     * Los clientes pueden agendar, los barberos pueden completar.
     */
    citas: {
        // Citas de una barbería en un día específico (para el admin)
        agendaBarberia: (barberiaId, fecha) => Api.get(`/citas/barberia/${barberiaId}?fecha=${fecha}`),
        // Citas de un barbero en un día específico (para el barbero)
        agendaBarbero:  (barberoId, fecha)  => Api.get(`/citas/barbero/${barberoId}?fecha=${fecha}`),
        // Citas del cliente autenticado
        misCitas:       ()                  => Api.get('/citas/mis-citas'),
        // Crear una nueva cita (cliente autenticado)
        agendar:        (datos)             => Api.post('/citas', datos),
        // Cambiar el estado de una cita (PENDIENTE → CONFIRMADA → COMPLETADA)
        cambiarEstado:  (citaId, estado)    => Api.put(`/citas/${citaId}/estado`, { estado }),
    },

    /**
     * Endpoints de servicios de barbería.
     * Listar es público (dentro de una barbería). CRUD requiere ADMIN.
     */
    servicios: {
        listar:     (barberiaId)       => Api.get(`/servicios/barberia/${barberiaId}`),
        crear:      (datos)            => Api.post('/servicios', datos),
        actualizar: (id, datos)        => Api.put(`/servicios/${id}`, datos),
        desactivar: (id)               => Api.delete(`/servicios/${id}`),
    },

    /**
     * Endpoints de productos de la tienda.
     * Listar es público por barbería. CRUD requiere ADMIN.
     */
    productos: {
        listar:             (barberiaId)            => Api.get(`/productos/barberia/${barberiaId}`),
        listarPorCategoria: (barberiaId, categoria) => Api.get(`/productos/barberia/${barberiaId}/categoria/${categoria}`),
        crear:              (datos)                 => Api.post('/productos', datos),
        actualizar:         (id, datos)             => Api.put(`/productos/${id}`, datos),
        desactivar:         (id)                    => Api.delete(`/productos/${id}`),
    },

    /**
     * Endpoints de calificaciones.
     * Los clientes pueden calificar una cita completada.
     */
    calificaciones: {
        listar:          (barberiaId)               => Api.get(`/calificaciones/barberia/${barberiaId}`),
        promedioBarberia: (barberiaId)              => Api.get(`/calificaciones/promedio/${barberiaId}`),
        promedioBarbero:  (barberoId, barberiaId)   => Api.get(`/calificaciones/barbero/${barberoId}?barberiaId=${barberiaId}`),
        calificar:        (datos)                   => Api.post('/calificaciones', datos),
    },

    /**
     * Endpoints de planes de suscripción.
     * Listar es público por barbería. CRUD requiere ADMIN.
     */
    suscripciones: {
        listar:     (barberiaId) => Api.get(`/suscripciones/barberia/${barberiaId}`),
        crear:      (datos)      => Api.post('/suscripciones', datos),
        actualizar: (id, datos)  => Api.put(`/suscripciones/${id}`, datos),
        toggle:     (id)         => Api.patch(`/suscripciones/${id}/toggle`, {}), // Activar/desactivar plan
    },

    /**
     * Endpoints de perfil del usuario autenticado.
     * El backend detecta el rol desde el token JWT y retorna el perfil correcto.
     */
    perfil: {
        obtener:    ()      => Api.get('/perfil'),       // Obtener datos del perfil propio
        actualizar: (datos) => Api.put('/perfil', datos), // Actualizar nombre, foto, teléfono, etc.
    },

    /**
     * Endpoints del carrito de compras.
     * El carrito es persistente en la BD y se aísla por barbería y cliente.
     */
    carrito: {
        // Obtener el carrito actual del cliente en una barbería específica
        obtener:            (barberiaId)                      => Api.get(`/carrito/${barberiaId}`),
        // Agregar un producto al carrito
        agregar:            (barberiaId, productoId, cantidad) =>
            Api.post(`/carrito/${barberiaId}`, { productoId, cantidad }),
        // Actualizar la cantidad de un producto ya en el carrito
        actualizarCantidad: (barberiaId, productoId, cantidad) =>
            Api.put(`/carrito/${barberiaId}/${productoId}`, { cantidad }),
        // Eliminar un producto del carrito
        eliminar:           (barberiaId, productoId)           =>
            Api.delete(`/carrito/${barberiaId}/${productoId}`),
        // Finalizar la compra: vacía el carrito en la BD (simulado sin pasarela de pago)
        checkout:           (barberiaId)                       =>
            Api.post(`/carrito/${barberiaId}/checkout`, {}),
    },
};
