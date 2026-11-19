if (Auth.estaAutenticado()) Auth.redirigirTrasLogin();

// Indicador de fuerza de contraseña
const pwdInput = document.getElementById('password');
const bar      = document.getElementById('strengthBar');
const txt      = document.getElementById('strengthText');

pwdInput.addEventListener('input', () => {
    const v = pwdInput.value;
    let score = 0;
    if (v.length >= 8) score++;
    if (/[A-Z]/.test(v)) score++;
    if (/[0-9]/.test(v)) score++;
    if (/[^A-Za-z0-9]/.test(v)) score++;
    const colors = ['', '#E74C3C', '#E67E22', '#F1C40F', '#2ECC71'];
    const labels = ['', 'Débil', 'Regular', 'Buena', '¡Fuerte!'];
    bar.style.width = `${score * 25}%`;
    bar.style.background = colors[score];
    txt.textContent = score > 0 ? labels[score] : 'Al menos 8 caracteres';
});

const alertaE = document.getElementById('alertaError');
const alertaX = document.getElementById('alertaExito');
const btn     = document.getElementById('btnRegistro');

function mostrarError(msg) {
    alertaE.textContent = msg; alertaE.classList.add('visible');
    alertaX.classList.remove('visible');
}

document.getElementById('formRegistro').addEventListener('submit', async (e) => {
    e.preventDefault();
    alertaE.classList.remove('visible');

    const nombre   = document.getElementById('nombre').value.trim();
    const apellido = document.getElementById('apellido').value.trim();
    const rut      = document.getElementById('rut').value.trim();
    const email    = document.getElementById('email').value.trim();
    const telefono = document.getElementById('telefono').value.trim();
    const password = document.getElementById('password').value;
    const confirmar= document.getElementById('confirmar').value;

    if (!nombre || !apellido || !rut || !email || !password) {
        return mostrarError('Completa todos los campos obligatorios.');
    }
    if (password !== confirmar) {
        return mostrarError('Las contraseñas no coinciden.');
    }
    if (password.length < 8) {
        return mostrarError('La contraseña debe tener al menos 8 caracteres.');
    }

    btn.disabled = true;
    btn.textContent = 'Creando cuenta...';

    // FIX Error 400: el backend espera 'documentoIdentidad' (no 'rut')
    // porque el sistema soporta múltiples tipos de documentos (RUT, CPF, DNI, etc.)
    const resultado = await Auth.registro({
        nombre,
        apellido,
        documentoIdentidad: rut,  // Mapeamos el campo 'rut' al nombre correcto del backend
        email,
        telefono,
        password
    });

    if (!resultado.exito) {
        btn.disabled = false;
        btn.textContent = 'Crear mi cuenta';
        return mostrarError(resultado.error || 'No pudimos crear la cuenta. Revisa los datos e inténtalo de nuevo.');
    }

    // Cuenta creada: se inicia sesión de inmediato y se vuelve a la reserva pendiente (si la hay)
    alertaX.textContent = Auth.vieneDeReserva()
        ? '¡Cuenta creada! Volviendo a tu reserva…'
        : '¡Cuenta creada! Entrando…';
    alertaX.classList.add('visible');
    const sesion = await Auth.login(email, password);
    if (sesion.exito) {
        Auth.redirigirTrasLogin();
    } else {
        window.location.href = 'login.html';
    }
});
