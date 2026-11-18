// Si ya está autenticado, redirigir
if (Auth.estaAutenticado()) {
    Auth.redirigirSegunRol();
}

const form     = document.getElementById('formLogin');
const btnLogin = document.getElementById('btnLogin');
const alerta   = document.getElementById('alertaError');

function mostrarError(msg) {
    alerta.textContent = msg;
    alerta.classList.add('visible');
}

function ocultarError() {
    alerta.classList.remove('visible');
}

function setLoading(loading) {
    if (loading) {
        btnLogin.disabled = true;
        btnLogin.innerHTML = '<span class="spinner"></span>Verificando...';
    } else {
        btnLogin.disabled = false;
        btnLogin.innerHTML = 'INICIAR SESIÓN';
    }
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarError();

    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email || !password) {
        mostrarError('Por favor completa todos los campos.');
        return;
    }

    setLoading(true);
    const resultado = await Auth.login(email, password);
    setLoading(false);

    if (resultado.exito) {
        Auth.redirigirSegunRol();
    } else {
        mostrarError(resultado.error || 'Credenciales incorrectas.');
    }
});

// Lleva el correo escrito a la pantalla de recuperación para no tener que repetirlo
document.getElementById('linkRecuperar')?.addEventListener('click', (e) => {
    const email = document.getElementById('email').value.trim();
    if (email) {
        e.preventDefault();
        window.location.href = `recuperar.html?email=${encodeURIComponent(email)}`;
    }
});

// Aviso tras restablecer la contraseña
if (new URLSearchParams(location.search).get('restablecida') === '1') {
    alerta.textContent = 'Tu contraseña fue actualizada. Inicia sesión con la nueva contraseña.';
    alerta.className = 'alert success visible';
}
