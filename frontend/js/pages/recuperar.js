/**
 * recuperar.js — "¿Olvidaste tu contraseña?" en tres pasos:
 *   1. Correo → el backend envía un código de 6 dígitos.
 *   2. Código → el backend devuelve un resetToken de corta duración.
 *   3. Nueva contraseña → se guarda y se vuelve al login.
 *
 * El resetToken solo vive en memoria (no se guarda en localStorage).
 */
const SEGUNDOS_REENVIO = 60;

const estado = { email: '', resetToken: null, temporizador: null };

const $ = (id) => document.getElementById(id);
const alerta = $('alerta');

function mostrar(msg, tipo = 'error') {
    alerta.textContent = msg;
    alerta.className = `alert ${tipo} visible`;
}
function ocultar() { alerta.className = 'alert'; }

function irPaso(n) {
    ['formEmail', 'formCodigo', 'formPassword'].forEach((id, i) => { $(id).hidden = i !== n - 1; });
    [1, 2, 3].forEach(i => {
        const el = $(`indPaso${i}`);
        el.classList.toggle('activo', i === n);
        el.classList.toggle('hecho', i < n);
    });
    const foco = { 1: 'email', 2: 'codigo', 3: 'password' }[n];
    setTimeout(() => $(foco)?.focus(), 0);
}

async function conCarga(boton, texto, accion) {
    const original = boton.textContent;
    boton.disabled = true;
    boton.innerHTML = `<span class="spinner"></span>${Utils.esc(texto)}`;
    try {
        await accion();
    } finally {
        boton.disabled = false;
        boton.textContent = original;
    }
}

function iniciarCuentaRegresiva() {
    const btn = $('btnReenviar');
    let restante = SEGUNDOS_REENVIO;
    clearInterval(estado.temporizador);
    btn.disabled = true;
    btn.textContent = `Reenviar código (${restante}s)`;
    estado.temporizador = setInterval(() => {
        restante -= 1;
        if (restante <= 0) {
            clearInterval(estado.temporizador);
            btn.disabled = false;
            btn.textContent = 'Reenviar código';
        } else {
            btn.textContent = `Reenviar código (${restante}s)`;
        }
    }, 1000);
}

async function enviarCodigo() {
    const r = await Api.auth.recuperar.solicitar(estado.email);
    $('textoCodigo').textContent = '';
    $('textoCodigo').append(
        'Si ', Object.assign(document.createElement('strong'), { textContent: estado.email }),
        ' está registrado, te enviamos un código de 6 dígitos. Vence en 15 minutos; revisa también la carpeta de spam.');
    iniciarCuentaRegresiva();
    return r;
}

// Paso 1
$('formEmail').addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultar();
    const email = $('email').value.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        mostrar('Ingresa un correo electrónico válido.');
        return;
    }
    estado.email = email;
    await conCarga($('btnEnviar'), 'Enviando...', async () => {
        try {
            await enviarCodigo();
            $('codigo').value = '';
            irPaso(2);
        } catch (err) {
            mostrar(err.message);
        }
    });
});

// Paso 2
$('codigo').addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
});

$('formCodigo').addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultar();
    const codigo = $('codigo').value.trim();
    if (!/^\d{6}$/.test(codigo)) {
        mostrar('Ingresa el código de 6 dígitos.');
        return;
    }
    await conCarga($('btnVerificar'), 'Verificando...', async () => {
        try {
            const r = await Api.auth.recuperar.verificar(estado.email, codigo);
            estado.resetToken = r.resetToken;
            irPaso(3);
        } catch (err) {
            mostrar(err.message);
            $('codigo').select();
        }
    });
});

$('btnReenviar').addEventListener('click', async () => {
    ocultar();
    try {
        await enviarCodigo();
        mostrar('Te enviamos un nuevo código. El anterior ya no es válido.', 'success');
    } catch (err) {
        mostrar(err.message);
    }
});

$('btnCambiarEmail').addEventListener('click', () => {
    ocultar();
    clearInterval(estado.temporizador);
    irPaso(1);
});

// Paso 3
$('password').addEventListener('input', (e) => {
    const v = e.target.value;
    let puntos = 0;
    if (v.length >= 8) puntos++;
    if (/[A-Z]/.test(v) && /[a-z]/.test(v)) puntos++;
    if (/\d/.test(v)) puntos++;
    if (/[^A-Za-z0-9]/.test(v)) puntos++;
    const barra = $('barraFuerza');
    barra.style.width = `${puntos * 25}%`;
    barra.style.background = ['#E74C3C', '#E74C3C', '#F39C12', '#D4AF37', '#2ECC71'][puntos];
});

$('formPassword').addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultar();
    const p1 = $('password').value;
    const p2 = $('password2').value;
    if (p1.length < 8 || p1.length > 72) {
        mostrar('La contraseña debe tener entre 8 y 72 caracteres.');
        return;
    }
    if (p1 !== p2) {
        mostrar('Las contraseñas no coinciden.');
        return;
    }
    await conCarga($('btnGuardar'), 'Guardando...', async () => {
        try {
            await Api.auth.recuperar.restablecer(estado.resetToken, p1);
            estado.resetToken = null;
            window.location.href = 'login.html?restablecida=1';
        } catch (err) {
            mostrar(err.message);
        }
    });
});

// Inicio: precarga el correo si viene desde el login
(() => {
    const email = new URLSearchParams(location.search).get('email');
    if (email) $('email').value = email.slice(0, 150);
    irPaso(1);
})();
