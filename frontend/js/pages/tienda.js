const urlParams = new URLSearchParams(window.location.search);
let BARBERIA_ID = parseInt(urlParams.get('barberiaId')) || 1;
let todosLosProductos = [];
let carritoData = [];

document.addEventListener('DOMContentLoaded', async () => {
    Navbar.init();
    await I18n.init();
    await cargarBarberiaInfo();
    await cargarProductos();
    await refrescarCarrito();
});

async function cargarBarberiaInfo() {
    try {
        const b = await Api.get(`/barberias/${BARBERIA_ID}`);
        if (b && b.nombre) {
            document.getElementById('txtBarberiaNombre').textContent = b.nombre;
        }
    } catch (e) {
        console.warn("No se pudo cargar datos de barbería", e);
    }
}

async function cargarProductos() {
    try {
        const res = await Api.get(`/productos/barberia/${BARBERIA_ID}`);
        todosLosProductos = (res || []).filter(p => p.activo);
        renderizarProductos(todosLosProductos);
    } catch (err) {
        document.getElementById('gridProductos').innerHTML = `<div class="empty-cart-state">Error al cargar productos de la barbería.</div>`;
    }
}

function renderizarProductos(productos) {
    const grid = document.getElementById('gridProductos');
    grid.innerHTML = '';

    if (!productos || productos.length === 0) {
        grid.innerHTML = `<div class="empty-cart-state" style="grid-column: 1 / -1;">No hay productos disponibles en esta sección.</div>`;
        return;
    }

    productos.forEach(p => {
        const card = document.createElement('div');
        card.className = 'producto-card';

        const imgUrl = p.imagenUrl || 'https://images.unsplash.com/photo-1599305090598-fe179d501227?auto=format&fit=crop&q=80&w=400';
        const enStock = p.stock > 0;
        const botonHtml = enStock 
            ? `<button class="btn-comprar" onclick="agregarAlCarrito(${p.id})">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/></svg>
                Añadir
               </button>` 
            : `<span class="agotado-badge">Agotado</span>`;

        card.innerHTML = `
            <div class="producto-img-container">
                <img src="${imgUrl}" alt="${p.nombre}" class="producto-img" onerror="this.src='https://images.unsplash.com/photo-1599305090598-fe179d501227?auto=format&fit=crop&q=80&w=400'">
                <span class="producto-categoria-tag">${(p.categoria || '').replace('_', ' ')}</span>
            </div>
            <div class="producto-info">
                <h3 class="producto-titulo">${p.nombre}</h3>
                <p class="producto-desc">${p.descripcion || 'Insumo de barbería premium.'}</p>
                <div class="producto-footer">
                    <span class="producto-precio">$${Number(p.precio).toLocaleString('es-CL')}</span>
                    ${botonHtml}
                </div>
            </div>
        `;
        grid.appendChild(card);
    });
}

function filtrarCategoria(categoria, btn) {
    document.querySelectorAll('.btn-categoria').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');

    if (categoria === 'TODOS') {
        renderizarProductos(todosLosProductos);
    } else {
        const filtrados = todosLosProductos.filter(p => p.categoria === categoria);
        renderizarProductos(filtrados);
    }
}

// ─── LÓGICA DE CARRITO PERSISTENTE EN BASE DE DATOS ─────────

async function refrescarCarrito() {
    if (!Auth.estaAutenticado()) {
        document.getElementById('cartCountBadge').textContent = '0';
        return;
    }
    try {
        carritoData = await Api.get(`/carrito/${BARBERIA_ID}`);
        const totalItems = carritoData.reduce((acc, item) => acc + item.cantidad, 0);
        document.getElementById('cartCountBadge').textContent = totalItems;
        renderizarDrawerItems();
    } catch (err) {
        console.warn("Error al cargar carrito persistente", err);
    }
}

async function agregarAlCarrito(productoId) {
    if (!Auth.estaAutenticado()) {
        alert('Debes iniciar sesión con tu cuenta de cliente para agregar productos al carro.');
        window.location.href = 'login.html';
        return;
    }
    try {
        await Api.post(`/carrito/${BARBERIA_ID}`, {
            productoId: productoId,
            cantidad: 1
        });
        await refrescarCarrito();
        abrirCarrito();
    } catch (err) {
        alert(err.message || 'No se pudo agregar el producto.');
    }
}

async function cambiarCantidad(productoId, delta, cantidadActual) {
    const nuevaCantidad = cantidadActual + delta;
    try {
        if (nuevaCantidad <= 0) {
            await Api.request(`/carrito/${BARBERIA_ID}/item/${productoId}`, { method: 'DELETE' });
        } else {
            await Api.put(`/carrito/${BARBERIA_ID}/item/${productoId}`, {
                cantidad: nuevaCantidad
            });
        }
        await refrescarCarrito();
    } catch (err) {
        alert(err.message || 'Error al actualizar cantidad');
    }
}

async function eliminarDelCarrito(productoId) {
    try {
        await Api.request(`/carrito/${BARBERIA_ID}/item/${productoId}`, { method: 'DELETE' });
        await refrescarCarrito();
    } catch (err) {
        alert(err.message || 'Error al eliminar producto');
    }
}

function renderizarDrawerItems() {
    const list = document.getElementById('cartItemsList');
    const totalEl = document.getElementById('cartTotalVal');
    const btnCheckout = document.getElementById('btnCheckout');

    if (!carritoData || carritoData.length === 0) {
        list.innerHTML = `<div class="empty-cart-state">Tu carrito en esta barbería está vacío.</div>`;
        totalEl.textContent = '$0';
        btnCheckout.disabled = true;
        btnCheckout.style.opacity = '0.5';
        return;
    }

    btnCheckout.disabled = false;
    btnCheckout.style.opacity = '1';

    let granTotal = 0;
    list.innerHTML = carritoData.map(item => {
        const subtotal = item.precio * item.cantidad;
        granTotal += subtotal;
        const img = item.imagenUrl || 'https://images.unsplash.com/photo-1599305090598-fe179d501227?auto=format&fit=crop&q=80&w=200';

        return `
            <div class="cart-item-row">
                <img src="${img}" class="cart-item-thumb" alt="${item.nombre}">
                <div class="cart-item-details">
                    <div class="cart-item-name">${item.nombre}</div>
                    <div class="cart-item-price">$${Number(item.precio).toLocaleString('es-CL')}</div>
                    <div class="cart-item-qty">
                        <button class="btn-qty" onclick="cambiarCantidad(${item.productoId}, -1, ${item.cantidad})">-</button>
                        <span class="qty-val">${item.cantidad}</span>
                        <button class="btn-qty" onclick="cambiarCantidad(${item.productoId}, 1, ${item.cantidad})">+</button>
                    </div>
                </div>
                <button class="btn-delete-item" onclick="eliminarDelCarrito(${item.productoId})" title="Eliminar">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
            </div>
        `;
    }).join('');

    totalEl.textContent = `$${granTotal.toLocaleString('es-CL')}`;
}

function abrirCarrito() {
    if (!Auth.estaAutenticado()) {
        alert('Debes iniciar sesión para ver tu carrito.');
        window.location.href = 'login.html';
        return;
    }
    refrescarCarrito();
    document.getElementById('cartOverlay').classList.add('active');
}

function cerrarCarrito(e) {
    document.getElementById('cartOverlay').classList.remove('active');
}

async function finalizarCompra() {
    if (!Auth.estaAutenticado()) {
        window.location.href = 'login.html';
        return;
    }
    try {
        await Api.post(`/carrito/${BARBERIA_ID}/checkout`, {});
        cerrarCarrito();
        await refrescarCarrito();
        document.getElementById('modalCompraExitosa').classList.add('active');
    } catch (err) {
        alert("Error al procesar compra: " + err.message);
    }
}

function cerrarModalCompra() {
    document.getElementById('modalCompraExitosa').classList.remove('active');
}
