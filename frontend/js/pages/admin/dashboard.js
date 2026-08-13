// ─── Guardia de autenticación ─────────────────────────────────
if (!Auth.requerirRol('ADMIN_BARBERIA')) { /* redirige automáticamente */ }

const usuario = Auth.getUsuario();
const barberiaId = Auth.getBarberiaId();

// ─── Inicialización ───────────────────────────────────────────
document.getElementById('userInitials').textContent =
    usuario?.nombre?.charAt(0)?.toUpperCase() || 'A';
document.getElementById('userName').textContent = usuario?.nombre || 'Admin';

const hoy = new Date().toISOString().split('T')[0];
document.getElementById('fechaAgenda').value = hoy;
document.getElementById('fechaAgendaFull').value = hoy;
document.getElementById('fechaHoy').textContent =
    new Date().toLocaleDateString('es-CL', { weekday:'long', day:'numeric', month:'long' });

// ─── Navegación entre secciones ───────────────────────────────
const secciones = ['dashboard','agenda','empleados','servicios','productos','suscripciones','calificaciones'];

function cambiarSeccion(seccion) {
    secciones.forEach(s => {
        document.getElementById(`sec-${s}`).style.display = s === seccion ? '' : 'none';
    });
    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.toggle('active', el.dataset.section === seccion);
    });
    document.getElementById('pageTitle').textContent = {
        dashboard: 'Dashboard', agenda: 'Agenda del Día', empleados: 'Empleados',
        servicios: 'Servicios', productos: 'Productos', suscripciones: 'Suscripciones',
        calificaciones: 'Calificaciones'
    }[seccion] || seccion;

    // Cargar datos según sección
    if (seccion === 'empleados') cargarEmpleados();
    if (seccion === 'servicios') cargarServicios();
    if (seccion === 'agenda') cargarAgendaFull();
    if (seccion === 'calificaciones') cargarCalificaciones();
}

// ─── Dashboard ────────────────────────────────────────────────
async function cargarDashboard() {
    try {
        // Nombre de la barbería
        const barb = await Api.barberias.obtener(barberiaId);
        document.getElementById('sidebarNombreBarberia').textContent = barb.nombre;

        // Agenda de hoy
        const citas = await Api.citas.agendaBarberia(barberiaId, hoy);
        document.getElementById('statCitasHoy').textContent = citas.length;
        document.getElementById('statCompletadas').textContent =
            citas.filter(c => c.estado === 'COMPLETADA').length;

        // Preview de citas (primeras 5)
        renderAgendaPreview(citas.slice(0, 5));

        // Chart
        renderChartEstados(citas);

        // Empleados
        const empleados = await Api.empleados.listar(barberiaId);
        document.getElementById('statEmpleados').textContent = empleados.length;

        // Calificación promedio
        const prom = await Api.calificaciones.promedioBarberia(barberiaId);
        document.getElementById('statCalificacion').textContent =
            prom.promedio ? `${prom.promedio.toFixed(1)} ★` : '—';

    } catch (err) {
        console.error('Error cargando dashboard:', err.message);
    }
}

function renderAgendaPreview(citas) {
    const container = document.getElementById('agenda-preview');
    if (!citas.length) {
        container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted);">Sin citas para hoy</div>';
        return;
    }
    container.innerHTML = citas.map(c => `
        <div class="agenda-item">
            <div class="agenda-hora">${c.hora?.substring(0,5) || '--:--'}</div>
            <div class="agenda-cliente">
                <div class="nombre">${c.cliente?.nombre || ''} ${c.cliente?.apellido || ''}</div>
                <div class="servicio">Barbero: ${c.empleado?.nombre || '—'}</div>
            </div>
            <span class="estado-badge estado-${c.estado}">${c.estado}</span>
            ${c.estado === 'PENDIENTE' ? `
                <button class="btn-confirmar" onclick="confirmarCita(${c.id})">Confirmar</button>
            ` : ''}
        </div>
    `).join('');
}

function renderChartEstados(citas) {
    const counts = { PENDIENTE: 0, CONFIRMADA: 0, COMPLETADA: 0, CANCELADA: 0 };
    citas.forEach(c => counts[c.estado]++);

    const ctx = document.getElementById('chartEstados').getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Pendiente', 'Confirmada', 'Completada', 'Cancelada'],
            datasets: [{
                data: Object.values(counts),
                backgroundColor: ['#F39C12', '#3498DB', '#2ECC71', '#E74C3C'],
                borderWidth: 0,
            }]
        },
        options: {
            cutout: '70%',
            plugins: {
                legend: { labels: { color: 'rgba(255,255,255,0.6)', font: { size: 11 } } }
            }
        }
    });
}

// ─── Agenda ───────────────────────────────────────────────────
async function cargarAgendaDia() {
    const fecha = document.getElementById('fechaAgenda').value || hoy;
    document.getElementById('fechaAgendaFull').value = fecha;
    await cargarAgendaFull();
}

async function cargarAgendaFull() {
    const fecha = document.getElementById('fechaAgendaFull').value || hoy;
    const container = document.getElementById('lista-agenda-full');
    container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted);">Cargando...</div>';
    try {
        const citas = await Api.citas.agendaBarberia(barberiaId, fecha);
        if (!citas.length) {
            container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted);">Sin citas para esta fecha</div>';
            return;
        }
        container.innerHTML = citas.map(c => `
            <div class="agenda-item">
                <div class="agenda-hora">${c.hora?.substring(0,5) || '--:--'}</div>
                <div class="agenda-cliente">
                    <div class="nombre">${c.cliente?.nombre || ''} ${c.cliente?.apellido || ''}</div>
                    <div class="servicio">${c.notas || 'Sin notas'}</div>
                </div>
                <div class="agenda-barbero">${c.empleado?.nombre || '—'} ${c.empleado?.apellido || ''}</div>
                <span class="estado-badge estado-${c.estado}">${c.estado}</span>
                ${c.estado === 'PENDIENTE' ? `
                    <button class="btn-confirmar" onclick="confirmarCita(${c.id})">Confirmar</button>
                ` : ''}
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = `<div style="padding:2rem; text-align:center; color:var(--danger);">${err.message}</div>`;
    }
}

async function confirmarCita(citaId) {
    try {
        await Api.citas.cambiarEstado(citaId, 'CONFIRMADA');
        cargarAgendaFull();
        if (document.getElementById('sec-dashboard').style.display !== 'none') cargarDashboard();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// ─── Empleados ────────────────────────────────────────────────
async function cargarEmpleados() {
    const tbody = document.getElementById('tablaEmpleados');
    try {
        const empleados = await Api.empleados.listar(barberiaId);
        if (!empleados.length) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">Sin empleados registrados</td></tr>';
            return;
        }
        tbody.innerHTML = empleados.map(e => `
            <tr>
                <td><strong>${e.nombre} ${e.apellido}</strong></td>
                <td style="color:var(--text-muted);">${e.rut}</td>
                <td style="color:var(--text-muted);">${e.email}</td>
                <td style="color:var(--text-muted);">${e.telefono || '—'}</td>
                <td><span class="rol-badge rol-${e.rol}">${e.rol === 'BARBERO' ? 'Barbero' : 'Administrador'}</span></td>
                <td>
                    <button class="btn btn-danger-outline" style="padding:0.3rem 0.75rem; font-size:0.75rem;"
                        onclick="desactivarEmpleado(${e.id})">
                        Desactivar
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--danger); padding:2rem;">${err.message}</td></tr>`;
    }
}

async function desactivarEmpleado(id) {
    if (!confirm('¿Desactivar este empleado?')) return;
    try {
        await Api.empleados.desactivar(id);
        cargarEmpleados();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// ─── Servicios ────────────────────────────────────────────────
async function cargarServicios() {
    const tbody = document.getElementById('tablaServicios');
    try {
        const servicios = await Api.servicios.listar(barberiaId);
        if (!servicios.length) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:2rem;">Sin servicios. ¡Agrega el primero!</td></tr>';
            return;
        }
        tbody.innerHTML = servicios.map(s => `
            <tr>
                <td><strong>${s.nombre}</strong></td>
                <td style="color:var(--text-muted);">${s.descripcion || '—'}</td>
                <td style="color:var(--gold);">$${s.precio?.toLocaleString('es-CL')}</td>
                <td style="color:var(--text-muted);">${s.duracionMinutos} min</td>
                <td>
                    <button class="btn btn-danger-outline" style="padding:0.3rem 0.75rem; font-size:0.75rem;"
                        onclick="desactivarServicio(${s.id})">
                        Eliminar
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="color:var(--danger); padding:2rem;">${err.message}</td></tr>`;
    }
}

async function desactivarServicio(id) {
    if (!confirm('¿Eliminar este servicio?')) return;
    try {
        await Api.servicios.desactivar(id);
        cargarServicios();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// ─── Calificaciones ───────────────────────────────────────────
async function cargarCalificaciones() {
    const container = document.getElementById('lista-calificaciones');
    try {
        const cals = await Api.calificaciones.listar(barberiaId);
        if (!cals.length) {
            container.innerHTML = '<div style="text-align:center; color:var(--text-muted); padding:2rem;">Aún no hay calificaciones.</div>';
            return;
        }
        container.innerHTML = cals.map(c => `
            <div style="padding:1rem; border-bottom:1px solid var(--border);">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <span style="color:var(--gold);">${'★'.repeat(c.estrellasBarberia)}${'☆'.repeat(5-c.estrellasBarberia)}</span>
                        <span style="color:var(--text-muted); font-size:0.75rem; margin-left:0.5rem;">Barbería</span>
                        <span style="color:var(--gold); margin-left:1rem;">${'★'.repeat(c.estrellasBarbero)}${'☆'.repeat(5-c.estrellasBarbero)}</span>
                        <span style="color:var(--text-muted); font-size:0.75rem; margin-left:0.5rem;">Barbero</span>
                    </div>
                    <span style="color:var(--text-muted); font-size:0.75rem;">${new Date(c.fecha).toLocaleDateString('es-CL')}</span>
                </div>
                ${c.comentario ? `<p style="color:var(--text-secondary); font-size:0.85rem; margin-top:0.5rem;">"${c.comentario}"</p>` : ''}
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = `<div style="color:var(--danger); padding:2rem;">${err.message}</div>`;
    }
}

// ─── Modales ──────────────────────────────────────────────────
function abrirModalEmpleado() {
    document.getElementById('empError').textContent = '';
    document.getElementById('modalEmpleado').classList.add('visible');
}

function abrirModalServicio() {
    document.getElementById('srvError').textContent = '';
    document.getElementById('modalServicio').classList.add('visible');
}

function cerrarModal(id) {
    document.getElementById(id).classList.remove('visible');
}

async function crearEmpleado() {
    const datos = {
        documentoIdentidad: document.getElementById('empRut').value.trim(),
        nombre:   document.getElementById('empNombre').value.trim(),
        apellido: document.getElementById('empApellido').value.trim(),
        email:    document.getElementById('empEmail').value.trim(),
        telefono: document.getElementById('empTelefono').value.trim(),
        password: document.getElementById('empPassword').value,
        rol:      document.getElementById('empRol').value,
    };

    if (!datos.rut || !datos.nombre || !datos.email || !datos.password) {
        document.getElementById('empError').textContent = 'Completa los campos obligatorios.';
        return;
    }

    try {
        await Api.empleados.crear(datos);
        cerrarModal('modalEmpleado');
        cargarEmpleados();
    } catch (err) {
        document.getElementById('empError').textContent = err.message;
    }
}

async function crearServicio() {
    const datos = {
        nombre:          document.getElementById('srvNombre').value.trim(),
        descripcion:     document.getElementById('srvDescripcion').value.trim(),
        precio:          parseFloat(document.getElementById('srvPrecio').value),
        duracionMinutos: parseInt(document.getElementById('srvDuracion').value) || 30,
    };

    if (!datos.nombre || !datos.precio) {
        document.getElementById('srvError').textContent = 'Nombre y precio son requeridos.';
        return;
    }

    try {
        await Api.servicios.crear(datos);
        cerrarModal('modalServicio');
        cargarServicios();
    } catch (err) {
        document.getElementById('srvError').textContent = err.message;
    }
}

// Cerrar modal al hacer click fuera
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.classList.remove('visible');
    });
});

// ─── Iniciar dashboard ────────────────────────────────────────
cargarDashboard();
