document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.estaAutenticado()) {
        window.location.href = Auth.getBasePath() + '/pages/login.html';
        return;
    }

    Navbar.init();
    await I18n.init();
    await cargarPerfil();
    document.getElementById('formEliminarCuenta')?.addEventListener('submit', eliminarCuenta);
});

/**
 * Elimina la cuenta del cliente tras confirmar contraseña y casilla.
 * Si el backend responde OK se cierra la sesión local y se vuelve al inicio.
 */
async function eliminarCuenta(ev) {
    ev.preventDefault();
    const pwd = document.getElementById('fPasswordEliminar').value;
    const ok = document.getElementById('fConfirmarEliminar').checked;
    if (!pwd || !ok) {
        mostrarMsg('Ingresa tu contraseña y marca la confirmación.', 'err');
        return;
    }
    if (!window.confirm('¿Seguro que quieres eliminar tu cuenta? Esta acción es permanente.')) return;

    const btn = document.getElementById('btnEliminarCuenta');
    btn.disabled = true;
    btn.textContent = 'Eliminando...';
    try {
        await Api.perfil.eliminar(pwd);
        Api.clearToken();
        window.location.href = Auth.getBasePath() + '/index.html';
    } catch (err) {
        // 401 aquí = contraseña incorrecta; Api.request ya redirige solo si el token expiró
        mostrarMsg(err.message || 'No se pudo eliminar la cuenta.', 'err');
        btn.disabled = false;
        btn.textContent = 'Eliminar cuenta';
    }
}

async function cargarPerfil() {
    try {
        const perfil = await Api.get('/perfil');
        poblarFormulario(perfil);
    } catch (err) {
        mostrarMsg('No se pudo cargar tu perfil.', 'err');
    }
}

function poblarFormulario(p) {
    document.getElementById('perfilNombre').textContent = `${p.nombre || ''} ${p.apellido || ''}`.trim() || 'Usuario';
    document.getElementById('perfilEmail').textContent  = p.email || '';
    document.getElementById('perfilRol').textContent    = p.rol || 'CLIENTE';
    document.getElementById('zonaPeligro').hidden = p.rol !== 'CLIENTE';

    if (p.puntosFidelidad !== undefined) {
        const ptEl = document.getElementById('perfilPuntos');
        ptEl.textContent = `${p.puntosFidelidad} pts fidelidad`;
        ptEl.style.display = 'inline-flex';
    }

    document.getElementById('fNombre').value   = p.nombre || '';
    document.getElementById('fApellido').value = p.apellido || '';
    document.getElementById('fEmail').value    = p.email || '';
    document.getElementById('fTelefono').value = p.telefono || '';
    document.getElementById('fFotoUrl').value  = p.fotoUrl || '';
    document.getElementById('fIdioma').value   = p.idiomaPreferido || 'es';

    // Avatar
    const inicial = (p.nombre || 'U')[0].toUpperCase();
    const avatarCont = document.getElementById('avatarContainer');
    const fotoSegura = Utils.urlSegura(p.fotoUrl);
    if (fotoSegura) {
        avatarCont.innerHTML = `<img src="${Utils.esc(fotoSegura)}" class="perfil-avatar" alt="foto">`;
        avatarCont.querySelector('img').addEventListener('error', (e) => {
            e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80';
        }, { once: true });
    } else {
        avatarCont.innerHTML = `<div class="perfil-avatar-inicial">${Utils.esc(inicial)}</div>`;
    }

    // Preview live
    const fotoInput = document.getElementById('fFotoUrl');
    const preview = document.getElementById('fotoPreview');
    fotoInput.addEventListener('input', () => {
        const url = Utils.urlSegura(fotoInput.value.trim());
        if (url) { preview.src = url; preview.style.display = 'block'; }
        else { preview.style.display = 'none'; }
    });
    if (fotoSegura) { preview.src = fotoSegura; preview.style.display = 'block'; }
}

async function guardarPerfil() {
    const btn = document.getElementById('btnGuardar');
    btn.disabled = true; btn.textContent = 'Guardando...';

    const datos = {
        nombre:          document.getElementById('fNombre').value.trim(),
        apellido:        document.getElementById('fApellido').value.trim(),
        telefono:        document.getElementById('fTelefono').value.trim(),
        fotoUrl:         document.getElementById('fFotoUrl').value.trim(),
        idiomaPreferido: document.getElementById('fIdioma').value
    };

    try {
        const perfilActualizado = await Api.put('/perfil', datos);

        const usuarioStorage = JSON.parse(localStorage.getItem('usuario') || '{}');
        usuarioStorage.nombre   = perfilActualizado.nombre;
        usuarioStorage.apellido = perfilActualizado.apellido;
        usuarioStorage.fotoUrl  = perfilActualizado.fotoUrl;
        usuarioStorage.idiomaPreferido = perfilActualizado.idiomaPreferido;
        localStorage.setItem('usuario', JSON.stringify(usuarioStorage));
        localStorage.setItem('idioma', perfilActualizado.idiomaPreferido);

        poblarFormulario(perfilActualizado);
        mostrarMsg('¡Perfil actualizado con éxito!', 'ok');
        Navbar.init();
    } catch (err) {
        mostrarMsg(err.message, 'err');
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> Guardar Cambios`;
    }
}

function mostrarMsg(texto, tipo) {
    const el = document.getElementById('msgBox');
    el.textContent = texto;
    el.className = `msg-alert ${tipo}`;
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 4000);
}
