/**
 * Utilidades globales para sanitización y seguridad en el Frontend (SEC-05).
 * Previene vulnerabilidades Cross-Site Scripting (XSS) en renderizado dinámico.
 */
const Utils = {
    /**
     * Escapa caracteres especiales de HTML para evitar inyección XSS.
     * @param {*} valor Cadena o dato a escapar
     * @returns {string} Cadena sanitizada
     */
    esc(valor) {
        return String(valor ?? '').replace(/[&<>"']/g, c =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    },

    /**
     * Valida que una URL externa use exclusivamente protocolos seguros (http/https).
     * Evita ataques basados en javascript: o data: maliciosos.
     * @param {string} url Cadena URL
     * @returns {string} URL validada o cadena vacía si es insegura
     */
    urlSegura(url) {
        if (!url) return '';
        try {
            const parsed = new URL(url, window.location.origin);
            return ['http:', 'https:'].includes(parsed.protocol) ? parsed.href : '';
        } catch {
            return '';
        }
    },

    /**
     * Formatea un monto en pesos chilenos sin decimales.
     * @param {number|string} monto
     * @returns {string} Ej: "$15.000"
     */
    dinero(monto) {
        const n = Number(monto);
        return '$' + (Number.isFinite(n) ? n : 0).toLocaleString('es-CL', { maximumFractionDigits: 0 });
    },

    /**
     * Fecha local en formato YYYY-MM-DD (evita el desfase de toISOString con UTC).
     * @param {Date} [d=new Date()]
     * @returns {string}
     */
    fechaISO(d = new Date()) {
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        return `${d.getFullYear()}-${mm}-${dd}`;
    },

    /**
     * Retrasa la ejecución de una función hasta que el usuario deja de escribir.
     * @param {Function} fn
     * @param {number} ms
     * @returns {Function}
     */
    debounce(fn, ms = 300) {
        let t;
        return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
    }
};
