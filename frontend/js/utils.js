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
    }
};
