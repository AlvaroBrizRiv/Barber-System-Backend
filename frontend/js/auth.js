/**
 * =====================================================================
 * auth.js — Gestión de Sesión de Usuario en el Frontend
 * =====================================================================
 *
 * RESPONSABILIDAD:
 *   Centraliza toda la lógica de autenticación del lado del cliente:
 *   inicio de sesión, registro, cierre de sesión, lectura de datos
 *   del usuario autenticado y guardias de ruta por rol.
 *
 * ALMACENAMIENTO (localStorage):
 *   - 'token'   → El JWT firmado por el servidor
 *   - 'rol'     → El rol del usuario ('CLIENTE', 'BARBERO', 'ADMIN_BARBERIA', 'SUPERADMIN')
 *   - 'usuario' → Objeto JSON con los datos básicos del usuario (nombre, email, foto, etc.)
 *   - 'idioma'  → Idioma preferido del usuario para i18n
 *
 * SEGURIDAD:
 *   - Nunca se guarda la contraseña en el cliente.
 *   - El token JWT se envía en cada request vía Api.js (header Authorization).
 *   - Las rutas protegidas llaman a requerirAuth() o requerirRol() al cargar la página.
 *
 * =====================================================================
 */

/**
 * Objeto principal de autenticación.
 * Se usa directamente desde cualquier página: Auth.login(), Auth.logout(), etc.
 */
const Auth = {

    // ─── SECCIÓN 1: Guardar y Recuperar la Sesión ────────────────────

    /**
     * Guarda los datos de sesión recibidos del backend en localStorage.
     * Se llama justo después de un login exitoso.
     *
     * @param {object} respuesta - El JSON del backend con { token, rol, usuario }
     */
    guardarSesion(respuesta) {
        // Guardar el token JWT para autenticar futuras peticiones
        localStorage.setItem('token',   respuesta.token);
        // Guardar el rol para controlar acceso a secciones
        localStorage.setItem('rol',     respuesta.rol);
        // Guardar el objeto usuario completo (nombre, foto, barberiaId, etc.)
        localStorage.setItem('usuario', JSON.stringify(respuesta.usuario));

        // Guardar el idioma preferido para que i18n.js lo use sin un fetch adicional
        if (respuesta.usuario?.idiomaPreferido) {
            localStorage.setItem('idioma', respuesta.usuario.idiomaPreferido);
        }
    },

    // ─── SECCIÓN 2: Utilidad de Rutas Relativas ──────────────────────

    /**
     * Detecta el prefijo base de las rutas según dónde está alojado el frontend.
     * Esto evita errores de redirección cuando el servidor sirve desde /frontend/
     * o cuando se abre el archivo directamente desde el sistema de archivos.
     *
     * @returns {string} '' o '/frontend' según la configuración del servidor
     */
    getBasePath() {
        const path = window.location.pathname;
        // Encontrar la posición de '/frontend' para soportar rutas absolutas de archivos locales (file:///)
        const index = path.indexOf('/frontend/');
        if (index !== -1) {
            return path.substring(0, index + 9); // Retorna la ruta exacta hasta '/frontend'
        }
        if (path.endsWith('/frontend') || path.endsWith('/frontend/')) {
            return path;
        }
        return '';
    },

    // ─── SECCIÓN 3: Lectura de Datos de Sesión ───────────────────────

    /**
     * Obtiene el objeto del usuario autenticado desde localStorage.
     * @returns {object|null} El usuario (con nombre, email, foto, etc.) o null.
     */
    getUsuario() {
        const u = localStorage.getItem('usuario');
        // Parsear el JSON guardado, o retornar null si no hay sesión
        return u ? JSON.parse(u) : null;
    },

    /**
     * Obtiene el rol del usuario autenticado.
     * @returns {string|null} El rol ('CLIENTE', 'BARBERO', 'ADMIN_BARBERIA') o null.
     */
    getRol() {
        return localStorage.getItem('rol');
    },

    /**
     * Verifica si el usuario tiene una sesión activa (hay token en localStorage).
     * NO valida si el token es válido o expirado — eso lo hace el backend.
     * @returns {boolean} true si hay sesión, false si no.
     */
    estaAutenticado() {
        return !!localStorage.getItem('token');
    },

    /**
     * Verifica si el usuario autenticado es administrador de barbería.
     * @returns {boolean}
     */
    esAdmin() {
        return this.getRol() === 'ADMIN_BARBERIA';
    },

    /**
     * Verifica si el usuario autenticado es barbero.
     * @returns {boolean}
     */
    esBarbero() {
        return this.getRol() === 'BARBERO';
    },

    /**
     * Verifica si el usuario autenticado es un cliente.
     * @returns {boolean}
     */
    esCliente() {
        return this.getRol() === 'CLIENTE';
    },

    /**
     * Obtiene el ID de la barbería asociada al usuario (solo para empleados).
     * Los clientes no tienen barberiaId (pueden ir a cualquier barbería).
     * @returns {number|null} El ID de la barbería o null.
     */
    getBarberiaId() {
        const u = this.getUsuario();
        return u ? u.barberiaId : null;
    },

    // ─── SECCIÓN 4: Login (Inicio de Sesión) ─────────────────────────

    /**
     * Inicia sesión del usuario enviando email y contraseña al backend.
     * Si es exitoso, guarda la sesión en localStorage.
     *
     * FLUJO:
     *   1. Llama a Api.auth.login() → POST /api/auth/login
     *   2. Si el servidor retorna 200: guarda token + usuario + rol
     *   3. Si el servidor retorna error: retorna el mensaje de error
     *
     * @param {string} email - El correo electrónico del usuario.
     * @param {string} password - La contraseña en texto plano (HTTPS la protege en tránsito).
     * @returns {Promise<{exito: boolean, respuesta?: object, error?: string}>}
     */
    async login(email, password) {
        try {
            // Llamar al endpoint de login del backend
            const respuesta = await Api.auth.login(email, password);
            // Guardar los datos de sesión en el navegador
            this.guardarSesion(respuesta);
            return { exito: true, respuesta };
        } catch (err) {
            // Retornar el error (ej: "Credenciales incorrectas") para mostrarlo en la UI
            return { exito: false, error: err.message };
        }
    },

    // ─── SECCIÓN 5: Registro de Cliente ──────────────────────────────

    /**
     * Registra un nuevo cliente en el sistema.
     * Solo los clientes se auto-registran; los empleados son creados por el admin.
     *
     * @param {object} datos - Los datos del nuevo cliente:
     *   { documentoIdentidad, nombre, apellido, email, telefono, password }
     * @returns {Promise<{exito: boolean, respuesta?: object, error?: string}>}
     */
    async registro(datos) {
        try {
            // Llamar al endpoint de registro → POST /api/auth/registro
            const respuesta = await Api.auth.registro(datos);
            return { exito: true, respuesta };
        } catch (err) {
            return { exito: false, error: err.message };
        }
    },

    // ─── SECCIÓN 6: Logout (Cerrar Sesión) ───────────────────────────

    /**
     * Cierra la sesión del usuario:
     *   1. Elimina token, rol y datos del usuario de localStorage.
     *   2. Redirige a la página de login.
     *
     * NOTA: El JWT no se invalida en el servidor (stateless). Para invalidación
     * real se necesitaría una lista de revocación (pendiente para producción).
     */
    logout() {
        // Limpiar todos los datos de sesión del localStorage
        localStorage.removeItem('token');
        localStorage.removeItem('rol');
        localStorage.removeItem('usuario');
        // Redirigir al login con la ruta correcta
        window.location.href = this.getBasePath() + '/pages/login.html';
    },

    // ─── SECCIÓN 7: Redirección por Rol ──────────────────────────────

    /**
     * Redirige al usuario a la página correcta según su rol.
     * Se usa después del login exitoso para llevar a cada usuario a su panel.
     *
     * Rutas de destino:
     *   - ADMIN_BARBERIA → /pages/admin/dashboard.html
     *   - BARBERO        → /pages/barbero/mi-agenda.html
     *   - CLIENTE        → /index.html (landing con sus barberías)
     */
    redirigirSegunRol() {
        const rol  = this.getRol();
        const base = this.getBasePath();

        switch (rol) {
            case 'ADMIN_BARBERIA':
                window.location.href = base + '/pages/admin/dashboard.html';
                break;
            case 'BARBERO':
                window.location.href = base + '/pages/barbero/mi-agenda.html';
                break;
            case 'CLIENTE':
                // El cliente vuelve al inicio donde puede ver las barberías
                window.location.href = base + '/index.html';
                break;
            default:
                // Si el rol no se reconoce, enviar al login
                window.location.href = base + '/pages/login.html';
        }
    },

    // ─── SECCIÓN 8: Guardias de Ruta ─────────────────────────────────

    /**
     * Guardia de ruta: requiere que el usuario esté autenticado.
     * Si no lo está, redirige al login inmediatamente.
     *
     * USO: Llamar al inicio del <script> de cualquier página protegida.
     *   if (!Auth.requerirAuth()) return; // Detiene el resto del script
     *
     * @returns {boolean} true si el usuario está autenticado, false si no.
     */
    requerirAuth() {
        if (!this.estaAutenticado()) {
            // No hay sesión → redirigir al login
            window.location.href = this.getBasePath() + '/pages/login.html';
            return false;
        }
        return true;
    },

    /**
     * Guardia de ruta: requiere que el usuario tenga un rol específico.
     * Primero verifica autenticación, luego verifica el rol.
     *
     * USO:
     *   if (!Auth.requerirRol('ADMIN_BARBERIA')) return;
     *
     * @param {string} rolRequerido - El rol que debe tener el usuario (ej: 'ADMIN_BARBERIA').
     * @returns {boolean} true si el usuario tiene el rol correcto, false si no.
     */
    requerirRol(rolRequerido) {
        // Primero verificar que esté autenticado
        if (!this.requerirAuth()) return false;

        // Luego verificar que tenga el rol correcto
        if (this.getRol() !== rolRequerido) {
            alert('No tienes permisos para acceder a esta página.');
            // Redirigir a la página correcta para su rol actual
            this.redirigirSegunRol();
            return false;
        }
        return true;
    }
};
