Auth.requerirRol('BARBERO', 'ADMIN_BARBERIA', 'SUPERADMIN');

let fechaActual = new Date();

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();
    actualizarLabels();
    await cargarAgenda();
});

function actualizarLabels() {
    const diasSemana = ['Domingo','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'];
    const meses = ['enero','febrero','marzo','abril','mayo','junio','julio','agosto','septiembre','octubre','noviembre','diciembre'];

    const hoy = new Date();
    const esHoy = fechaActual.toDateString() === hoy.toDateString();

    const manana = new Date(hoy);
    manana.setDate(hoy.getDate() + 1);
    const esManana = fechaActual.toDateString() === manana.toDateString();

    const ayer = new Date(hoy);
    ayer.setDate(hoy.getDate() - 1);
    const esAyer = fechaActual.toDateString() === ayer.toDateString();

    let diaStr = diasSemana[fechaActual.getDay()];
    if (esHoy) diaStr = 'Hoy';
    else if (esManana) diaStr = 'Mañana';
    else if (esAyer) diaStr = 'Ayer';

    document.getElementById('diaLabel').textContent = diaStr;
    document.getElementById('fechaLabel').textContent =
        `${diasSemana[fechaActual.getDay()]}, ${fechaActual.getDate()} de ${meses[fechaActual.getMonth()]} ${fechaActual.getFullYear()}`;

    const yyyy = fechaActual.getFullYear();
    const mm = String(fechaActual.getMonth() + 1).padStart(2, '0');
    const dd = String(fechaActual.getDate()).padStart(2, '0');
    document.getElementById('datePicker').value = `${yyyy}-${mm}-${dd}`;
}

function cambiarDia(delta) {
    fechaActual.setDate(fechaActual.getDate() + delta);
    actualizarLabels();
    cargarAgenda();
}

function irAFecha(fechaStr) {
    if (!fechaStr) return;
    const [y, m, d] = fechaStr.split('-');
    fechaActual = new Date(parseInt(y), parseInt(m) - 1, parseInt(d));
    actualizarLabels();
    cargarAgenda();
}

async function cargarAgenda() {
    const container = document.getElementById('citasContainer');
    container.innerHTML = '<div class="empty-state">Actualizando citas...</div>';

    const yyyy = fechaActual.getFullYear();
    const mm = String(fechaActual.getMonth() + 1).padStart(2, '0');
    const dd = String(fechaActual.getDate()).padStart(2, '0');
    const fechaIso = `${yyyy}-${mm}-${dd}`;

    try {
        const citas = await Api.get(`/citas/mi-agenda?fecha=${fechaIso}`);

        document.getElementById('qsPendientes').textContent  = citas.filter(c => c.estado === 'PENDIENTE').length;
        document.getElementById('qsConfirmadas').textContent = citas.filter(c => c.estado === 'CONFIRMADA').length;
        document.getElementById('qsCompletadas').textContent = citas.filter(c => c.estado === 'COMPLETADA').length;
        document.getElementById('qsCanceladas').textContent  = citas.filter(c => c.estado === 'CANCELADA').length;
        document.getElementById('citasCount').textContent    = `${citas.length} cita${citas.length !== 1 ? 's' : ''}`;

        if (!citas.length) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>
                    </div>
                    Sin citas agendadas para este día
                </div>
            `;
            return;
        }

        container.innerHTML = citas.map(c => `
            <div class="cita-item">
                <div class="cita-hora">
                    ${c.hora?.substring(0,5) || '--:--'}
                    <span class="duracion">hrs</span>
                </div>
                <div class="cita-info">
                    <div class="cita-cliente">
                        ${c.cliente?.nombre || ''} ${c.cliente?.apellido || ''}
                    </div>
                    <div class="cita-servicios">
                        <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                        ${c.cliente?.telefono || 'Sin teléfono'}
                    </div>
                    ${c.notas ? `<div class="cita-notas">"${c.notas}"</div>` : ''}
                </div>
                <div class="cita-precio">
                    ${c.precioTotal > 0 ? '$' + Number(c.precioTotal).toLocaleString('es-CL') : '—'}
                </div>
                <span class="estado-badge estado-${c.estado}">${c.estado}</span>
                ${c.estado === 'CONFIRMADA' || c.estado === 'PENDIENTE' ? `
                    <button class="btn-completar" onclick="completarCita(${c.id})">
                        ✓ Marcar Completada
                    </button>
                ` : ''}
            </div>
        `).join('');

    } catch (err) {
        container.innerHTML = `<div class="empty-state" style="color:#E74C3C;">${err.message}</div>`;
    }
}

async function completarCita(citaId) {
    if (!confirm('¿Marcar esta cita como completada?')) return;
    try {
        await Api.citas.cambiarEstado(citaId, 'COMPLETADA');
        await cargarAgenda();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}
