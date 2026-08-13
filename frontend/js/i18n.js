/**
 * i18n.js — Módulo de internacionalización ligero
 *
 * Soporta: es (Español) | pt-BR (Português Brasil)
 * Escalable: añadir nuevos idiomas creando frontend/locales/{lang}.json
 *
 * Uso:
 *   await I18n.init();           // Cargar idioma al iniciar la página
 *   I18n.t('nav.inicio')         // Obtener texto traducido
 *   I18n.setLang('pt-BR')        // Cambiar idioma y recargar
 *   I18n.getLang()               // Obtener idioma actual
 */
const I18n = (() => {
    let _traducciones = {};
    let _lang = 'es';

    // ─── Detectar idioma ──────────────────────────────────────────────

    function detectarLang() {
        // 1. localStorage (preferencia explícita del usuario)
        const guardado = localStorage.getItem('idioma');
        if (guardado) return guardado;

        // 2. Idioma del usuario autenticado (si existe en session)
        const usuario = JSON.parse(localStorage.getItem('usuario') || 'null');
        if (usuario?.idiomaPreferido) return usuario.idiomaPreferido;

        // 3. Idioma del navegador
        const nav = navigator.language || navigator.userLanguage;
        if (nav && nav.startsWith('pt')) return 'pt-BR';

        return 'es';
    }

    // ─── Cargar archivo de traducciones ───────────────────────────────

    async function cargar(lang) {
        // Calcular la ruta relativa al archivo de locales
        const profundidad = window.location.pathname.split('/').filter(Boolean).length;
        const base = profundidad <= 2 ? '..' : '../..';
        
        try {
            const resp = await fetch(`${base}/locales/${lang}.json`);
            if (!resp.ok) throw new Error(`No se pudo cargar ${lang}.json`);
            return await resp.json();
        } catch {
            // Fallback a español si el archivo no existe
            if (lang !== 'es') {
                const resp = await fetch(`${base}/locales/es.json`);
                return resp.json();
            }
            return {};
        }
    }

    // ─── API pública ──────────────────────────────────────────────────

    return {
        /**
         * Inicializa el módulo: detecta el idioma, carga las traducciones
         * y aplica los textos al DOM.
         */
        async init() {
            _lang = detectarLang();
            _traducciones = await cargar(_lang);
            this.aplicar();
            return _lang;
        },

        /**
         * Obtiene una traducción por clave con notación de punto.
         * @param {string} clave  Ej: 'nav.inicio', 'auth.login'
         * @param {Object} vars   Variables a interpolar (ej: { nombre: 'Juan' })
         * @returns {string}
         */
        t(clave, vars = {}) {
            const partes = clave.split('.');
            let valor = _traducciones;
            for (const parte of partes) {
                valor = valor?.[parte];
                if (valor === undefined) return clave; // Devolver la clave si no se encontró
            }
            // Interpolación de variables: {{ nombre }} → 'Juan'
            return String(valor).replace(/\{\{(\w+)\}\}/g, (_, k) => vars[k] ?? `{{${k}}}`);
        },

        /** Cambia el idioma activo, lo guarda y recarga la página. */
        setLang(lang) {
            localStorage.setItem('idioma', lang);
            window.location.reload();
        },

        getLang() { return _lang; },

        /**
         * Aplica las traducciones al DOM buscando elementos con [data-i18n].
         * Ej: <span data-i18n="nav.inicio"></span>
         */
        aplicar() {
            document.querySelectorAll('[data-i18n]').forEach(el => {
                const clave = el.getAttribute('data-i18n');
                const texto = this.t(clave);
                if (texto !== clave) el.textContent = texto;
            });
            document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
                const clave = el.getAttribute('data-i18n-placeholder');
                const texto = this.t(clave);
                if (texto !== clave) el.placeholder = texto;
            });
        }
    };
})();
