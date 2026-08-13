const BARBERIA_ID = new URLSearchParams(location.search).get('id');

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();

    if (!BARBERIA_ID) {
        window.location.href = Auth.getBasePath() + '/index.html';
        return;
    }
    await cargarBarberia();
});

async function cargarBarberia() {
    try {
        const [barberia, servicios, barberos, calificaciones, promedioData] = await Promise.all([
            Api.get(`/barberias/${BARBERIA_ID}`),
            Api.get(`/servicios/barberia/${BARBERIA_ID}`),
            Api.get(`/empleados/barberos/${BARBERIA_ID}`),
            Api.get(`/calificaciones/barberia/${BARBERIA_ID}`),
            Api.get(`/calificaciones/promedio/${BARBERIA_ID}`)
        ]);

        renderizarHero(barberia, promedioData.promedio || 0);
        renderizarServicios(servicios.filter(s => s.activo));
        renderizarBarberos(barberos);
        renderizarCalificaciones(calificaciones);
        document.getElementById('contenidoMain').style.display = 'block';

    } catch (err) {
        document.getElementById('contenidoHero').innerHTML =
            `<div style="text-align:center;padding:4rem;color:rgba(255,255,255,0.4)">
                No se pudo cargar la barbería. <br><a href="../index.html" style="color:#D4AF37;">Volver al inicio</a>
            </div>`;
    }
}

function renderizarHero(b, promedio) {
    const base = Auth.getBasePath();
    const estrellas = '★'.repeat(Math.round(promedio)) + '☆'.repeat(5 - Math.round(promedio));

    document.getElementById('contenidoHero').innerHTML = `
        <div class="hero">
            ${b.logoUrl ? `<img src="${b.logoUrl}" class="hero-bg" alt="fondo">` : ''}
            <div class="hero-content">
                <span class="hero-badge">BARBERÍA VERIFICADA</span>
                <h1 class="hero-titulo">${b.nombre}</h1>
                <p class="hero-desc">${b.descripcion || ''}</p>
                <div class="hero-meta">
                    <span>
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>
                        ${b.direccion || 'Ubicación central'}
                    </span>
                    <span>
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                        ${b.telefono || ''}
                    </span>
                    <span class="stars-display">
                        ${estrellas} <small style="color:rgba(255,255,255,0.6)">(${promedio.toFixed(1)})</small>
                    </span>
                </div>
                <div class="hero-actions">
                    <a href="${base}/reserva.html?barberiaId=${BARBERIA_ID}" class="btn-primary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                        Agendar Cita
                    </a>
                    <a href="${base}/pages/tienda.html?barberiaId=${BARBERIA_ID}" class="btn-secondary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
                        Ver Tienda
                    </a>
                </div>
            </div>
        </div>
    `;
}

function renderizarServicios(servicios) {
    const grid = document.getElementById('gridServicios');
    if (!servicios.length) { grid.innerHTML = `<p class="empty">No hay servicios disponibles.</p>`; return; }

    grid.innerHTML = servicios.map(s => `
        <div class="servicio-card">
            <div class="servicio-nombre">${s.nombre}</div>
            <div class="servicio-desc">${s.descripcion || ''}</div>
            <div class="servicio-footer">
                <span class="servicio-precio">$${s.precio.toLocaleString()}</span>
                <span class="servicio-duracion">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    ${s.duracionMinutos} min
                </span>
            </div>
        </div>
    `).join('');
}

function renderizarBarberos(barberos) {
    const grid = document.getElementById('gridBarberos');
    if (!barberos.length) { grid.innerHTML = `<p class="empty">No hay barberos disponibles.</p>`; return; }

    grid.innerHTML = barberos.map(b => {
        const avatarHtml = b.fotoUrl
            ? `<img src="${b.fotoUrl}" alt="${b.nombre}" class="barbero-avatar">`
            : `<div class="barbero-avatar-inicial">${(b.nombre || 'B')[0]}</div>`;
        return `
            <div class="barbero-card">
                ${avatarHtml}
                <div class="barbero-nombre">${b.nombre} ${b.apellido}</div>
                <div class="barbero-rol">Barbero Profesional</div>
            </div>
        `;
    }).join('');
}

function renderizarCalificaciones(cals) {
    const lista = document.getElementById('listaCalificaciones');
    if (!cals.length) { lista.innerHTML = `<p class="empty">Aún no hay calificaciones para esta barbería.</p>`; return; }

    lista.innerHTML = cals.slice(0, 6).map(c => {
        const estrellas = '★'.repeat(c.estrellasBarberia) + '☆'.repeat(5 - c.estrellasBarberia);
        return `
            <div class="calificacion-card">
                <div class="cal-header">
                    <span class="cal-autor">Cliente Verificado</span>
                    <span class="cal-estrellas">${estrellas}</span>
                </div>
                ${c.comentario ? `<p class="cal-comentario">"${c.comentario}"</p>` : ''}
            </div>
        `;
    }).join('');
}
