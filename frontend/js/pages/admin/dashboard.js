// ─── Guardia de autenticación ─────────────────────────────────
if (!Auth.requerirRol('ADMIN_BARBERIA')) { /* redirige automáticamente */ }

const usuario = Auth.getUsuario();
const barberiaId = Auth.getBarberiaId();

// ─── Inicialización ───────────────────────────────────────────
document.getElementById('userInitials').textContent =
    usuario?.nombre?.charAt(0)?.toUpperCase() || 'A';
document.getElementById('userName').textContent = usuario?.nombre || 'Admin';

const hoy = Utils.fechaISO();
document.getElementById('fechaAgenda').value = hoy;
document.getElementById('fechaAgendaFull').value = hoy;
document.getElementById('fechaHoy').textContent =
    new Date().toLocaleDateString('es-CL', { weekday:'long', day:'numeric', month:'long' });

// ─── Navegación entre secciones ───────────────────────────────
const secciones = ['dashboard','agenda','empleados','servicios','productos','suscripciones','calificaciones','ganancias'];

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
        calificaciones: 'Calificaciones', ganancias: 'Ganancias'
    }[seccion] || seccion;
    cerrarMenuMovil();

    // Cargar datos según sección
    if (seccion === 'empleados') void cargarEmpleados();
    if (seccion === 'servicios') void cargarServicios();
    if (seccion === 'agenda') void cargarAgendaFull();
    if (seccion === 'calificaciones') void cargarCalificaciones();
    if (seccion === 'ganancias') void iniciarGanancias();
    if (seccion === 'suscripciones') void cargarToggleSuscripciones();
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

/** Estrellas acotadas a 0..5 (evita RangeError con datos corruptos). */
function estrellas(n) {
    const v = Math.min(5, Math.max(0, Number(n) || 0));
    return '★'.repeat(v) + '☆'.repeat(5 - v);
}

function renderAgendaPreview(citas) {
    const container = document.getElementById('agenda-preview');
    if (!citas.length) {
        container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted);">Sin citas para hoy</div>';
        return;
    }
    container.innerHTML = citas.map(c => `
        <div class="agenda-item">
            <div class="agenda-hora">${Utils.esc(c.hora?.substring(0,5) || '--:--')}</div>
            <div class="agenda-cliente">
                <div class="nombre">${Utils.esc(c.cliente?.nombre)} ${Utils.esc(c.cliente?.apellido)}</div>
                <div class="servicio">Barbero: ${Utils.esc(c.empleado?.nombre || '—')}</div>
            </div>
            <span class="estado-badge estado-${Utils.esc(c.estado)}">${Utils.esc(c.estado)}</span>
            ${c.estado === 'PENDIENTE' ? `
                <button class="btn-confirmar" onclick="confirmarCita(${Number(c.id)})">Confirmar</button>
            ` : ''}
        </div>
    `).join('');
}

let chartEstados = null;

function renderChartEstados(citas) {
    const counts = { PENDIENTE: 0, CONFIRMADA: 0, COMPLETADA: 0, CANCELADA: 0 };
    citas.forEach(c => {
        if (counts[c.estado] !== undefined) counts[c.estado]++;
    });

    if (chartEstados) {
        chartEstados.destroy();
    }

    const ctx = document.getElementById('chartEstados').getContext('2d');
    chartEstados = new Chart(ctx, {
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
            responsive: true,
            maintainAspectRatio: false,
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
                <div class="agenda-hora">${Utils.esc(c.hora?.substring(0,5) || '--:--')}</div>
                <div class="agenda-cliente">
                    <div class="nombre">${Utils.esc(c.cliente?.nombre)} ${Utils.esc(c.cliente?.apellido)}</div>
                    <div class="servicio">${Utils.esc(c.notas || 'Sin notas')}</div>
                </div>
                <div class="agenda-barbero">${Utils.esc(c.empleado?.nombre || '—')} ${Utils.esc(c.empleado?.apellido)}</div>
                <span class="estado-badge estado-${Utils.esc(c.estado)}">${Utils.esc(c.estado)}</span>
                ${c.estado === 'PENDIENTE' ? `
                    <button class="btn-confirmar" onclick="confirmarCita(${Number(c.id)})">Confirmar</button>
                ` : ''}
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = `<div style="padding:2rem; text-align:center; color:var(--danger);">${Utils.esc(err.message)}</div>`;
    }
}

async function confirmarCita(citaId) {
    try {
        await Api.citas.cambiarEstado(citaId, 'CONFIRMADA');
       await cargarAgendaFull();
        if (document.getElementById('sec-dashboard').style.display !== 'none') await cargarDashboard();
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
                <td><strong>${Utils.esc(e.nombre)} ${Utils.esc(e.apellido)}</strong></td>
                <td style="color:var(--text-muted);">${Utils.esc(e.documentoIdentidad || '—')}</td>
                <td style="color:var(--text-muted);">${Utils.esc(e.email)}</td>
                <td style="color:var(--text-muted);">${Utils.esc(e.telefono || '—')}</td>
                <td><span class="rol-badge rol-${Utils.esc(e.rol)}">${e.rol === 'BARBERO' ? 'Barbero' : 'Administrador'}</span></td>
                <td>
                    <button class="btn btn-danger-outline btn-desactivar-emp" style="padding:0.3rem 0.75rem; font-size:0.75rem;"
                        data-id="${Number(e.id)}">
                        Desactivar
                    </button>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('.btn-desactivar-emp').forEach(btn => {
            btn.addEventListener('click', () => desactivarEmpleado(Number(btn.dataset.id)));
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:var(--danger); padding:2rem;">${Utils.esc(err.message)}</td></tr>`;
    }
}

async function desactivarEmpleado(id) {
    if (!confirm('¿Desactivar este empleado?')) return;
    try {
        await Api.empleados.desactivar(id);
        await cargarEmpleados();
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
                <td><strong>${Utils.esc(s.nombre)}</strong></td>
                <td style="color:var(--text-muted);">${Utils.esc(s.descripcion || '—')}</td>
                <td style="color:var(--gold);">${Utils.dinero(s.precio)}</td>
                <td style="color:var(--text-muted);">${Number(s.duracionMinutos) || 0} min</td>
                <td>
                    <button class="btn btn-danger-outline" style="padding:0.3rem 0.75rem; font-size:0.75rem;"
                        onclick="desactivarServicio(${Number(s.id)})">
                        Eliminar
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" style="color:var(--danger); padding:2rem;">${Utils.esc(err.message)}</td></tr>`;
    }
}

async function desactivarServicio(id) {
    if (!confirm('¿Eliminar este servicio?')) return;
    try {
        await Api.servicios.desactivar(id);
        await cargarServicios();
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
                        <span style="color:var(--gold);">${estrellas(c.estrellasBarberia)}</span>
                        <span style="color:var(--text-muted); font-size:0.75rem; margin-left:0.5rem;">Barbería</span>
                        <span style="color:var(--gold); margin-left:1rem;">${estrellas(c.estrellasBarbero)}</span>
                        <span style="color:var(--text-muted); font-size:0.75rem; margin-left:0.5rem;">Barbero</span>
                    </div>
                    <span style="color:var(--text-muted); font-size:0.75rem;">${new Date(c.fecha).toLocaleDateString('es-CL')}</span>
                </div>
                ${c.comentario ? `<p style="color:var(--text-secondary); font-size:0.85rem; margin-top:0.5rem;">"${Utils.esc(c.comentario)}"</p>` : ''}
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = `<div style="color:var(--danger); padding:2rem;">${Utils.esc(err.message)}</div>`;
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

    if (!datos.documentoIdentidad || !datos.nombre || !datos.email || !datos.password) {
        document.getElementById('empError').textContent = 'Completa los campos obligatorios.';
        return;
    }

    try {
        await Api.empleados.crear(datos);
        cerrarModal('modalEmpleado');
        await cargarEmpleados();
    } catch (err) {
        document.getElementById('empError').textContent = err.message;
    }
}

async function crearServicio() {
    const datos = {
        nombre:          document.getElementById('srvNombre').value.trim(),
        descripcion:     document.getElementById('srvDescripcion').value.trim(),
        precio:          Number.parseFloat(document.getElementById('srvPrecio').value),
        duracionMinutos: Number.parseInt(document.getElementById('srvDuracion').value) || 30,
    };

    if (!datos.nombre || !datos.precio) {
        document.getElementById('srvError').textContent = 'Nombre y precio son requeridos.';
        return;
    }

    try {
        await Api.servicios.crear(datos);
        cerrarModal('modalServicio');
        await cargarServicios();
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
void cargarDashboard();


// ─── Menú lateral en móvil ────────────────────────────────────
(() => {
    const sidebar = document.getElementById('sidebar');
    const btn = document.getElementById('btnMenu');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (!sidebar || !btn || !backdrop) return;
    btn.addEventListener('click', () => {
        const abierto = sidebar.classList.toggle('open');
        backdrop.hidden = !abierto;
        btn.setAttribute('aria-expanded', String(abierto));
    });
    backdrop.addEventListener('click', cerrarMenuMovil);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrarMenuMovil(); });
})();

function cerrarMenuMovil() {
    document.getElementById('sidebar')?.classList.remove('open');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (backdrop) backdrop.hidden = true;
    document.getElementById('btnMenu')?.setAttribute('aria-expanded', 'false');
}

// ─── Ganancias ────────────────────────────────────────────────
const ganEstado = { periodo: 'DIA', iniciado: false };
let chartGanancias = null;

async function iniciarGanancias() {
    if (!ganEstado.iniciado) {
        ganEstado.iniciado = true;
        const fecha = document.getElementById('ganFecha');
        fecha.value = hoy;
        fecha.addEventListener('change', () => void cargarGanancias());
        document.getElementById('ganBarbero').addEventListener('change', () => void cargarGanancias());
        document.querySelectorAll('.periodo-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                ganEstado.periodo = tab.dataset.periodo;
                document.querySelectorAll('.periodo-tab').forEach(t => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', String(t === tab));
                });
                void cargarGanancias();
            });
        });
        try {
            const barberos = await Api.empleados.listarBarberos(barberiaId);
            const select = document.getElementById('ganBarbero');
            barberos.forEach(b => {
                const opt = document.createElement('option');
                opt.value = String(Number(b.id));
                opt.textContent = `${b.nombre} ${b.apellido}`;
                select.appendChild(opt);
            });
        } catch { /* el filtro queda solo con "Todos" */ }
    }
    await cargarGanancias();
}

async function cargarGanancias() {
    const tbody = document.getElementById('tablaGanancias');
    const fecha = document.getElementById('ganFecha').value || hoy;
    const barberoId = document.getElementById('ganBarbero').value;
    try {
        const r = await Api.ganancias.barberia(barberiaId, ganEstado.periodo, fecha, barberoId);
        renderResumenGanancias(r);
        renderTablaGanancias(r);
        renderChartGanancias(r);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" style="color:var(--danger); padding:2rem;">${Utils.esc(err.message)}</td></tr>`;
    }
}

function textoRango(r) {
    const f = (iso) => new Date(iso + 'T00:00:00').toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' });
    return r.desde === r.hasta ? f(r.desde) : `${f(r.desde)} — ${f(r.hasta)}`;
}

function renderResumenGanancias(r) {
    const total = Number(r.total) || 0;
    const citas = Number(r.totalCitas) || 0;
    document.getElementById('ganRango').textContent = `Periodo: ${textoRango(r)}`;
    document.getElementById('ganTotal').textContent = Utils.dinero(total);
    document.getElementById('ganCitas').textContent = String(citas);
    document.getElementById('ganPromedio').textContent = citas ? Utils.dinero(total / citas) : '—';
}

function renderTablaGanancias(r) {
    const tbody = document.getElementById('tablaGanancias');
    const filas = r.porBarbero || [];
    if (!filas.length) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:var(--text-muted); padding:2rem;">Sin barberos para mostrar</td></tr>';
        return;
    }
    const total = Number(r.total) || 0;
    tbody.innerHTML = filas.map(g => {
        const t = Number(g.total) || 0;
        const pct = total > 0 ? Math.round((t / total) * 100) : 0;
        return `
            <tr>
                <td><strong>${Utils.esc(g.nombre)} ${Utils.esc(g.apellido)}</strong></td>
                <td>${Number(g.citas) || 0}</td>
                <td style="color:var(--gold); font-weight:700;">${Utils.dinero(t)}</td>
                <td>
                    <div class="barra-pct" title="${pct}%"><span style="width:${pct}%"></span></div>
                    <small style="color:var(--text-muted);">${pct}%</small>
                </td>
            </tr>`;
    }).join('');
}

function renderChartGanancias(r) {
    if (typeof Chart === 'undefined') return; // CDN bloqueado: la tabla sigue siendo útil
    const serie = r.serie || [];
    const etiquetas = serie.map(d => new Date(d.fecha + 'T00:00:00')
        .toLocaleDateString('es-CL', serie.length > 7 ? { day: 'numeric' } : { weekday: 'short', day: 'numeric' }));
    if (chartGanancias) chartGanancias.destroy();
    chartGanancias = new Chart(document.getElementById('chartGanancias').getContext('2d'), {
        type: 'bar',
        data: {
            labels: etiquetas,
            datasets: [{ label: 'Ganancias', data: serie.map(d => Number(d.total) || 0), backgroundColor: '#D4AF37', borderRadius: 4, maxBarThickness: 36 }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { callbacks: { label: (c) => Utils.dinero(c.parsed.y) } }
            },
            scales: {
                x: { ticks: { color: 'rgba(255,255,255,0.5)' }, grid: { display: false } },
                y: { beginAtZero: true, ticks: { color: 'rgba(255,255,255,0.5)', callback: (v) => Utils.dinero(v) }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
}

// ─── Toggle de la sección pública de suscripciones ────────────
async function cargarToggleSuscripciones() {
    const toggle = document.getElementById('toggleSubs');
    const estado = document.getElementById('toggleSubsEstado');
    try {
        const b = await Api.barberias.obtener(barberiaId);
        toggle.checked = b.suscripcionesHabilitadas === true;
        estado.textContent = toggle.checked ? 'Visible para los clientes.' : 'Oculta para los clientes.';
    } catch (err) {
        estado.textContent = err.message;
    }
    if (!toggle.dataset.listo) {
        toggle.dataset.listo = '1';
        toggle.addEventListener('change', async () => {
            toggle.disabled = true;
            try {
                await Api.barberias.configurarSuscripciones(barberiaId, toggle.checked);
                estado.textContent = toggle.checked ? 'Visible para los clientes.' : 'Oculta para los clientes.';
            } catch (err) {
                toggle.checked = !toggle.checked;
                estado.textContent = err.message;
            } finally {
                toggle.disabled = false;
            }
        });
    }
}
