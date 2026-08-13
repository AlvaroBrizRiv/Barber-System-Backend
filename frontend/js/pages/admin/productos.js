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
    cargarProductos();
});

// ─── Lógica de Productos ──────────────────────────────────────
async function cargarProductos() {
    try {
        // api.js no tiene el método para productos definido directamente, usamos request custom
        const res = await Api.request(`/api/productos/barberia/${barberiaId}`);
        const tbody = document.querySelector('#tablaProductos tbody');
        tbody.innerHTML = '';

        if (res.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:#888;">No hay productos en inventario</td></tr>';
            return;
        }

        res.forEach(p => {
            const tr = document.createElement('tr');
            
            const btnDesactivar = p.activo 
                ? `<button class="btn-danger-outline" onclick="desactivarProducto(${p.id})">Desactivar</button>`
                : `<span style="color:var(--text-muted);font-size:0.8rem;">Desactivado</span>`;

            const precio = `$${p.precio.toLocaleString()}`;
            let stockBadge = '';
            if (p.stock > 10) stockBadge = `<span class="badge badge-success">${p.stock}</span>`;
            else if (p.stock > 0) stockBadge = `<span class="badge badge-warning">${p.stock}</span>`;
            else stockBadge = `<span class="badge badge-danger">Agotado</span>`;

            const estadoBadge = p.activo 
                ? `<span class="badge badge-success">Activo</span>` 
                : `<span class="badge badge-danger">Inactivo</span>`;
            
            const imgUrl = p.imagenUrl || 'https://via.placeholder.com/40x40/222222/D4AF37?text=P';

            tr.innerHTML = `
                <td><img src="${imgUrl}" alt="${p.nombre}" class="img-producto"></td>
                <td><strong>${p.nombre}</strong><br><span style="font-size:0.7rem;color:var(--text-muted)">${p.descripcion || ''}</span></td>
                <td><span class="badge badge-info">${p.categoria}</span></td>
                <td>${precio}</td>
                <td>${stockBadge}</td>
                <td>${estadoBadge}</td>
                <td>${btnDesactivar}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error al cargar productos:", err);
    }
}

async function desactivarProducto(id) {
    if (!confirm('¿Seguro que deseas desactivar este producto del catálogo?')) return;
    try {
        await Api.request(`/api/productos/${id}`, { method: 'DELETE' });
        cargarProductos();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ─── Modal y Formulario ───────────────────────────────────────
function abrirModal() {
    document.getElementById('prodError').textContent = '';
    document.getElementById('modalProducto').classList.add('visible');
}

function cerrarModal() {
    document.getElementById('modalProducto').classList.remove('visible');
}

async function guardarProducto() {
    const errorDiv = document.getElementById('prodError');
    errorDiv.textContent = '';

    const datos = {
        nombre: document.getElementById('prodNombre').value.trim(),
        descripcion: document.getElementById('prodDescripcion').value.trim(),
        precio: parseFloat(document.getElementById('prodPrecio').value),
        stock: parseInt(document.getElementById('prodStock').value) || 0,
        categoria: document.getElementById('prodCategoria').value,
        imagenUrl: document.getElementById('prodImagen').value.trim()
    };

    if (!datos.nombre || isNaN(datos.precio) || datos.precio <= 0) {
        return errorDiv.textContent = 'El nombre y el precio (mayor a 0) son obligatorios.';
    }

    try {
        await Api.request('/api/productos', {
            method: 'POST',
            body: JSON.stringify(datos)
        });
        cerrarModal();
        cargarProductos();
        
        // Limpiar form
        document.getElementById('prodNombre').value = '';
        document.getElementById('prodDescripcion').value = '';
        document.getElementById('prodPrecio').value = '';
        document.getElementById('prodStock').value = '0';
        document.getElementById('prodImagen').value = '';
    } catch (err) {
        errorDiv.textContent = err.message;
    }
}
