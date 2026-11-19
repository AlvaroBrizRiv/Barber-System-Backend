/**
 * navbar.js — Barra de navegación dinámica con perfil de usuario
 *
 * Incluye en tu HTML:
 *   <div id="navbar-root"></div>
 *   <script src="../js/api.js"></script>
 *   <script src="../js/auth.js"></script>
 *   <script src="../js/i18n.js"></script>
 *   <script src="../js/navbar.js"></script>
 *   <script>Navbar.init();</script>
 */
const Navbar = (() => {

    // ─── Iconos Vectoriales SVG Profesionales (Estilo Lucide / Minimalista) ───
    const Icons = {
        scissors: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><line x1="20" y1="4" x2="8.12" y2="15.88"/><line x1="14.47" y1="14.48" x2="20" y2="20"/><line x1="8.12" y1="8.12" x2="12" y2="12"/></svg>`,
        store: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7"/><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><path d="M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4"/><path d="M2 7h20"/><path d="M22 7v3a2 2 0 0 1-2 2v0a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7"/></svg>`,
        calendar: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`,
        user: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
        logOut: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>`,
        layoutDashboard: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
        shoppingBag: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>`,
        creditCard: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/></svg>`,
        globe: `<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`,
    };

    /** Escape local (navbar.js puede cargarse sin utils.js). */
    function esc(v) {
        return String(v ?? '').replace(/[&<>"']/g, c =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }
    function urlSegura(url) {
        if (!url) return '';
        try {
            const u = new URL(url, window.location.origin);
            return ['http:', 'https:'].includes(u.protocol) ? u.href : '';
        } catch { return ''; }
    }

    function getBase() {
        return (typeof Auth !== 'undefined' && Auth.getBasePath)
            ? Auth.getBasePath()
            : (window.location.pathname.includes('/frontend/') ? '/frontend' : '');
    }

    const CSS = `
        #nav-bs {
            display: flex; align-items: center; justify-content: space-between;
            padding: 1rem 4%; background: rgba(10,10,10,0.94);
            backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
            border-bottom: 1px solid rgba(212,175,55,0.2);
            position: sticky; top: 0; z-index: 1000;
            font-family: var(--font-body);
        }
        .nav-logo {
            font-family: var(--font-display); font-size: 1.3rem; color: #D4AF37;
            font-weight: 600; text-decoration: none; min-height: 44px;
            display: flex; align-items: center; gap: 0.6rem;
            transition: opacity 0.2s;
        }
        .nav-logo:hover { opacity: 0.9; }
        .nav-logo-icon {
            color: #D4AF37; display: flex; align-items: center;
        }
        .nav-links { display: flex; gap: 1.5rem; align-items: center; }
        .nav-link {
            color: rgba(255,255,255,0.82); text-decoration: none; font-size: 0.92rem;
            font-weight: 600;
            transition: color 0.2s, transform 0.2s; display: flex; align-items: center; gap: 0.4rem;
        }
        .nav-link:hover, .nav-link.activo { color: #D4AF37; }
        .nav-link svg { opacity: 0.75; transition: opacity 0.2s; }
        .nav-link:hover svg { opacity: 1; stroke: #D4AF37; }

        /* Botón Iniciar Sesión */
        .btn-nav-login {
            background: #D4AF37;
            color: #000; border: none; padding: 0.55rem 1.1rem; min-height: 40px;
            border-radius: 8px; font-family: var(--font-body);
            font-size: 0.9rem; font-weight: 700; cursor: pointer;
            text-decoration: none;
            transition: background-color 0.2s;
            display: flex; align-items: center; gap: 0.4rem;
        }
        .btn-nav-login:hover { background: #F5D57A; }

        /* Avatar + menú desplegable */
        .nav-perfil { position: relative; }
        .nav-avatar-btn {
            display: flex; align-items: center; gap: 0.6rem; cursor: pointer;
            font: inherit; color: inherit; min-height: 44px;
            background: rgba(255,255,255,0.05); border: 1px solid rgba(212,175,55,0.35);
            border-radius: 30px; padding: 0.35rem 0.9rem 0.35rem 0.35rem;
            transition: background 0.2s, border-color 0.2s;
        }
        .nav-avatar-btn:hover { background: rgba(255,255,255,0.1); border-color: #D4AF37; }
        .nav-avatar {
            width: 32px; height: 32px; border-radius: 50%; object-fit: cover;
            border: 2px solid #D4AF37; display: block;
        }
        .nav-avatar-inicial {
            width: 32px; height: 32px; border-radius: 50%;
            background: linear-gradient(135deg, #D4AF37, #8B6914);
            display: flex; align-items: center; justify-content: center;
            font-size: 0.85rem; font-weight: 700; color: #000;
        }
        .nav-nombre { color: #fff; font-size: 0.85rem; font-weight: 600; max-width: 140px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .nav-chevron { color: rgba(255,255,255,0.6); display: flex; transition: transform 0.2s; }
        .nav-avatar-btn[aria-expanded="true"] .nav-chevron { transform: rotate(180deg); }

        .nav-dropdown {
            position: absolute; top: calc(100% + 0.6rem); right: 0;
            background: #121212; border: 1px solid rgba(212,175,55,0.25);
            border-radius: 12px; min-width: 220px; overflow: hidden;
            box-shadow: 0 15px 35px rgba(0,0,0,0.7);
            opacity: 0; pointer-events: none; visibility: hidden;
            transform: translateY(-8px); transition: opacity 0.2s, transform 0.2s, visibility 0s linear 0.2s;
            z-index: 1001;
        }
        .nav-dropdown.visible { opacity: 1; pointer-events: auto; visibility: visible; transform: translateY(0); transition: opacity 0.2s, transform 0.2s; }
        .nav-dropdown-header {
            padding: 0.9rem 1.2rem;
            border-bottom: 1px solid rgba(255,255,255,0.06);
            background: rgba(255,255,255,0.02);
        }
        .nav-dropdown-user-name { color: #fff; font-size: 0.85rem; font-weight: 600; }
        .nav-dropdown-user-role { color: #D4AF37; font-size: 0.8rem; margin-top: 2px; }

        .nav-dropdown-item {
            display: flex; align-items: center; gap: 0.8rem;
            padding: 0.8rem 1.2rem; color: rgba(255,255,255,0.85);
            text-decoration: none; font-size: 0.92rem; font-weight: 500;
            transition: background 0.2s, color 0.2s; cursor: pointer; border: none;
            background: transparent; width: 100%; text-align: left;
            font-family: var(--font-body);
        }
        .nav-dropdown-item:hover, .nav-dropdown-item:focus-visible { background: rgba(212,175,55,0.1); color: #D4AF37; outline-offset: -2px; }
        .nav-dropdown-item.peligro:hover { background: rgba(231,76,60,0.12); color: #E74C3C; }
        .nav-dropdown-divider { border: none; border-top: 1px solid rgba(255,255,255,0.06); margin: 0.2rem 0; }

        /* Selector de idioma */
        .lang-switcher { display: flex; gap: 0.3rem; align-items: center; margin-left: 0.5rem; }
        .btn-lang {
            background: transparent; border: 1px solid rgba(255,255,255,0.12);
            color: rgba(255,255,255,0.72); padding: 0.25rem 0.55rem; border-radius: 6px;
            min-width: 40px; min-height: 36px;
            font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: border-color 0.2s, color 0.2s, background-color 0.2s;
        }
        .btn-lang:hover, .btn-lang.activo { border-color: #D4AF37; color: #D4AF37; background: rgba(212,175,55,0.08); }

        @media (max-width: 768px) {
            .nav-links .nav-link:not(.always-show) { display: none; }
        }
        @media (max-width: 480px) {
            #nav-bs { padding: 0.75rem 1rem; }
            .nav-logo span:last-child, .nav-nombre, .nav-chevron, .nav-links .nav-link { display: none; }
            .nav-logo-icon svg { width: 24px; height: 24px; }
            .nav-logo { min-width: 44px; justify-content: center; }
            .nav-links { gap: 0.6rem; }
            .lang-switcher { margin-left: 0; }
            .lang-switcher > svg { display: none; }
            .nav-avatar-btn { padding: 0.25rem; }
            .btn-nav-login { padding: 0.5rem 0.9rem; }
            .nav-dropdown { right: -0.5rem; min-width: min(240px, calc(100vw - 2rem)); }
        }
    `;

    function inyectarCSS() {
        if (document.getElementById('nav-bs-css')) return;
        const style = document.createElement('style');
        style.id = 'nav-bs-css';
        style.textContent = CSS;
        document.head.appendChild(style);
    }

    function htmlSeccionUsuario(usuario, rol) {
        const base = getBase();
        const lang = localStorage.getItem('idioma') || 'es';

        const labels = {
            es: {
                perfil: 'Mi perfil',
                citas: 'Mis citas',
                agenda: 'Mi Agenda de Trabajo',
                ganancias: 'Mis Ganancias',
                stock: 'Stock de Productos',
                panelAdmin: 'Panel de Administrador',
                salir: 'Cerrar sesión',
                login: 'Entrar',
                rolCliente: 'Cliente',
                rolBarbero: 'Barbero',
                rolAdmin: 'Administrador',
                rolSuper: 'Super Administrador'
            },
            'pt-BR': {
                perfil: 'Meu Perfil',
                citas: 'Meus Agendamentos',
                agenda: 'Minha Agenda de Trabalho',
                ganancias: 'Meus Ganhos',
                stock: 'Estoque de Produtos',
                panelAdmin: 'Painel do Administrador',
                salir: 'Sair',
                login: 'Entrar',
                rolCliente: 'Cliente',
                rolBarbero: 'Barbeiro',
                rolAdmin: 'Administrador',
                rolSuper: 'Super Administrador'
            }
        };
        const L = labels[lang] || labels.es;

        if (!usuario) {
            return `
                <a href="${base}/pages/login.html" class="btn-nav-login">
                    ${Icons.user} ${L.login}
                </a>
            `;
        }

        const inicial = esc((usuario.nombre || 'U')[0].toUpperCase());
        const foto = urlSegura(usuario.fotoUrl);
        const avatarHtml = foto
            ? `<img src="${esc(foto)}" alt="" class="nav-avatar" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
            : '';
        const inicialHtml = `<span class="nav-avatar-inicial" aria-hidden="true" style="${foto ? 'display:none' : ''}">${inicial}</span>`;

        let rolTexto = L.rolCliente;
        if (rol === 'BARBERO') rolTexto = L.rolBarbero;
        else if (rol === 'ADMIN_BARBERIA') rolTexto = L.rolAdmin;
        else if (rol === 'SUPERADMIN') rolTexto = L.rolSuper;

        let extraLinks = '';
        if (rol === 'CLIENTE') {
            extraLinks = `<a href="${base}/pages/mis-citas.html" class="nav-dropdown-item">${Icons.calendar} ${L.citas}</a>
                <a href="${base}/reserva.html" class="nav-dropdown-item">${Icons.scissors} Reservar hora</a>`;
        } else if (rol === 'BARBERO') {
            extraLinks = `
                <a href="${base}/pages/barbero/mi-agenda.html" class="nav-dropdown-item">${Icons.calendar} ${L.agenda}</a>
                <a href="${base}/pages/barbero/mis-ganancias.html" class="nav-dropdown-item">${Icons.creditCard} ${L.ganancias}</a>
                <a href="${base}/pages/barbero/stock.html" class="nav-dropdown-item">${Icons.shoppingBag} ${L.stock}</a>`;
        } else if (rol === 'ADMIN_BARBERIA' || rol === 'SUPERADMIN') {
            extraLinks = `<a href="${base}/pages/admin/dashboard.html" class="nav-dropdown-item">${Icons.layoutDashboard} ${L.panelAdmin}</a>`;
        }

        return `
            <div class="nav-perfil" id="navPerfilRoot">
                <button type="button" class="nav-avatar-btn" id="navAvatarBtn"
                        aria-haspopup="true" aria-expanded="false" aria-controls="navDropdown"
                        aria-label="Menú de ${esc(usuario.nombre || 'tu cuenta')}">
                    ${avatarHtml}
                    ${inicialHtml}
                    <span class="nav-nombre">${esc(usuario.nombre || 'Usuario')}</span>
                    <span class="nav-chevron" aria-hidden="true"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg></span>
                </button>
                <div class="nav-dropdown" id="navDropdown">
                    <div class="nav-dropdown-header">
                        <div class="nav-dropdown-user-name">${esc(usuario.nombre)} ${esc(usuario.apellido)}</div>
                        <div class="nav-dropdown-user-role">${rolTexto}</div>
                    </div>
                    <a href="${base}/pages/perfil.html" class="nav-dropdown-item">
                        ${Icons.user} ${L.perfil}
                    </a>
                    ${extraLinks}
                    <hr class="nav-dropdown-divider">
                    <button class="nav-dropdown-item peligro" onclick="Auth.logout()">
                        ${Icons.logOut} ${L.salir}
                    </button>
                </div>
            </div>
        `;
    }

    function render() {
        const base = getBase();
        const lang = localStorage.getItem('idioma') || 'es';
        const usuario = JSON.parse(localStorage.getItem('usuario') || 'null');
        const rol = localStorage.getItem('rol') || '';

        const path = window.location.pathname;
        const activoInicio = !path.includes('/pages/') && (path.endsWith('index.html') || path.endsWith('/')) ? 'activo' : '';

        const langEs   = lang === 'es' ? 'activo' : '';
        const langPtBR = lang === 'pt-BR' ? 'activo' : '';

        const html = `
            <nav id="nav-bs">
                <a href="${base}/index.html" class="nav-logo" aria-label="Barber System, ir al inicio">
                    <span class="nav-logo-icon" aria-hidden="true">${Icons.scissors}</span>
                    <span>Barber System</span>
                </a>
                <div class="nav-links">
                    <a href="${base}/index.html" class="nav-link always-show ${activoInicio}">
                        ${Icons.scissors} Inicio
                    </a>
                    <a href="${base}/index.html#barberias" class="nav-link">
                        ${Icons.store} Barberías
                    </a>
                    <div class="lang-switcher" role="group" aria-label="Idioma">
                        <span aria-hidden="true" style="display:flex">${Icons.globe}</span>
                        <button type="button" class="btn-lang ${langEs}" aria-pressed="${!!langEs}" aria-label="Español" onclick="if(typeof I18n !== 'undefined') I18n.setLang('es')">ES</button>
                        <button type="button" class="btn-lang ${langPtBR}" aria-pressed="${!!langPtBR}" aria-label="Português" onclick="if(typeof I18n !== 'undefined') I18n.setLang('pt-BR')">PT</button>
                    </div>
                    ${htmlSeccionUsuario(usuario, rol)}
                </div>
            </nav>
        `;

        const root = document.getElementById('navbar-root');
        if (root) root.innerHTML = html;

        document.getElementById('navAvatarBtn')?.addEventListener('click', () => toggleDropdown());
        document.addEventListener('click', (e) => {
            if (!e.target.closest('#navPerfilRoot')) cerrarDropdown(false);
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') cerrarDropdown(true);
        });
    }

    function toggleDropdown() {
        const menu = document.getElementById('navDropdown');
        const btn = document.getElementById('navAvatarBtn');
        if (!menu || !btn) return;
        const abrir = !menu.classList.contains('visible');
        menu.classList.toggle('visible', abrir);
        btn.setAttribute('aria-expanded', String(abrir));
        if (abrir) menu.querySelector('.nav-dropdown-item')?.focus();
    }

    function cerrarDropdown(devolverFoco) {
        const menu = document.getElementById('navDropdown');
        const btn = document.getElementById('navAvatarBtn');
        if (!menu?.classList.contains('visible')) return;
        menu.classList.remove('visible');
        btn?.setAttribute('aria-expanded', 'false');
        if (devolverFoco) btn?.focus();
    }

    return {
        init() {
            inyectarCSS();
            render();
        },
        toggleDropdown,
        Icons
    };

})();

