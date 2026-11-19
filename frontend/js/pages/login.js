// Si ya está autenticado, redirigir (al destino pendiente si lo hay)
if (Auth.estaAutenticado()) {
    Auth.redirigirTrasLogin();
}

const form     = document.getElementById('formLogin');
const btnLogin = document.getElementById('btnLogin');
const alerta   = document.getElementById('alertaError');

function mostrarError(msg) {
    alerta.textContent = msg;
    alerta.className = 'alert error visible';
    alerta.setAttribute('role', 'alert');
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
        btnLogin.innerHTML = 'Entrar';
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
        Auth.redirigirTrasLogin();
        return;
    }
    const codigo = resultado.data?.codigo;
    if (codigo === 'SUSPENDIDA') {
        iniciarEspera(Number(resultado.data.segundosRestantes) || 15);
    } else if (codigo === 'BLOQUEADA') {
        mostrarBloqueo(email);
    } else {
        mostrarError(resultado.error || 'Credenciales incorrectas.');
    }
});

// ─── Esperas y bloqueo por intentos fallidos (los aplica el backend por cuenta) ───
let temporizadorEspera = null;

/** Deshabilita el botón y muestra una cuenta regresiva hasta poder reintentar. */
function iniciarEspera(segundos) {
    clearInterval(temporizadorEspera);
    let restante = Math.max(1, Math.ceil(segundos));
    const pintar = () => {
        mostrarError(`Demasiados intentos fallidos. Podrás intentarlo de nuevo en ${restante} s.`);
        btnLogin.disabled = true;
        btnLogin.textContent = `Espera ${restante} s`;
    };
    pintar();
    temporizadorEspera = setInterval(() => {
        restante -= 1;
        if (restante <= 0) {
            clearInterval(temporizadorEspera);
            ocultarError();
            setLoading(false);
            document.getElementById('password').focus();
        } else {
            pintar();
        }
    }, 1000);
}

/** Cuenta bloqueada: solo se puede salir restableciendo la contraseña. */
function mostrarBloqueo(email) {
    clearInterval(temporizadorEspera);
    alerta.textContent = 'Tu cuenta fue bloqueada por demasiados intentos fallidos. ';
    const enlace = document.createElement('a');
    enlace.href = `recuperar.html?email=${encodeURIComponent(email)}`;
    enlace.textContent = 'Restablece tu contraseña para desbloquearla.';
    enlace.style.color = 'inherit';
    enlace.style.fontWeight = '600';
    alerta.appendChild(enlace);
    alerta.className = 'alert error visible';
}

// Lleva el correo escrito a la pantalla de recuperación para no tener que repetirlo
document.getElementById('linkRecuperar')?.addEventListener('click', (e) => {
    const email = document.getElementById('email').value.trim();
    if (email) {
        e.preventDefault();
        window.location.href = `recuperar.html?email=${encodeURIComponent(email)}`;
    }
});

// Viene de una reserva a medio hacer: explicar por qué se pide la sesión
if (Auth.vieneDeReserva()) {
    alerta.textContent = 'Inicia sesión o crea tu cuenta para confirmar tu cita. Guardamos lo que elegiste.';
    alerta.className = 'alert info visible';
    alerta.setAttribute('role', 'status');
    const sub = document.getElementById('loginSub');
    if (sub) sub.textContent = 'Ya casi: falta confirmar tu cita';
}

// Aviso tras restablecer la contraseña
if (new URLSearchParams(location.search).get('restablecida') === '1') {
    alerta.textContent = 'Tu contraseña fue actualizada. Inicia sesión con la nueva contraseña.';
    alerta.className = 'alert success visible';
}
