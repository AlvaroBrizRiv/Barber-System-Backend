/**
 * stock.js — Vista de inventario de solo lectura para el barbero.
 * No existen botones de edición: el backend además exige ADMIN para modificar productos.
 */
Auth.requerirRol('BARBERO', 'ADMIN_BARBERIA');

const STOCK_BAJO = 5;
let productos = [];

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();
    document.getElementById('filtroTexto').addEventListener('input', Utils.debounce(render, 150));
    await cargar();
});

async function cargar() {
    const tbody = document.getElementById('tablaStock');
    const barberiaId = Auth.getBarberiaId();
    if (!barberiaId) {
        tbody.innerHTML = '<tr><td colspan="4" class="vacio">Tu usuario no está asociado a una barbería.</td></tr>';
        return;
    }
    try {
        productos = await Api.productos.listar(barberiaId);
        render();
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="vacio">${Utils.esc(err.message)}</td></tr>`;
    }
}

function render() {
    const tbody = document.getElementById('tablaStock');
    const texto = document.getElementById('filtroTexto').value.trim().toLowerCase();
    const lista = productos
        .filter(p => !texto || (p.nombre || '').toLowerCase().includes(texto))
        .sort((a, b) => (Number(a.stock) || 0) - (Number(b.stock) || 0)); // primero lo que se está agotando

    const agotados = productos.filter(p => !(Number(p.stock) > 0)).length;
    document.getElementById('resumenStock').textContent =
        `${productos.length} productos · ${agotados} agotado(s)`;

    if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="vacio">No hay productos que coincidan.</td></tr>';
        return;
    }
    tbody.innerHTML = lista.map(p => {
        const stock = Number(p.stock) || 0;
        const clase = stock === 0 ? 'stock-agotado' : stock <= STOCK_BAJO ? 'stock-bajo' : 'stock-ok';
        const texto = stock === 0 ? 'Agotado' : `${stock} u.`;
        return `
            <tr>
                <td><strong>${Utils.esc(p.nombre)}</strong></td>
                <td>${Utils.esc((p.categoria || '').replaceAll('_', ' '))}</td>
                <td>${Utils.dinero(p.precio)}</td>
                <td><span class="stock-badge ${clase}">${Utils.esc(texto)}</span></td>
            </tr>`;
    }).join('');
}
