// Guardia de seguridad
Auth.requerirRol('ADMIN_BARBERIA');
const barberiaId = Auth.getBarberiaId();

// ─── Inicialización ───────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const u = Auth.getUsuario();
    if (u) {
        document.getElementById('userName').textContent = u.nombre;
        document.getElementById('userAvatar').textContent = u.nombre.charAt(0).toUpperCase();
    }
    cargarSuscripciones();
});

// ─── Lógica de Suscripciones ──────────────────────────────────
async function cargarSuscripciones() {
    try {
        const subs = await Api.suscripciones.obtenerPorBarberia(barberiaId);
        const tbody = document.querySelector('#tablaSuscripciones tbody');
        tbody.innerHTML = '';

        if (subs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:#888;">No hay planes registrados</td></tr>';
            return;
        }

        subs.forEach(s => {
            const tr = document.createElement('tr');
            
            const btnToggle = s.activa 
                ? `<button class="btn-danger-outline" onclick="toggleEstado(${s.id})">Desactivar</button>`
                : `<button class="btn-success-outline" onclick="toggleEstado(${s.id})">Activar</button>`;

            const precioMensual = s.precioMensual != null ? `$${s.precioMensual.toLocaleString()}` : '-';
            const precioCita    = s.precioPorCita != null ? `$${s.precioPorCita.toLocaleString()}` : '-';

            const estadoBadge = s.activa 
                ? `<span class="badge badge-success">Activa</span>` 
                : `<span class="badge badge-danger">Inactiva</span>`;

            tr.innerHTML = `
                <td><strong>${s.nombre}</strong></td>
                <td><span class="badge badge-info">${s.tipo}</span></td>
                <td>${precioMensual}</td>
                <td>${precioCita}</td>
                <td>${estadoBadge}</td>
                <td>${btnToggle}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error al cargar suscripciones:", err);
    }
}

async function toggleEstado(id) {
    try {
        await Api.suscripciones.toggleEstado(id);
        cargarSuscripciones();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ─── Modal y Formulario ───────────────────────────────────────
function abrirModal() {
    document.getElementById('subError').textContent = '';
    document.getElementById('modalSuscripcion').classList.add('visible');
    togglePrecios();
}

function cerrarModal() {
    document.getElementById('modalSuscripcion').classList.remove('visible');
}

function togglePrecios() {
    const tipo = document.getElementById('subTipo').value;
    const divMensual = document.getElementById('groupMensual');
    const divCita    = document.getElementById('groupCita');

    divMensual.style.display = (tipo === 'MENSUAL' || tipo === 'AMBAS') ? 'block' : 'none';
    divCita.style.display    = (tipo === 'POR_CITA' || tipo === 'AMBAS') ? 'block' : 'none';
}

async function guardarSuscripcion() {
    const errorDiv = document.getElementById('subError');
    errorDiv.textContent = '';

    const datos = {
        barberiaId: barberiaId,
        nombre: document.getElementById('subNombre').value.trim(),
        tipo: document.getElementById('subTipo').value,
        descripcion: document.getElementById('subDescripcion').value.trim()
    };

    if (datos.tipo === 'MENSUAL' || datos.tipo === 'AMBAS') {
        const pm = parseFloat(document.getElementById('subPrecioMensual').value);
        if (isNaN(pm) || pm < 0) return errorDiv.textContent = 'Ingresa un precio mensual válido.';
        datos.precioMensual = pm;
    }

    if (datos.tipo === 'POR_CITA' || datos.tipo === 'AMBAS') {
        const pc = parseFloat(document.getElementById('subPrecioCita').value);
        if (isNaN(pc) || pc < 0) return errorDiv.textContent = 'Ingresa un precio por cita válido.';
        datos.precioPorCita = pc;
    }

    if (!datos.nombre) return errorDiv.textContent = 'El nombre es obligatorio.';

    try {
        await Api.suscripciones.crear(datos);
        cerrarModal();
        cargarSuscripciones();
        // Limpiar form
        document.getElementById('subNombre').value = '';
        document.getElementById('subDescripcion').value = '';
        document.getElementById('subPrecioMensual').value = '';
        document.getElementById('subPrecioCita').value = '';
    } catch (err) {
        errorDiv.textContent = err.message;
    }
}
