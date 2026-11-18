/*
 * Ledger · lógica compartida del login y el registro.
 * - Sesión en sessionStorage (se borra al cerrar la pestaña, como en la banca en línea).
 * - Validación en el cliente idéntica a la del servidor (DatosRegistro.java).
 * - Roseta guilloché dibujada en <canvas> una sola vez al cargar.
 */
(function () {
  "use strict";

  const API = "http://localhost:8080";
  const CLAVE_SESION = "ledger.sesion";
  const CLAVE_AVISO = "ledger.aviso";
  const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // ---------------- Sesión ----------------
  const Sesion = {
    leer() {
      try {
        const s = JSON.parse(sessionStorage.getItem(CLAVE_SESION) || "null");
        return s && new Date(s.expires_at) > new Date() ? s : null;
      } catch (e) { return null; }
    },
    guardar(s) {
      try { sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s)); } catch (e) { /* sin almacenamiento */ }
    },
    tomarAviso() {
      try {
        const aviso = sessionStorage.getItem(CLAVE_AVISO);
        sessionStorage.removeItem(CLAVE_AVISO);
        return aviso ? JSON.parse(aviso) : null;
      } catch (e) { return null; }
    }
  };

  // Si ya hay una sesión vigente, no tiene sentido volver a ingresar
  if (Sesion.leer()) {
    location.replace("dashboard.html");
    return;
  }

  const $ = (sel, raiz = document) => raiz.querySelector(sel);

  // ---------------- Roseta guilloché ----------------
  function guilloche(canvas) {
    const ctx = canvas.getContext("2d");
    const estilos = getComputedStyle(document.documentElement);
    const acento = estilos.getPropertyValue("--accent").trim() || "#1e7a5c";

    // Familias de epitrocoides, como en la impresión de seguridad de un billete
    const curvas = [
      { R: 1.00, r: 0.115, d: 0.36, alfa: 0.55, color: acento },
      { R: 0.92, r: 0.095, d: 0.30, alfa: 0.35, color: "#f2f5f2" },
      { R: 0.70, r: 0.075, d: 0.25, alfa: 0.45, color: acento },
      { R: 0.52, r: 0.130, d: 0.20, alfa: 0.25, color: "#f2f5f2" }
    ];

    function puntos(c, escala) {
      const pasos = 2600;
      const vueltas = Math.PI * 2 * Math.round(1 / c.r);
      const k = (c.R + c.r) / c.r;
      const lista = new Array(pasos + 1);
      for (let i = 0; i <= pasos; i++) {
        const t = (i / pasos) * vueltas;
        lista[i] = [
          ((c.R + c.r) * Math.cos(t) - c.d * Math.cos(k * t)) * escala,
          ((c.R + c.r) * Math.sin(t) - c.d * Math.sin(k * t)) * escala
        ];
      }
      return lista;
    }

    let trazos = [];
    let centro = [0, 0];

    function preparar() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const { width, height } = canvas.getBoundingClientRect();
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      // La roseta "se sale" por la esquina superior derecha, como en un billete,
      // y deja libre la mitad inferior donde va el texto
      const angosto = window.matchMedia("(max-width: 960px)").matches; // mismo corte que el CSS
      const escala = angosto ? width * 0.42 : width * 0.38;
      centro = angosto ? [width * 0.86, height * 0.38] : [width * 0.98, height * 0.06];
      trazos = curvas.map(c => ({ c, p: puntos(c, escala) }));
    }

    function dibujar(progreso) {
      const { width, height } = canvas.getBoundingClientRect();
      ctx.clearRect(0, 0, width, height);
      ctx.lineWidth = 0.7;
      trazos.forEach(({ c, p }) => {
        const hasta = Math.max(2, Math.floor(p.length * progreso));
        ctx.globalAlpha = c.alfa;
        ctx.strokeStyle = c.color;
        ctx.beginPath();
        ctx.moveTo(centro[0] + p[0][0], centro[1] + p[0][1]);
        for (let i = 1; i < hasta; i++) ctx.lineTo(centro[0] + p[i][0], centro[1] + p[i][1]);
        ctx.stroke();
      });
      ctx.globalAlpha = 1;
    }

    preparar();
    if (reducirMovimiento) {
      dibujar(1);
    } else {
      // Un único momento orquestado: la roseta se "imprime" en 1,4 s con ease-out fuerte
      const duracion = 1400;
      const inicio = performance.now();
      const easeOut = t => 1 - Math.pow(1 - t, 4);
      (function cuadro(ahora) {
        const t = Math.min(1, (ahora - inicio) / duracion);
        dibujar(easeOut(t));
        if (t < 1) requestAnimationFrame(cuadro);
      })(inicio);
    }

    let pendiente;
    window.addEventListener("resize", () => {
      cancelAnimationFrame(pendiente);
      pendiente = requestAnimationFrame(() => { preparar(); dibujar(1); });
    });
  }

  // ---------------- Utilidades de formulario ----------------
  function mostrarErrorCampo(input, mensaje) {
    const error = document.getElementById(input.id + "-error");
    input.setAttribute("aria-invalid", mensaje ? "true" : "false");
    if (!error) return;
    error.textContent = mensaje || "";
    void error.offsetHeight; // fija el estado inicial para que la transición se vea
    error.classList.toggle("visible", Boolean(mensaje));
  }

  function mostrarAlerta(texto, tipo) {
    const alerta = $("#alerta");
    alerta.className = "alerta" + (tipo ? " " + tipo : "");
    alerta.textContent = texto;
    alerta.hidden = false;
    // Forzar un reflow aplica el estado inicial antes de la transición. A diferencia de
    // requestAnimationFrame, funciona también si la pestaña está en segundo plano.
    void alerta.offsetHeight;
    alerta.classList.add("visible");
  }

  function ocultarAlerta() {
    const alerta = $("#alerta");
    alerta.classList.remove("visible");
    alerta.hidden = true;
  }

  function ocupado(boton, estado) {
    boton.disabled = estado;
    boton.classList.toggle("ocupado", estado);
    boton.setAttribute("aria-busy", String(estado));
  }

  function horaLocal(iso) {
    return new Date(iso).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" });
  }

  async function llamar(ruta, cuerpo) {
    let respuesta;
    try {
      respuesta = await fetch(API + ruta, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cuerpo)
      });
    } catch (e) {
      throw { mensaje: "No pudimos conectar con el banco. Verifica que transaction_api esté en marcha en localhost:8080." };
    }
    const json = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) throw { estado: respuesta.status, ...json, mensaje: json.message };
    return json;
  }

  function entrar(sesion, boton) {
    Sesion.guardar(sesion);
    boton.classList.add("listo");
    // Pequeña pausa para que se lea la confirmación del botón antes de cambiar de página
    setTimeout(() => location.assign("dashboard.html"), reducirMovimiento ? 0 : 450);
  }

  // Mostrar / ocultar contraseña y aviso de mayúsculas activadas
  document.querySelectorAll("[data-mostrar]").forEach(boton => {
    const input = document.getElementById(boton.dataset.mostrar);
    boton.addEventListener("click", () => {
      const visible = input.type === "text";
      input.type = visible ? "password" : "text";
      boton.textContent = visible ? "Mostrar" : "Ocultar";
      boton.setAttribute("aria-pressed", String(!visible));
      input.focus();
    });
  });
  document.querySelectorAll("input[type=password]").forEach(input => {
    const aviso = document.getElementById(input.id + "-mayus");
    if (!aviso) return;
    const revisar = e => {
      const activo = e.getModifierState && e.getModifierState("CapsLock");
      aviso.textContent = activo ? "Las mayúsculas están activadas." : "";
      aviso.classList.toggle("visible", Boolean(activo));
    };
    input.addEventListener("keydown", revisar);
    input.addEventListener("keyup", revisar);
    input.addEventListener("blur", () => aviso.classList.remove("visible"));
  });

  // ---------------- Login ----------------
  function iniciarLogin() {
    const form = $("#form-login");
    const boton = $("button[type=submit]", form);

    const aviso = Sesion.tomarAviso();
    if (aviso) mostrarAlerta(aviso.texto, aviso.tipo);

    form.addEventListener("submit", async e => {
      e.preventDefault();
      ocultarAlerta();
      const usuario = form.usuario;
      const clave = form.clave;
      mostrarErrorCampo(usuario, usuario.value.trim() ? "" : "Escribe tu usuario o tu correo.");
      mostrarErrorCampo(clave, clave.value ? "" : "Escribe tu contraseña.");
      const invalido = form.querySelector("[aria-invalid=true]");
      if (invalido) { invalido.focus(); return; }

      ocupado(boton, true);
      try {
        const sesion = await llamar("/auth/login", { username: usuario.value.trim(), password: clave.value });
        entrar(sesion, boton);
      } catch (err) {
        ocupado(boton, false);
        clave.value = "";
        if (err.estado === 423) {
          mostrarAlerta("Por seguridad bloqueamos el acceso tras 5 intentos fallidos. Podrás intentarlo de nuevo a las "
            + horaLocal(err.locked_until) + ".", "aviso");
        } else {
          mostrarAlerta(err.mensaje || "No pudimos iniciar tu sesión. Intenta de nuevo.");
        }
        clave.focus();
      }
    });
  }

  // ---------------- Registro ----------------
  const REGLAS = {
    nombre: v => v.trim().replace(/\s+/g, " ").length >= 3 ? "" : "Escribe tu nombre completo.",
    documento: v => /^\d{6,12}$/.test(v.replace(/[.\s-]/g, "")) ? "" : "Usa entre 6 y 12 dígitos, sin letras.",
    email: v => /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/.test(v.trim()) ? "" : "Revisa el correo; parece incompleto.",
    usuario: v => /^[A-Za-z0-9_]{3,20}$/.test(v.trim()) ? "" : "De 3 a 20 caracteres: letras, números o guion bajo.",
    clave: v => (v.length >= 8 && /\p{L}/u.test(v) && /\d/.test(v)) ? "" : "Debe cumplir los tres requisitos de abajo.",
    confirmacion: (v, form) => !v ? "Vuelve a escribir tu contraseña." : v === form.clave.value ? "" : "Las contraseñas no coinciden.",
    terminos: (v, form) => form.terminos.checked ? "" : "Debes aceptar para abrir la cuenta."
  };

  function nivelClave(v) {
    const requisitos = [v.length >= 8, /\p{L}/u.test(v), /\d/.test(v)];
    let puntos = requisitos.filter(Boolean).length;
    if (puntos === 3 && (v.length >= 12 || /[^\p{L}\d]/u.test(v))) puntos = 4;
    return { requisitos, puntos };
  }

  const NIVELES = [
    { texto: "", color: "var(--line)" },
    { texto: "Débil", color: "var(--danger)" },
    { texto: "Aceptable", color: "var(--warn)" },
    { texto: "Buena", color: "var(--accent)" },
    { texto: "Fuerte", color: "var(--accent)" }
  ];

  function iniciarRegistro() {
    const form = $("#form-registro");
    const boton = $("button[type=submit]", form);
    const segmentos = form.querySelectorAll(".fortaleza span");
    const etiquetaNivel = $("#nivel-clave");
    const requisitos = form.querySelectorAll(".requisitos li");
    const tocados = new Set();

    function validar(nombre) {
      const input = form[nombre];
      const mensaje = REGLAS[nombre](input.type === "checkbox" ? "" : input.value, form);
      mostrarErrorCampo(input, mensaje);
      return !mensaje;
    }

    // Se valida al salir del campo (no mientras se escribe), y en vivo solo si ya tenía error
    Object.keys(REGLAS).forEach(nombre => {
      const input = form[nombre];
      const evento = input.type === "checkbox" ? "change" : "blur";
      input.addEventListener(evento, () => { tocados.add(nombre); validar(nombre); });
      input.addEventListener("input", () => {
        if (input.getAttribute("aria-invalid") === "true") validar(nombre);
      });
    });

    form.clave.addEventListener("input", () => {
      const { requisitos: ok, puntos } = nivelClave(form.clave.value);
      const nivel = NIVELES[form.clave.value ? Math.max(1, puntos) : 0];
      segmentos.forEach((s, i) => {
        s.style.setProperty("--nivel-color", nivel.color);
        s.classList.toggle("lleno", form.clave.value && i < Math.max(1, puntos));
      });
      etiquetaNivel.textContent = nivel.texto;
      requisitos.forEach((li, i) => li.classList.toggle("ok", ok[i]));
      if (tocados.has("confirmacion")) validar("confirmacion");
    });

    form.addEventListener("submit", async e => {
      e.preventDefault();
      ocultarAlerta();
      const validos = Object.keys(REGLAS).map(validar);
      if (validos.includes(false)) {
        form.querySelector("[aria-invalid=true]").focus();
        return;
      }

      ocupado(boton, true);
      try {
        const sesion = await llamar("/auth/register", {
          full_name: form.nombre.value,
          document: form.documento.value,
          email: form.email.value,
          username: form.usuario.value.trim(),
          password: form.clave.value
        });
        $(".texto", boton).textContent = "Cuenta abierta";
        entrar(sesion, boton);
      } catch (err) {
        ocupado(boton, false);
        const campo = { username: "usuario", email: "email", document: "documento" }[err.field];
        if (campo) {
          mostrarErrorCampo(form[campo], err.mensaje);
          form[campo].focus();
        } else {
          mostrarAlerta(err.mensaje || "No pudimos abrir tu cuenta. Intenta de nuevo.");
        }
      }
    });
  }

  // ---------------- Arranque ----------------
  guilloche($(".marca canvas"));
  ({ login: iniciarLogin, registro: iniciarRegistro })[document.body.dataset.pagina]();
})();
