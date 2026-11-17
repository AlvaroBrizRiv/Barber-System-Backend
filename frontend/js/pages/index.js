/**
 * index.js — Catálogo público de barberías con buscador.
 * Todo dato del servidor se escapa con Utils.esc / Utils.urlSegura (anti-XSS).
 */
const IMG_DEFECTO = 'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=800&q=80';
const IMG_ERROR   = 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=800&q=80';

const ICONO_UBICACION = '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>';
const ICONO_TELEFONO  = '<svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>';

let ultimaBusqueda = 0; // descarta respuestas de búsquedas antiguas que lleguen tarde

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
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

        if (estado) {
            estado.textContent = termino
                ? `${barberias.length} resultado(s) para "${termino}"`
                : '';
        }
        if (!barberias || barberias.length === 0) {
            grid.innerHTML = `<div class="column has-text-centered has-text-grey">${
                termino ? 'No encontramos barberías con ese nombre o dirección.' : 'No hay barberías disponibles en este momento.'
            }</div>`;
            return;
        }
        grid.innerHTML = barberias.map(tarjetaBarberia).join('');
        grid.querySelectorAll('img[data-fallback]').forEach(img =>
            img.addEventListener('error', () => { img.src = IMG_ERROR; }, { once: true }));
    } catch (err) {
        if (id !== ultimaBusqueda) return;
        grid.innerHTML = `<div class="column has-text-centered has-text-grey">${Utils.esc(err.message)}</div>`;
    }
}

function tarjetaBarberia(b) {
    const desc = b.descripcion
        ? (b.descripcion.length > 110 ? b.descripcion.substring(0, 110) + '…' : b.descripcion)
        : 'Especialistas en corte de cabello, barba y cuidado integral masculino.';
    return `
        <div class="column is-12-mobile is-6-tablet is-4-desktop">
            <div class="barberia-card">
                <div class="barberia-img-wrap">
                    <img src="${Utils.esc(Utils.urlSegura(b.logoUrl) || IMG_DEFECTO)}" alt="${Utils.esc(b.nombre)}" loading="lazy" data-fallback>
                </div>
                <div class="barberia-body">
                    <div>
                        <h3 class="font-classic has-text-white is-size-4 mb-2">${Utils.esc(b.nombre)}</h3>
                        <p class="has-text-grey-light is-size-7 mb-2" style="display:flex; align-items:center; gap:0.4rem;">
                            ${ICONO_UBICACION} ${Utils.esc(b.direccion || 'Ubicación central')}
                        </p>
                        <p class="has-text-grey-light is-size-7 mb-4" style="display:flex; align-items:center; gap:0.4rem;">
                            ${ICONO_TELEFONO} ${Utils.esc(b.telefono || 'Atención continua')}
                        </p>
                        <p class="has-text-grey is-size-7 mb-4" style="line-height:1.5;">${Utils.esc(desc)}</p>
                    </div>
                    <div class="mt-4">
                        <a href="pages/barberia.html?id=${Number(b.id)}" class="btn-gold" style="width: 100%;">
                            Ingresar a Barbería
                            <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                        </a>
                    </div>
                </div>
            </div>
        </div>`;
}
