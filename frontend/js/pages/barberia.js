/**
 * barberia.js — Página de inicio de una barbería.
 *
 * Secciones: hero, barberos (elegir → reserva), servicios (→ reserva),
 * productos (vista previa de la tienda) y suscripciones, esta última solo si
 * la barbería la habilitó (suscripcionesHabilitadas) y tiene planes activos.
 *
 * Seguridad: todo dato proveniente de la API se escapa con Utils.esc y las
 * URLs de imágenes pasan por Utils.urlSegura (evita XSS e inyección de javascript:).
 */
const BARBERIA_ID = Number.parseInt(new URLSearchParams(location.search).get('id'), 10);
const MAX_PRODUCTOS_PREVIA = 4;

const ICONOS = {
    ubicacion: '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>',
    telefono: '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>',
    calendario: '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
    bolsa: '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>',
    reloj: '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
};

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();

    if (!Number.isInteger(BARBERIA_ID) || BARBERIA_ID <= 0) {
        window.location.href = Auth.getBasePath() + '/index.html';
        return;
    }
    await cargarBarberia();
});

/** Resuelve una petición devolviendo un valor por defecto si falla (sección opcional). */
const opcional = (promesa, porDefecto) => promesa.catch(() => porDefecto);

async function cargarBarberia() {
    try {
        const [barberia, servicios, barberos, productos, planes, calificaciones, promedioData] = await Promise.all([
            Api.barberias.obtener(BARBERIA_ID),
            opcional(Api.servicios.listar(BARBERIA_ID), []),
            opcional(Api.empleados.listarBarberos(BARBERIA_ID), []),
            opcional(Api.productos.listar(BARBERIA_ID), []),
            opcional(Api.suscripciones.listar(BARBERIA_ID), []),
            opcional(Api.calificaciones.listar(BARBERIA_ID), []),
            opcional(Api.calificaciones.promedioBarberia(BARBERIA_ID), { promedio: 0 }),
        ]);

        if (!barberia || barberia.activa === false) throw new Error('Barbería no disponible');

        document.title = `${barberia.nombre} — Barber System`;
        renderizarHero(barberia, Number(promedioData?.promedio) || 0);
        renderizarBarberos(barberos);
        renderizarServicios(servicios.filter(s => s.activo !== false));
        renderizarProductos(productos);
        renderizarSuscripciones(barberia, planes);
        renderizarCalificaciones(calificaciones);
        document.getElementById('contenidoMain').hidden = false;
    } catch {
        document.getElementById('contenidoHero').innerHTML =
            `<div style="text-align:center;padding:4rem 1rem;color:rgba(255,255,255,0.4)">
                No se pudo cargar la barbería. <br><a href="../index.html" style="color:#D4AF37;">Volver al inicio</a>
            </div>`;
    }
}

function urlReserva(extra = {}) {
    const q = new URLSearchParams({ barberiaId: BARBERIA_ID, ...extra });
    return `${Auth.getBasePath()}/reserva.html?${q}`;
}

function renderizarHero(b, promedio) {
    const base = Auth.getBasePath();
    const redondeado = Math.min(5, Math.max(0, Math.round(promedio)));
    const estrellas = '★'.repeat(redondeado) + '☆'.repeat(5 - redondeado);
    const fondo = Utils.urlSegura(b.logoUrl);

    document.getElementById('contenidoHero').innerHTML = `
        <div class="hero">
            ${fondo ? `<img src="${Utils.esc(fondo)}" class="hero-bg" alt="">` : ''}
            <div class="hero-content">
                <span class="hero-badge">BARBERÍA VERIFICADA</span>
                <h1 class="hero-titulo">${Utils.esc(b.nombre)}</h1>
                <p class="hero-desc">${Utils.esc(b.descripcion || '')}</p>
                <div class="hero-meta">
                    <span>${ICONOS.ubicacion} ${Utils.esc(b.direccion || 'Ubicación central')}</span>
                    ${b.telefono ? `<span>${ICONOS.telefono} ${Utils.esc(b.telefono)}</span>` : ''}
                    <span class="stars-display" aria-label="Calificación ${promedio.toFixed(1)} de 5">
                        ${estrellas} <small style="color:rgba(255,255,255,0.6)">(${promedio.toFixed(1)})</small>
                    </span>
                </div>
                <div class="hero-actions">
                    <a href="${Utils.esc(urlReserva())}" class="btn-primary">${ICONOS.calendario} Agendar Cita</a>
                    <a href="${base}/pages/tienda.html?barberiaId=${BARBERIA_ID}" class="btn-secondary">${ICONOS.bolsa} Ver Tienda</a>
                </div>
            </div>
        </div>`;
}

function renderizarBarberos(barberos) {
    const grid = document.getElementById('gridBarberos');
    if (!barberos.length) { grid.innerHTML = '<p class="empty">No hay barberos disponibles.</p>'; return; }

    grid.innerHTML = barberos.map(b => {
        const foto = Utils.urlSegura(b.fotoUrl);
        const nombre = `${b.nombre || ''} ${b.apellido || ''}`.trim();
        const avatar = foto
            ? `<img src="${Utils.esc(foto)}" alt="" class="barbero-avatar" loading="lazy">`
            : `<div class="barbero-avatar-inicial" aria-hidden="true">${Utils.esc((b.nombre || 'B')[0])}</div>`;
        return `
            <a class="barbero-card" href="${Utils.esc(urlReserva({ barberoId: Number(b.id) }))}"
               aria-label="Agendar con ${Utils.esc(nombre)}">
                ${avatar}
                <div class="barbero-nombre">${Utils.esc(nombre)}</div>
                <div class="barbero-rol">Barbero Profesional</div>
                <div class="barbero-cta">Agendar con él →</div>
            </a>`;
    }).join('');
}

function renderizarServicios(servicios) {
    const grid = document.getElementById('gridServicios');
    if (!servicios.length) { grid.innerHTML = '<p class="empty">No hay servicios disponibles.</p>'; return; }

    grid.innerHTML = servicios.map(s => `
        <div class="servicio-card">
            <div class="servicio-nombre">${Utils.esc(s.nombre)}</div>
            <div class="servicio-desc">${Utils.esc(s.descripcion || '')}</div>
            <div class="servicio-footer">
                <span class="servicio-precio">${Utils.dinero(s.precio)}</span>
                <span class="servicio-duracion">${ICONOS.reloj} ${Number(s.duracionMinutos) || 0} min</span>
            </div>
            <a class="servicio-reservar" href="${Utils.esc(urlReserva({ servicioId: Number(s.id) }))}">Reservar este servicio →</a>
        </div>`).join('');
}

function renderizarProductos(productos) {
    const grid = document.getElementById('gridProductos');
    document.getElementById('linkTienda').href = `${Auth.getBasePath()}/pages/tienda.html?barberiaId=${BARBERIA_ID}`;
    if (!productos.length) { grid.innerHTML = '<p class="empty">Esta barbería aún no tiene productos a la venta.</p>'; return; }

    // Primero los que tienen stock
    const destacados = [...productos].sort((a, b) => (b.stock > 0) - (a.stock > 0)).slice(0, MAX_PRODUCTOS_PREVIA);
    grid.innerHTML = destacados.map(p => {
        const img = Utils.urlSegura(p.imagenUrl);
        const stock = Number(p.stock) || 0;
        return `
            <div class="producto-card">
                ${img ? `<img class="producto-img" src="${Utils.esc(img)}" alt="${Utils.esc(p.nombre)}" loading="lazy">` : '<div class="producto-img"></div>'}
                <div class="producto-body">
                    <span class="producto-categoria">${Utils.esc((p.categoria || '').replaceAll('_', ' '))}</span>
                    <span class="producto-nombre">${Utils.esc(p.nombre)}</span>
                    <div class="producto-footer">
                        <span class="servicio-precio">${Utils.dinero(p.precio)}</span>
                        <span class="producto-stock ${stock === 0 ? 'agotado' : ''}">${stock === 0 ? 'Agotado' : `${stock} disp.`}</span>
                    </div>
                </div>
            </div>`;
    }).join('');
}

/**
 * Muestra la sección solo si la barbería la habilitó y hay planes activos.
 * Si no, la sección y su enlace de navegación permanecen ocultos.
 */
function renderizarSuscripciones(barberia, planes) {
    const activos = (planes || []).filter(p => p.activa !== false);
    const mostrar = barberia.suscripcionesHabilitadas === true && activos.length > 0;
    document.getElementById('secSuscripciones').hidden = !mostrar;
    document.getElementById('navSuscripciones').hidden = !mostrar;
    if (!mostrar) return;

    document.getElementById('gridSuscripciones').innerHTML = activos.map(p => {
        const precios = [];
        if (p.precioMensual != null) precios.push(`${Utils.dinero(p.precioMensual)} <small>/ mes</small>`);
        if (p.precioPorCita != null) precios.push(`${Utils.dinero(p.precioPorCita)} <small>/ cita</small>`);
        const tipo = { MENSUAL: 'Mensual', POR_CITA: 'Por cita', AMBAS: 'Mensual o por cita' }[p.tipo] || '';
        return `
            <article class="sub-card">
                <div class="sub-tipo">${Utils.esc(tipo)}</div>
                <h3 class="sub-nombre">${Utils.esc(p.nombre)}</h3>
                <div class="sub-precio">${precios.join(' · ')}</div>
                ${p.descripcion ? `<p class="sub-desc">${Utils.esc(p.descripcion)}</p>` : ''}
            </article>`;
    }).join('');
}

function renderizarCalificaciones(cals) {
    const lista = document.getElementById('listaCalificaciones');
    if (!cals.length) { lista.innerHTML = '<p class="empty">Aún no hay calificaciones para esta barbería.</p>'; return; }

    lista.innerHTML = cals.slice(0, 6).map(c => {
        const n = Math.min(5, Math.max(0, Number(c.estrellasBarberia) || 0));
        return `
            <div class="calificacion-card">
                <div class="cal-header">
                    <span class="cal-autor">Cliente Verificado</span>
                    <span class="cal-estrellas" aria-label="${n} de 5 estrellas">${'★'.repeat(n)}${'☆'.repeat(5 - n)}</span>
                </div>
                ${c.comentario ? `<p class="cal-comentario">"${Utils.esc(c.comentario)}"</p>` : ''}
            </div>`;
    }).join('');
}
