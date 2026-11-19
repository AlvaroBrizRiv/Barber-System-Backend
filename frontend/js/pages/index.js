/**
 * index.js — Directorio público de barberías con buscador.
 * Todo dato del servidor se escapa con Utils.esc / Utils.urlSegura (anti-XSS).
 * Cada tarjeta lleva directo a reservar en esa barbería, o a ver su página.
 */
const ICONO_UBICACION = '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>';
const ICONO_TELEFONO  = '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>';
const ICONO_TIJERAS   = '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><line x1="20" y1="4" x2="8.12" y2="15.88"/><line x1="14.47" y1="14.48" x2="20" y2="20"/><line x1="8.12" y1="8.12" x2="12" y2="12"/></svg>';

let ultimaBusqueda = 0; // descarta respuestas de búsquedas antiguas que lleguen tarde

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    if (typeof I18n !== 'undefined') await I18n.init();
    const input = document.getElementById('buscarBarberia');
    if (input) {
        input.addEventListener('input', Utils.debounce(() => cargarBarberias(input.value), 300));
    }
    await cargarBarberias('');
});

async function cargarBarberias(texto) {
    const grid = document.getElementById('barberias-grid');
    const estado = document.getElementById('buscadorEstado');
    const termino = (texto || '').trim();
    const id = ++ultimaBusqueda;
    try {
        const barberias = termino ? await Api.barberias.buscar(termino) : await Api.barberias.listar();
        if (id !== ultimaBusqueda) return;

        const n = barberias?.length || 0;
        estado.textContent = termino
            ? `${n} ${n === 1 ? 'resultado' : 'resultados'} para “${termino}”`
            : (n ? `${n} ${n === 1 ? 'barbería' : 'barberías'} para reservar` : '');

        if (!n) {
            Utils.mostrarAviso(grid, termino
                ? 'No encontramos barberías con ese nombre o comuna. Prueba con otra palabra.'
                : 'Todavía no hay barberías para reservar.');
            return;
        }
        grid.innerHTML = barberias.map(tarjetaBarberia).join('');
        grid.querySelectorAll('img[data-fallback]').forEach(img =>
            img.addEventListener('error', () => img.closest('.barberia-foto')?.classList.add('sin-foto'), { once: true }));
    } catch (err) {
        if (id !== ultimaBusqueda) return;
        estado.textContent = '';
        const msg = err.sinConexion
            ? 'No pudimos cargar las barberías. Revisa tu conexión e inténtalo de nuevo.'
            : (err.message || 'No pudimos cargar las barberías.');
        Utils.mostrarError(grid, msg, () => cargarBarberias(document.getElementById('buscarBarberia')?.value || ''));
    }
}

function tarjetaBarberia(b) {
    const id = Number(b.id);
    const foto = Utils.urlSegura(b.logoUrl);
    const desc = b.descripcion
        ? (b.descripcion.length > 120 ? b.descripcion.substring(0, 120).trimEnd() + '…' : b.descripcion)
        : '';
    const tel = (b.telefono || '').replace(/[^\d+]/g, '');
    return `
        <article class="barberia-card">
            <a class="barberia-foto ${foto ? '' : 'sin-foto'}" href="pages/barberia.html?id=${id}" tabindex="-1" aria-hidden="true">
                ${foto ? `<img src="${Utils.esc(foto)}" alt="" loading="lazy" data-fallback>` : ''}
                <span class="barberia-foto-icono">${ICONO_TIJERAS}</span>
            </a>
            <div class="barberia-body">
                <h3><a href="pages/barberia.html?id=${id}">${Utils.esc(b.nombre)}</a></h3>
                ${b.direccion ? `<p class="barberia-dato">${ICONO_UBICACION}<span>${Utils.esc(b.direccion)}</span></p>` : ''}
                ${tel ? `<p class="barberia-dato">${ICONO_TELEFONO}<a href="tel:${Utils.esc(tel)}">${Utils.esc(b.telefono)}</a></p>` : ''}
                ${desc ? `<p class="barberia-desc">${Utils.esc(desc)}</p>` : ''}
                <div class="barberia-acciones">
                    <a href="reserva.html?barberiaId=${id}" class="btn-primary">Reservar hora</a>
                    <a href="pages/barberia.html?id=${id}" class="btn-outline">Ver barbería</a>
                </div>
            </div>
        </article>`;
}
