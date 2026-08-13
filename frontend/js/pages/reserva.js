let datos = { barberiaId: null, barberoId: null, servicios: [], fecha: null, hora: null };
let barberiaSeleccionada = null;
let barberoSeleccionado = null;
let serviciosDisponibles = [];

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();

    if (!Auth.estaAutenticado()) {
        sessionStorage.setItem('redirectAfterLogin', window.location.href);
        window.location.href = 'pages/login.html';
        return;
    }

    // Fecha mínima = hoy
    const hoy = new Date().toISOString().split('T')[0];
    document.getElementById('fechaCita').min = hoy;
    document.getElementById('fechaCita').value = hoy;

    // Generar horarios de 9:00 a 19:30
    const selectHora = document.getElementById('horaCita');
    for (let h = 9; h <= 19; h++) {
        ['00','30'].forEach(m => {
            if (h === 19 && m === '30') return;
            const opt = document.createElement('option');
            opt.value = `${String(h).padStart(2,'0')}:${m}`;
            opt.textContent = `${String(h).padStart(2,'0')}:${m} hrs`;
            selectHora.appendChild(opt);
        });
    }

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('barberiaId')) {
        datos.barberiaId = parseInt(urlParams.get('barberiaId'));
    }

    await cargarBarberias();

    if (datos.barberiaId) {
        irPaso(2);
    }
});

let pasoActual = 1;

function irPaso(paso) {
    if (paso > 1 && !datos.barberiaId) {
        showAlert('alertBarberia', 'Selecciona una barbería para continuar.');
        return;
    }
    if (paso > 2 && !datos.barberoId) {
        showAlert('alertBarbero', 'Selecciona un barbero para continuar.');
        return;
    }
    if (paso > 3 && datos.servicios.length === 0) {
        showAlert('alertServicios', 'Selecciona al menos un servicio.');
        return;
    }
    if (paso > 4) {
        const f = document.getElementById('fechaCita').value;
        const h = document.getElementById('horaCita').value;
        if (!f || !h) {
            showAlert('alertFecha', 'Selecciona fecha y hora.');
            return;
        }
        datos.fecha = f;
        datos.hora = h;
        datos.notas = document.getElementById('notasCita').value;
        renderResumen();
    }

    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById(`panel${paso}`)?.classList.add('active');

    for (let i = 1; i <= 5; i++) {
        const s = document.getElementById(`step${i}`);
        if (s) {
            s.classList.remove('active', 'done');
            if (i < paso) s.classList.add('done');
            if (i === paso) s.classList.add('active');
        }
    }

    pasoActual = paso;

    if (paso === 2 && datos.barberiaId) cargarBarberos();
    if (paso === 3 && datos.barberiaId) cargarServicios();
}

function showAlert(id, msg) {
    const el = document.getElementById(id);
    el.textContent = msg;
    el.classList.add('visible');
    setTimeout(() => el.classList.remove('visible'), 4000);
}

async function cargarBarberias() {
    const grid = document.getElementById('gridBarberias');
    try {
        const barberias = await Api.barberias.listar();
        grid.innerHTML = barberias.map(b => `
            <div class="selection-card ${datos.barberiaId === b.id ? 'selected' : ''}"
                 onclick="seleccionarBarberia(${b.id}, '${b.nombre.replace(/'/g,"\\'")}', this)">
                <div class="card-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7"/><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><path d="M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4"/><path d="M2 7h20"/><path d="M22 7v3a2 2 0 0 1-2 2v0a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7"/></svg>
                </div>
                <div class="card-nombre">${b.nombre}</div>
                <div class="card-sub">${b.direccion || 'Ubicación central'}</div>
            </div>
        `).join('');

        if (datos.barberiaId) {
            const sel = barberias.find(b => b.id === datos.barberiaId);
            if (sel) barberiaSeleccionada = sel.nombre;
        }
    } catch (err) {
        grid.innerHTML = `<div style="color:#E74C3C;">${err.message}</div>`;
    }
}

function seleccionarBarberia(id, nombre, el) {
    datos.barberiaId = id;
    datos.barberoId = null;
    datos.servicios = [];
    barberiaSeleccionada = nombre;
    document.querySelectorAll('#gridBarberias .selection-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
}

async function cargarBarberos() {
    const grid = document.getElementById('gridBarberos');
    grid.innerHTML = '<div style="color:rgba(255,255,255,0.4);">Cargando barberos...</div>';
    try {
        const barberos = await Api.empleados.listarBarberos(datos.barberiaId);
        if (!barberos.length) {
            grid.innerHTML = '<div style="color:rgba(255,255,255,0.4);">Esta barbería no tiene barberos disponibles en este momento.</div>';
            return;
        }
        grid.innerHTML = barberos.map(b => `
            <div class="selection-card ${datos.barberoId === b.id ? 'selected' : ''}"
                 onclick="seleccionarBarbero(${b.id}, '${b.nombre} ${b.apellido}', this)">
                <div class="card-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><line x1="20" y1="4" x2="8.12" y2="15.88"/><line x1="14.47" y1="14.48" x2="20" y2="20"/><line x1="8.12" y1="8.12" x2="12" y2="12"/></svg>
                </div>
                <div class="card-nombre">${b.nombre} ${b.apellido}</div>
                <div class="card-sub">Barbero Profesional</div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<div style="color:#E74C3C;">${err.message}</div>`;
    }
}

function seleccionarBarbero(id, nombre, el) {
    datos.barberoId = id;
    barberoSeleccionado = nombre;
    document.querySelectorAll('#gridBarberos .selection-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
}

async function cargarServicios() {
    const grid = document.getElementById('gridServicios');
    grid.innerHTML = '<div style="color:rgba(255,255,255,0.4);">Cargando servicios...</div>';
    try {
        serviciosDisponibles = await Api.servicios.listar(datos.barberiaId);
        if (!serviciosDisponibles.length) {
            grid.innerHTML = '<div style="color:rgba(255,255,255,0.4);">Esta barbería no tiene servicios registrados aún.</div>';
            return;
        }
        grid.innerHTML = serviciosDisponibles.map(s => `
            <div class="servicio-item ${datos.servicios.includes(s.id) ? 'selected' : ''}"
                 onclick="toggleServicio(${s.id}, this)">
                <div class="servicio-checkbox">
                    <span style="font-size:0.75rem; ${datos.servicios.includes(s.id) ? '' : 'display:none'}">✓</span>
                </div>
                <div class="servicio-info">
                    <div class="nombre">${s.nombre}</div>
                    <div class="precio">$${Number(s.precio).toLocaleString('es-CL')}</div>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<div style="color:#E74C3C;">${err.message}</div>`;
    }
}

function toggleServicio(id, el) {
    el.classList.toggle('selected');
    const checkmark = el.querySelector('.servicio-checkbox span');
    if (el.classList.contains('selected')) {
        datos.servicios.push(id);
        checkmark.style.display = '';
    } else {
        datos.servicios = datos.servicios.filter(s => s !== id);
        checkmark.style.display = 'none';
    }
}

function renderResumen() {
    const serviciosNombres = datos.servicios.map(id => {
        const s = serviciosDisponibles.find(sv => sv.id === id);
        return s ? `${s.nombre} ($${Number(s.precio).toLocaleString('es-CL')})` : '';
    });
    const total = datos.servicios.reduce((sum, id) => {
        const s = serviciosDisponibles.find(sv => sv.id === id);
        return sum + (s ? s.precio : 0);
    }, 0);

    document.getElementById('resumen').innerHTML = `
        <div class="resumen-row">
            <span class="label">Barbería</span>
            <span class="value">${barberiaSeleccionada || 'Barbería'}</span>
        </div>
        <div class="resumen-row">
            <span class="label">Barbero Asignado</span>
            <span class="value">${barberoSeleccionado || 'Barbero'}</span>
        </div>
        <div class="resumen-row">
            <span class="label">Servicios</span>
            <span class="value">${serviciosNombres.join(', ')}</span>
        </div>
        <div class="resumen-row">
            <span class="label">Fecha</span>
            <span class="value">${new Date(datos.fecha + 'T00:00:00').toLocaleDateString('es-CL', {weekday:'long', day:'numeric', month:'long'})}</span>
        </div>
        <div class="resumen-row">
            <span class="label">Hora</span>
            <span class="value">${datos.hora} hrs</span>
        </div>
        <div class="resumen-row resumen-total">
            <span class="label">Total Estimado</span>
            <span class="value">$${total.toLocaleString('es-CL')}</span>
        </div>
    `;
}

async function confirmarCita() {
    const btn = document.getElementById('btnConfirmar');
    btn.disabled = true;
    btn.textContent = 'Agendando...';

    try {
        await Api.citas.agendar({
            barberiaId: datos.barberiaId,
            empleadoId: datos.barberoId,
            fecha: datos.fecha,
            hora: datos.hora + ':00',
            notas: datos.notas || ''
        });

        document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
        document.getElementById('panelExito').classList.add('active');

    } catch (err) {
        showAlert('alertConfirmar', err.message);
        btn.disabled = false;
        btn.textContent = '✓ Confirmar Cita';
    }
}
