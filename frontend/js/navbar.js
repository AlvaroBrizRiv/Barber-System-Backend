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
            font-family: 'Montserrat', sans-serif;
        }
        .nav-logo {
            font-family: 'Cinzel', serif; font-size: 1.35rem; color: #D4AF37;
            letter-spacing: 2.5px; font-weight: 700; text-decoration: none;
            display: flex; align-items: center; gap: 0.6rem;
            transition: opacity 0.2s;
        }
        .nav-logo:hover { opacity: 0.9; }
        .nav-logo-icon {
            color: #D4AF37; display: flex; align-items: center;
        }
        .nav-links { display: flex; gap: 1.5rem; align-items: center; }
        .nav-link {
            color: rgba(255,255,255,0.8); text-decoration: none; font-size: 0.85rem;
            font-weight: 600; text-transform: uppercase; letter-spacing: 0.6px;
            transition: color 0.2s, transform 0.2s; display: flex; align-items: center; gap: 0.4rem;
        }
        .nav-link:hover, .nav-link.activo { color: #D4AF37; }
        .nav-link svg { opacity: 0.75; transition: opacity 0.2s; }
        .nav-link:hover svg { opacity: 1; stroke: #D4AF37; }

        /* Botón Iniciar Sesión */
        .btn-nav-login {
            background: linear-gradient(135deg, #D4AF37, #8B6914);
            color: #000; border: none; padding: 0.55rem 1.3rem;
            border-radius: 6px; font-family: 'Montserrat', sans-serif;
            font-size: 0.8rem; font-weight: 700; cursor: pointer;
            text-decoration: none; letter-spacing: 0.5px;
            transition: transform 0.2s, box-shadow 0.2s;
            display: flex; align-items: center; gap: 0.4rem;
        }
        .btn-nav-login:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(212,175,55,0.3); }

        /* Avatar + menú desplegable */
        .nav-perfil { position: relative; }
        .nav-avatar-btn {
            display: flex; align-items: center; gap: 0.6rem; cursor: pointer;
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
        .nav-chevron { color: rgba(255,255,255,0.5); font-size: 0.65rem; transition: transform 0.2s; }

        .nav-dropdown {
            position: absolute; top: calc(100% + 0.6rem); right: 0;
            background: #121212; border: 1px solid rgba(212,175,55,0.25);
            border-radius: 12px; min-width: 220px; overflow: hidden;
            box-shadow: 0 15px 35px rgba(0,0,0,0.7);
            opacity: 0; pointer-events: none;
            transform: translateY(-8px); transition: opacity 0.2s, transform 0.2s;
            z-index: 1001;
        }
        .nav-dropdown.visible { opacity: 1; pointer-events: auto; transform: translateY(0); }
        .nav-dropdown-header {
            padding: 0.9rem 1.2rem;
            border-bottom: 1px solid rgba(255,255,255,0.06);
            background: rgba(255,255,255,0.02);
        }
        .nav-dropdown-user-name { color: #fff; font-size: 0.85rem; font-weight: 600; }
        .nav-dropdown-user-role { color: #D4AF37; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px; margin-top: 2px; }

        .nav-dropdown-item {
            display: flex; align-items: center; gap: 0.8rem;
            padding: 0.75rem 1.2rem; color: rgba(255,255,255,0.8);
            text-decoration: none; font-size: 0.85rem; font-weight: 500;
            transition: background 0.2s, color 0.2s; cursor: pointer; border: none;
            background: transparent; width: 100%; text-align: left;
            font-family: 'Montserrat', sans-serif;
        }
        .nav-dropdown-item:hover { background: rgba(212,175,55,0.1); color: #D4AF37; }
        .nav-dropdown-item.peligro:hover { background: rgba(231,76,60,0.12); color: #E74C3C; }
        .nav-dropdown-divider { border: none; border-top: 1px solid rgba(255,255,255,0.06); margin: 0.2rem 0; }

        /* Selector de idioma */
        .lang-switcher { display: flex; gap: 0.3rem; align-items: center; margin-left: 0.5rem; }
        .btn-lang {
            background: transparent; border: 1px solid rgba(255,255,255,0.12);
            color: rgba(255,255,255,0.5); padding: 0.25rem 0.5rem; border-radius: 4px;
            font-size: 0.7rem; font-weight: 700; cursor: pointer; transition: all 0.2s;
        }
        .btn-lang:hover, .btn-lang.activo { border-color: #D4AF37; color: #D4AF37; background: rgba(212,175,55,0.08); }

        @media (max-width: 768px) {
            .nav-links .nav-link:not(.always-show) { display: none; }
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
                perfil: 'Mi Perfil & Suscripción',
                citas: 'Mis Citas Agendadas',
                agenda: 'Mi Agenda de Trabajo',
                panelAdmin: 'Panel de Administrador',
                salir: 'Cerrar Sesión',
                login: 'Iniciar Sesión',
                rolCliente: 'Cliente',
                rolBarbero: 'Barbero',
                rolAdmin: 'Administrador',
                rolSuper: 'Super Administrador'
            },
            'pt-BR': {
                perfil: 'Meu Perfil & Assinatura',
                citas: 'Meus Agendamentos',
                agenda: 'Minha Agenda de Trabalho',
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

        const inicial = (usuario.nombre || 'U')[0].toUpperCase();
        const avatarHtml = usuario.fotoUrl
            ? `<img src="${usuario.fotoUrl}" alt="perfil" class="nav-avatar" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
            : '';
        const inicialHtml = `<div class="nav-avatar-inicial" style="${usuario.fotoUrl ? 'display:none' : ''}">${inicial}</div>`;

        let rolTexto = L.rolCliente;
        if (rol === 'BARBERO') rolTexto = L.rolBarbero;
        else if (rol === 'ADMIN_BARBERIA') rolTexto = L.rolAdmin;
        else if (rol === 'SUPERADMIN') rolTexto = L.rolSuper;

        let extraLinks = '';
        if (rol === 'CLIENTE') {
            extraLinks = `<a href="${base}/pages/mis-citas.html" class="nav-dropdown-item">${Icons.calendar} ${L.citas}</a>`;
        } else if (rol === 'BARBERO') {
            extraLinks = `<a href="${base}/pages/barbero/mi-agenda.html" class="nav-dropdown-item">${Icons.calendar} ${L.agenda}</a>`;
        } else if (rol === 'ADMIN_BARBERIA' || rol === 'SUPERADMIN') {
            extraLinks = `<a href="${base}/pages/admin/dashboard.html" class="nav-dropdown-item">${Icons.layoutDashboard} ${L.panelAdmin}</a>`;
        }

        return `
            <div class="nav-perfil" id="navPerfilRoot">
                <div class="nav-avatar-btn" onclick="Navbar.toggleDropdown()">
                    ${avatarHtml}
                    ${inicialHtml}
                    <span class="nav-nombre">${usuario.nombre || 'Usuario'}</span>
                    <span class="nav-chevron">▼</span>
                </div>
                <div class="nav-dropdown" id="navDropdown">
                    <div class="nav-dropdown-header">
                        <div class="nav-dropdown-user-name">${usuario.nombre || ''} ${usuario.apellido || ''}</div>
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
                <a href="${base}/index.html" class="nav-logo">
                    <span class="nav-logo-icon">${Icons.scissors}</span>
                    <span>BARBER SYSTEM</span>
                </a>
                <div class="nav-links">
                    <a href="${base}/index.html" class="nav-link always-show ${activoInicio}">
                        ${Icons.scissors} Inicio
                    </a>
                    <a href="${base}/index.html#barberias" class="nav-link">
                        ${Icons.store} Barberías
                    </a>
                    <div class="lang-switcher">
                        ${Icons.globe}
                        <button class="btn-lang ${langEs}"   onclick="if(typeof I18n !== 'undefined') I18n.setLang('es')">ES</button>
                        <button class="btn-lang ${langPtBR}" onclick="if(typeof I18n !== 'undefined') I18n.setLang('pt-BR')">PT</button>
                    </div>
                    ${htmlSeccionUsuario(usuario, rol)}
                </div>
            </nav>
        `;

        const root = document.getElementById('navbar-root');
        if (root) root.innerHTML = html;

        document.addEventListener('click', (e) => {
            if (!e.target.closest('#navPerfilRoot')) {
                document.getElementById('navDropdown')?.classList.remove('visible');
            }
        });
    }

    return {
        init() {
            inyectarCSS();
            render();
        },
        toggleDropdown() {
            document.getElementById('navDropdown')?.classList.toggle('visible');
        },
        Icons
    };

})();

