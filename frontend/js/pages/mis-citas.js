Auth.requerirRol('CLIENTE');

let todasLasCitas = [];
let citaAcalificar = null;

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();
    await cargarMisCitas();
});

async function cargarMisCitas() {
    try {
        todasLasCitas = await Api.get('/citas/mis-citas');
        renderizarCitas('proximas');
    } catch (err) {
        document.getElementById('citasContenedor').innerHTML = `<div class="empty-state">No se pudieron cargar tus citas.</div>`;
    }
}

function cambiarTab(tipo, elemento) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    elemento.classList.add('active');
    renderizarCitas(tipo);
}

function renderizarCitas(tipo) {
    const contenedor = document.getElementById('citasContenedor');
    contenedor.innerHTML = '';

    let filtradas = [];
    if (tipo === 'proximas') {
        filtradas = (todasLasCitas || []).filter(c => c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA');
    } else {
        filtradas = (todasLasCitas || []).filter(c => c.estado === 'COMPLETADA' || c.estado === 'CANCELADA');
    }

    if (!filtradas || filtradas.length === 0) {
        contenedor.innerHTML = `<div class="empty-state">No tienes citas en esta sección.</div>`;
        return;
    }

    filtradas.sort((a,b) => new Date(b.fecha) - new Date(a.fecha));

    filtradas.forEach(cita => {
        const div = document.createElement('div');
        div.className = 'cita-card';

        const fechaTexto = `${cita.fecha} — ${cita.hora ? cita.hora.substring(0,5) : ''} hrs`;

        let barberoNombre = 'Barbero Asignado';
        let barberoId = null;
        if (cita.empleado) {
            barberoNombre = `${cita.empleado.nombre} ${cita.empleado.apellido}`;
            barberoId = cita.empleado.id;
        }

        let accionesHtml = '';
        if (cita.estado === 'COMPLETADA') {
            accionesHtml = `<button class="btn-calificar" onclick="abrirModalCalificacion(${cita.id}, ${cita.barberiaId}, ${barberoId})">Calificar Atención</button>`;
        }

        div.innerHTML = `
            <div class="cita-info">
                <h3>${fechaTexto}</h3>
                <p>Barbero: <span class="barbero">${barberoNombre}</span></p>
                <p style="font-size:0.8rem; color:var(--text-muted);">${cita.notas ? 'Detalle: ' + cita.notas : ''}</p>
            </div>
            <div class="cita-status">
                <span class="badge estado-${cita.estado}">${cita.estado}</span>
                ${accionesHtml}
            </div>
        `;
        contenedor.appendChild(div);
    });
}

function abrirModalCalificacion(citaId, barberiaId, empleadoId) {
    citaAcalificar = { citaId, barberiaId, empleadoId };
    document.querySelectorAll('input[type="radio"]').forEach(r => r.checked = false);
    document.getElementById('calComentario').value = '';
    document.getElementById('calError').textContent = '';
    document.getElementById('modalCalificacion').classList.add('visible');
}

function cerrarModal() {
    document.getElementById('modalCalificacion').classList.remove('visible');
    citaAcalificar = null;
}

async function enviarCalificacion() {
    const errorDiv = document.getElementById('calError');
    errorDiv.textContent = '';

    const estBarberia = document.querySelector('input[name="starBarberia"]:checked');
    const estBarbero = document.querySelector('input[name="starBarbero"]:checked');

    if (!estBarberia || !estBarbero) {
        errorDiv.textContent = 'Por favor selecciona las estrellas para la barbería y el barbero.';
        return;
    }

    const payload = {
        citaId: citaAcalificar.citaId,
        barberiaId: citaAcalificar.barberiaId,
        empleadoId: citaAcalificar.empleadoId,
        estrellasBarberia: parseInt(estBarberia.value),
        estrellasBarbero: parseInt(estBarbero.value),
        comentario: document.getElementById('calComentario').value.trim()
    };

    try {
        await Api.post('/calificaciones', payload);
        alert("¡Gracias por tu calificación!");
        cerrarModal();
        await cargarMisCitas();
    } catch (err) {
        errorDiv.textContent = err.message || 'Error al enviar calificación.';
    }
}
