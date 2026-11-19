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

        const f = new Date(`${cita.fecha}T00:00:00`);
        const dia = isNaN(f) ? '--' : String(f.getDate()).padStart(2, '0');
        const mes = isNaN(f) ? '' : f.toLocaleDateString('es-CL', { month: 'short' }).replace('.', '');
        const diaSemana = isNaN(f) ? '' : f.toLocaleDateString('es-CL', { weekday: 'long', year: 'numeric' });
        const hora = cita.hora ? cita.hora.substring(0, 5) : '--:--';
        const servicios = (cita.servicios || []).map(s => s.nombre).join(', ');
        const total = Number(cita.precioTotal || 0).toLocaleString('es-CL');

        let barberoNombre = 'Barbero Asignado';
        let barberoId = null;
        if (cita.empleado) {
            barberoNombre = `${cita.empleado.nombre} ${cita.empleado.apellido}`;
            barberoId = cita.empleado.id;
        }

        let accionesHtml = '';
        if (cita.estado === 'COMPLETADA') {
            accionesHtml = `<button class="btn-accion calificar btn-abrir-cal" data-id="${Number(cita.id)}" data-barberia="${Number(cita.barberiaId)}" data-empleado="${Number(barberoId)}">★ Calificar</button>`;
        }

        div.innerHTML = `
            <div class="cita-fecha-box">
                <span class="cita-dia">${Utils.esc(dia)}</span>
                <span class="cita-mes">${Utils.esc(mes)}</span>
                <span class="cita-hora">${Utils.esc(hora)}</span>
            </div>
            <div class="cita-info">
                <div class="cita-barberia">${Utils.esc(diaSemana)}</div>
                <div class="cita-detalles">
                    <span>✂ ${Utils.esc(barberoNombre)}</span>
                    ${servicios ? `<span>• ${Utils.esc(servicios)}</span>` : ''}
                    <span class="cita-total">$${Utils.esc(total)}</span>
                </div>
                ${cita.notas ? `<p class="cita-notas">${Utils.esc(cita.notas)}</p>` : ''}
            </div>
            <div class="cita-acciones">
                <span class="estado-badge ${Utils.esc(String(cita.estado).toLowerCase())}">${Utils.esc(cita.estado)}</span>
                ${accionesHtml}
            </div>
        `;
        const btnCal = div.querySelector('.btn-abrir-cal');
        if (btnCal) {
            btnCal.addEventListener('click', () => abrirModalCalificacion(
                Number(btnCal.dataset.id),
                Number(btnCal.dataset.barberia),
                Number(btnCal.dataset.empleado)
            ));
        }
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
