/**
 * Asistente de reserva en 5 pasos: barbería → barbero → servicios → fecha y hora → confirmar.
 *
 * - Se puede recorrer sin sesión; la cuenta se pide solo al confirmar.
 * - Lo elegido se guarda en sessionStorage para no perderlo al iniciar sesión o registrarse.
 * - Las horas vienen del backend (horario de la barbería, citas del barbero y duración
 *   de los servicios), así que solo se ofrecen horas que realmente se pueden reservar.
 */
const CLAVE_PENDIENTE = 'reservaPendiente';

let datos = { barberiaId: null, barberoId: null, servicios: [], fecha: null, hora: null, notas: '' };
let barberias = [];
let barberos = [];
let serviciosDisponibles = [];
let pasoActual = 1;

const $ = (id) => document.getElementById(id);
const NOMBRES_PASO = ['', 'Barbería', 'Barbero', 'Servicios', 'Fecha y hora', 'Confirmar'];

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();

    document.querySelectorAll('[data-ir]').forEach(b =>
        b.addEventListener('click', () => irPaso(Number(b.dataset.ir))));
    $('btnConfirmar').addEventListener('click', confirmarCita);
    $('fechaCita').addEventListener('change', () => {
        datos.fecha = $('fechaCita').value || null;
        datos.hora = null;
        guardarPendiente();
        void cargarHoras();
    });
    $('notasCita').addEventListener('input', () => {
        datos.notas = $('notasCita').value;
        guardarPendiente();
    });

    const hoy = Utils.fechaISO();
    const limite = new Date();
    limite.setDate(limite.getDate() + 90);
    $('fechaCita').min = hoy;
    $('fechaCita').max = Utils.fechaISO(limite);

    const params = new URLSearchParams(window.location.search);
    const idUrl = (clave) => {
        const n = Number.parseInt(params.get(clave), 10);
        return Number.isInteger(n) && n > 0 ? n : null;
    };

    const pendiente = leerPendiente();
    const reanudar = params.get('reanudar') === '1' && pendiente;
    if (reanudar) {
        datos = { ...datos, ...pendiente };
    } else {
        datos.barberiaId = idUrl('barberiaId');
        if (datos.barberiaId) {
            datos.barberoId = idUrl('barberoId');
            const servicioId = idUrl('servicioId');
            if (servicioId) datos.servicios = [servicioId];
        }
    }
    if (!datos.fecha || datos.fecha < hoy) datos.fecha = hoy;
    $('fechaCita').value = datos.fecha;
    $('notasCita').value = datos.notas || '';

    await iniciar(reanudar);
});

/** Carga lo necesario según lo que ya venga elegido y abre el paso que corresponde. */
async function iniciar(reanudar) {
    await cargarBarberias();
    if (!datos.barberiaId) return mostrarPaso(1, false);

    await cargarBarberos();
    if (!datos.barberoId) return mostrarPaso(2, false);

    await cargarServicios();
    if (reanudar && datos.servicios.length && datos.hora) {
        renderResumen();
        return mostrarPaso(5, false);
    }
    mostrarPaso(3, false);
}

// ─── Navegación ──────────────────────────────────────────────

function irPaso(paso) {
    ocultarAlertas();
    if (paso > 1 && !datos.barberiaId) return mostrarAlerta('alertBarberia', 'Elige una barbería para continuar.');
    if (paso > 2 && !datos.barberoId) return mostrarAlerta('alertBarbero', 'Elige un barbero para continuar.');
    if (paso > 3 && datos.servicios.length === 0) return mostrarAlerta('alertServicios', 'Elige al menos un servicio.');
    if (paso > 4 && !datos.hora) return mostrarAlerta('alertFecha', 'Elige una de las horas libres.');

    if (paso === 2) void cargarBarberos();
    if (paso === 3) void cargarServicios();
    if (paso === 4) void cargarHoras();
    if (paso === 5) renderResumen();
    mostrarPaso(paso, true);
}

function mostrarPaso(paso, moverFoco) {
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    $(paso === 'exito' ? 'panelExito' : `panel${paso}`)?.classList.add('active');

    for (let i = 1; i <= 5; i++) {
        const s = $(`step${i}`);
        s.classList.toggle('done', paso === 'exito' || i < paso);
        s.classList.toggle('active', i === paso);
        if (i === paso) s.setAttribute('aria-current', 'step');
        else s.removeAttribute('aria-current');
    }
    pasoActual = paso;

    if (paso !== 'exito') {
        $('pasoAnuncio').textContent = `Paso ${paso} de 5: ${NOMBRES_PASO[paso]}`;
        $(`step${paso}`).scrollIntoView({ block: 'nearest', inline: 'center' });
    }
    if (moverFoco) {
        const titulo = $(paso === 'exito' ? 'tituloExito' : `titulo${paso}`);
        titulo?.focus({ preventScroll: true });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    actualizarSubtitulo();
}

function actualizarSubtitulo() {
    const b = barberias.find(x => x.id === datos.barberiaId);
    $('reservaSub').textContent = b
        ? `En ${b.nombre}${b.direccion ? ' · ' + b.direccion : ''}`
        : 'Elige barbería, barbero, servicios y una hora libre.';
}

function mostrarAlerta(id, msg) {
    const el = $(id);
    el.textContent = msg;
    el.classList.add('visible');
}

function ocultarAlertas() {
    document.querySelectorAll('.panel .alert').forEach(a => a.classList.remove('visible'));
}

// ─── Estado pendiente (sobrevive al login) ───────────────────

function guardarPendiente() {
    try { sessionStorage.setItem(CLAVE_PENDIENTE, JSON.stringify(datos)); } catch { /* sin almacenamiento */ }
}

function leerPendiente() {
    try {
        const d = JSON.parse(sessionStorage.getItem(CLAVE_PENDIENTE) || 'null');
        if (!d || typeof d !== 'object') return null;
        return {
            barberiaId: Number.isInteger(d.barberiaId) ? d.barberiaId : null,
            barberoId: Number.isInteger(d.barberoId) ? d.barberoId : null,
            servicios: Array.isArray(d.servicios) ? d.servicios.filter(Number.isInteger) : [],
            fecha: typeof d.fecha === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d.fecha) ? d.fecha : null,
            hora: typeof d.hora === 'string' && /^\d{2}:\d{2}$/.test(d.hora) ? d.hora : null,
            notas: typeof d.notas === 'string' ? d.notas.slice(0, 500) : ''
        };
    } catch { return null; }
}

function borrarPendiente() {
    try { sessionStorage.removeItem(CLAVE_PENDIENTE); } catch { /* sin almacenamiento */ }
}

// ─── Paso 1: barbería ────────────────────────────────────────

async function cargarBarberias() {
    const grid = $('gridBarberias');
    try {
        barberias = await Api.barberias.listar();
        if (!barberias.length) return Utils.mostrarAviso(grid, 'Todavía no hay barberías disponibles.');
        if (datos.barberiaId && !barberias.some(b => b.id === datos.barberiaId)) datos.barberiaId = null;
        grid.innerHTML = barberias.map(b => tarjeta(b.id, b.nombre, b.direccion || '', datos.barberiaId === b.id)).join('');
        activarTarjetas(grid, (el) => seleccionarBarberia(Number(el.dataset.id)));
    } catch (err) {
        Utils.mostrarError(grid, mensajeCarga(err, 'las barberías'), cargarBarberias);
    }
}

function seleccionarBarberia(id) {
    if (datos.barberiaId !== id) {
        datos = { ...datos, barberiaId: id, barberoId: null, servicios: [], hora: null };
        barberos = [];
        serviciosDisponibles = [];
    }
    marcarSeleccion($('gridBarberias'), id);
    actualizarSubtitulo();
    guardarPendiente();
}

// ─── Paso 2: barbero ─────────────────────────────────────────

async function cargarBarberos() {
    const grid = $('gridBarberos');
    Utils.mostrarAviso(grid, 'Cargando barberos…');
    try {
        barberos = await Api.empleados.listarBarberos(datos.barberiaId);
        if (!barberos.length) {
            datos.barberoId = null;
            return Utils.mostrarAviso(grid, 'Esta barbería aún no tiene barberos con agenda. Prueba con otra.');
        }
        if (!barberos.some(b => b.id === datos.barberoId)) datos.barberoId = null;
        grid.innerHTML = barberos.map(b =>
            tarjeta(b.id, `${b.nombre} ${b.apellido}`, '', datos.barberoId === b.id)).join('');
        activarTarjetas(grid, (el) => seleccionarBarbero(Number(el.dataset.id)));
    } catch (err) {
        Utils.mostrarError(grid, mensajeCarga(err, 'los barberos'), cargarBarberos);
    }
}

function seleccionarBarbero(id) {
    if (datos.barberoId !== id) datos.hora = null;
    datos.barberoId = id;
    marcarSeleccion($('gridBarberos'), id);
    guardarPendiente();
}

// ─── Paso 3: servicios ───────────────────────────────────────

async function cargarServicios() {
    const grid = $('gridServicios');
    Utils.mostrarAviso(grid, 'Cargando servicios…');
    try {
        serviciosDisponibles = await Api.servicios.listar(datos.barberiaId);
        if (!serviciosDisponibles.length) {
            datos.servicios = [];
            return Utils.mostrarAviso(grid, 'Esta barbería aún no publica sus servicios.');
        }
        datos.servicios = datos.servicios.filter(id => serviciosDisponibles.some(s => s.id === id));
        grid.innerHTML = serviciosDisponibles.map(s => {
            const sel = datos.servicios.includes(s.id);
            const min = Number(s.duracionMinutos) || 30;
            return `
            <div class="servicio-item ${sel ? 'selected' : ''}" role="checkbox" tabindex="0"
                 aria-checked="${sel}" data-id="${Number(s.id)}">
                <span class="servicio-checkbox" aria-hidden="true">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                </span>
                <span class="servicio-info">
                    <span class="nombre">${Utils.esc(s.nombre)}</span>
                    ${s.descripcion ? `<span class="desc">${Utils.esc(s.descripcion)}</span>` : ''}
                    <span class="meta"><span class="precio">${Utils.dinero(s.precio)}</span> · ${min} min</span>
                </span>
            </div>`;
        }).join('');
        activarTarjetas(grid, (el) => toggleServicio(Number(el.dataset.id), el));
        pintarTotalServicios();
    } catch (err) {
        Utils.mostrarError(grid, mensajeCarga(err, 'los servicios'), cargarServicios);
    }
}

function toggleServicio(id, el) {
    const sel = !datos.servicios.includes(id);
    datos.servicios = sel ? [...datos.servicios, id] : datos.servicios.filter(s => s !== id);
    el.classList.toggle('selected', sel);
    el.setAttribute('aria-checked', String(sel));
    datos.hora = null; // cambia la duración: hay que volver a elegir hora
    pintarTotalServicios();
    guardarPendiente();
}

function elegidos() {
    return datos.servicios.map(id => serviciosDisponibles.find(s => s.id === id)).filter(Boolean);
}

function pintarTotalServicios() {
    const lista = elegidos();
    const total = lista.reduce((t, s) => t + Number(s.precio || 0), 0);
    const min = lista.reduce((t, s) => t + (Number(s.duracionMinutos) || 30), 0);
    $('serviciosTotal').textContent = lista.length
        ? `${lista.length} servicio${lista.length > 1 ? 's' : ''} · ${Utils.dinero(total)} · ${min} min aprox.`
        : '';
}

// ─── Paso 4: fecha y hora ────────────────────────────────────

let consultaHoras = 0;

async function cargarHoras() {
    const box = $('horasBox');
    if (!datos.fecha) return Utils.mostrarAviso(box, 'Elige una fecha para ver las horas libres.');
    const id = ++consultaHoras;
    Utils.mostrarAviso(box, 'Buscando horas libres…');
    try {
        const disp = await Api.barberias.disponibilidad(datos.barberiaId, datos.barberoId, datos.fecha, datos.servicios);
        if (id !== consultaHoras) return; // llegó una respuesta más nueva
        pintarHoras(disp);
    } catch (err) {
        if (id !== consultaHoras) return;
        Utils.mostrarError(box, mensajeCarga(err, 'las horas libres'), cargarHoras);
    }
}

function pintarHoras(disp) {
    const box = $('horasBox');
    const fechaTxt = fechaLarga(datos.fecha);
    if (!disp.abierto) {
        datos.hora = null;
        return Utils.mostrarAviso(box, `La barbería no atiende el ${fechaTxt}. Elige otro día.`);
    }
    const libres = disp.horas.filter(h => h.disponible);
    if (datos.hora && !libres.some(h => h.hora === datos.hora)) datos.hora = null;
    if (!libres.length) {
        return Utils.mostrarAviso(box, `No quedan horas libres el ${fechaTxt} con este barbero. Prueba otro día u otro barbero.`);
    }

    // Hoy no tiene sentido mostrar las horas que ya pasaron; solo se tachan las tomadas
    if (datos.fecha === Utils.fechaISO()) {
        const d = new Date();
        const ahora = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
        disp = { ...disp, horas: disp.horas.filter(h => h.hora > ahora) };
    }

    const grupos = [
        ['Mañana', h => h < '13:00'],
        ['Tarde', h => h >= '13:00' && h < '18:00'],
        ['Noche', h => h >= '18:00']
    ];
    const html = grupos.map(([nombre, filtro]) => {
        const horas = disp.horas.filter(h => filtro(h.hora));
        if (!horas.length) return '';
        return `
        <fieldset class="horas-grupo">
            <legend>${nombre}</legend>
            <div class="horas-grid">
                ${horas.map(h => `
                <label class="hora-chip ${h.disponible ? '' : 'ocupada'}">
                    <input type="radio" name="hora" value="${Utils.esc(h.hora)}"
                        ${h.disponible ? '' : 'disabled'} ${datos.hora === h.hora ? 'checked' : ''}>
                    <span>${Utils.esc(h.hora)}</span>
                </label>`).join('')}
            </div>
        </fieldset>`;
    }).join('');

    box.innerHTML = `
        <p class="horas-info">Atiende de ${Utils.esc(disp.apertura)} a ${Utils.esc(disp.cierre)} · tu cita dura ${Number(disp.duracionMinutos)} min aprox.</p>
        ${html}`;
    box.querySelectorAll('input[name="hora"]').forEach(r => r.addEventListener('change', () => {
        datos.hora = r.value;
        $('alertFecha').classList.remove('visible');
        guardarPendiente();
    }));
}

// ─── Paso 5: resumen y confirmación ──────────────────────────

function renderResumen() {
    const barberia = barberias.find(b => b.id === datos.barberiaId);
    const barbero = barberos.find(b => b.id === datos.barberoId);
    const lista = elegidos();
    const total = lista.reduce((t, s) => t + Number(s.precio || 0), 0);
    const min = lista.reduce((t, s) => t + (Number(s.duracionMinutos) || 30), 0);
    const fila = (label, valor, extra = '') =>
        `<div class="resumen-row ${extra}"><dt class="label">${label}</dt><dd class="value">${valor}</dd></div>`;

    $('resumen').innerHTML = [
        fila('Barbería', Utils.esc(barberia?.nombre || '—')),
        fila('Barbero', Utils.esc(barbero ? `${barbero.nombre} ${barbero.apellido}` : '—')),
        fila('Servicios', lista.map(s => Utils.esc(s.nombre)).join(', ') || '—'),
        fila('Cuándo', `${Utils.esc(fechaLarga(datos.fecha))}, ${Utils.esc(datos.hora || '—')} hrs`),
        fila('Duración', `${min} min aprox.`),
        fila('Total', Utils.dinero(total), 'resumen-total')
    ].join('');

    const conSesion = Auth.estaAutenticado();
    $('avisoSesion').hidden = conSesion;
    $('btnConfirmar').textContent = conSesion ? 'Confirmar cita' : 'Entrar y confirmar';
}

async function confirmarCita() {
    ocultarAlertas();
    guardarPendiente();

    if (!Auth.estaAutenticado()) {
        Auth.guardarDestino(`${window.location.pathname}?reanudar=1`);
        window.location.href = 'pages/login.html';
        return;
    }
    if (!Auth.esCliente()) {
        return mostrarAlerta('alertConfirmar', 'Las reservas se hacen con una cuenta de cliente. Tu cuenta es de personal de barbería.');
    }

    const btn = $('btnConfirmar');
    btn.disabled = true;
    btn.textContent = 'Reservando…';
    try {
        await Api.citas.agendar({
            barberiaId: datos.barberiaId,
            empleadoId: datos.barberoId,
            fecha: datos.fecha,
            hora: datos.hora + ':00',
            servicios: datos.servicios,
            notas: datos.notas || ''
        });
        const barbero = barberos.find(b => b.id === datos.barberoId);
        $('exitoDetalle').textContent =
            `${fechaLarga(datos.fecha)}, ${datos.hora} hrs${barbero ? ' con ' + barbero.nombre : ''}. Te esperamos.`;
        borrarPendiente();
        mostrarPaso('exito', true);
    } catch (err) {
        if (err.status === 409) {
            datos.hora = null;
            guardarPendiente();
            mostrarAlerta('alertConfirmar', 'Alguien tomó esa hora recién. Vuelve al paso anterior y elige otra.');
        } else {
            mostrarAlerta('alertConfirmar', err.message || 'No pudimos reservar. Inténtalo de nuevo.');
        }
        btn.disabled = false;
        btn.textContent = 'Confirmar cita';
    }
}

// ─── Utilidades de la vista ──────────────────────────────────

function tarjeta(id, nombre, sub, seleccionada) {
    return `
    <div class="selection-card ${seleccionada ? 'selected' : ''}" role="radio" tabindex="0"
         aria-checked="${seleccionada}" data-id="${Number(id)}">
        <span class="card-nombre">${Utils.esc(nombre)}</span>
        ${sub ? `<span class="card-sub">${Utils.esc(sub)}</span>` : ''}
    </div>`;
}

function marcarSeleccion(grid, id) {
    grid.querySelectorAll('.selection-card').forEach(c => {
        const sel = Number(c.dataset.id) === id;
        c.classList.toggle('selected', sel);
        c.setAttribute('aria-checked', String(sel));
    });
}

function mensajeCarga(err, que) {
    return err.sinConexion
        ? `No pudimos cargar ${que}. Revisa tu conexión e inténtalo de nuevo.`
        : (err.message || `No pudimos cargar ${que}.`);
}

function fechaLarga(iso) {
    if (!iso) return '—';
    return new Date(iso + 'T00:00:00').toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' });
}

/**
 * Asocia clic y teclado (Enter/Espacio) a las tarjetas seleccionables de un grid,
 * sin usar atributos onclick con datos interpolados (evita XSS).
 */
function activarTarjetas(grid, accion) {
    grid.querySelectorAll('[data-id]').forEach(el => {
        el.addEventListener('click', () => accion(el));
        el.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); accion(el); }
        });
    });
}
