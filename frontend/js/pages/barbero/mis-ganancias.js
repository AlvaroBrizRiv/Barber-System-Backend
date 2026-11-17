/**
 * mis-ganancias.js — El barbero consulta SOLO sus ganancias (el backend toma su ID del JWT).
 */
Auth.requerirRol('BARBERO', 'ADMIN_BARBERIA');

const estado = { periodo: 'DIA' };

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();

    const fecha = document.getElementById('fechaRef');
    fecha.value = Utils.fechaISO();
    fecha.addEventListener('change', () => void cargar());

    document.querySelectorAll('.periodo-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            estado.periodo = tab.dataset.periodo;
            document.querySelectorAll('.periodo-tab').forEach(t => {
                t.classList.toggle('active', t === tab);
                t.setAttribute('aria-selected', String(t === tab));
            });
            void cargar();
        });
    });
    await cargar();
});

async function cargar() {
    const msg = document.getElementById('mensaje');
    msg.hidden = true;
    try {
        const r = await Api.ganancias.mias(estado.periodo, document.getElementById('fechaRef').value || Utils.fechaISO());
        const total = Number(r.total) || 0;
        const citas = Number(r.totalCitas) || 0;
        document.getElementById('kpiTotal').textContent = Utils.dinero(total);
        document.getElementById('kpiCitas').textContent = String(citas);
        document.getElementById('kpiPromedio').textContent = citas ? Utils.dinero(total / citas) : '—';
        document.getElementById('rango').textContent = 'Periodo: ' + textoRango(r.desde, r.hasta);
        renderBarras(r.serie || []);
    } catch (err) {
        msg.textContent = err.message;
        msg.hidden = false;
    }
}

function textoRango(desde, hasta) {
    const f = (iso) => new Date(iso + 'T00:00:00').toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' });
    return desde === hasta ? f(desde) : `${f(desde)} — ${f(hasta)}`;
}

/** Gráfico de barras liviano en HTML/CSS (sin librerías externas). */
function renderBarras(serie) {
    const cont = document.getElementById('barras');
    const panel = document.getElementById('panelSerie');
    panel.hidden = serie.length <= 1; // en vista diaria basta con los KPI
    if (panel.hidden) return;

    const max = Math.max(...serie.map(d => Number(d.total) || 0), 1);
    cont.innerHTML = serie.map(d => {
        const total = Number(d.total) || 0;
        const alto = Math.round((total / max) * 100);
        const fecha = new Date(d.fecha + 'T00:00:00');
        const etiqueta = serie.length > 7
            ? String(fecha.getDate())
            : fecha.toLocaleDateString('es-CL', { weekday: 'short' });
        const titulo = `${fecha.toLocaleDateString('es-CL')}: ${Utils.dinero(total)} (${Number(d.citas) || 0} citas)`;
        return `
            <div class="barra" title="${Utils.esc(titulo)}">
                <div class="barra-fill" style="height:${alto}%"></div>
                <span class="barra-label">${Utils.esc(etiqueta)}</span>
            </div>`;
    }).join('');
}
