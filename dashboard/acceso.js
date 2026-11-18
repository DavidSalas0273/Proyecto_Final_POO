(function () {
  "use strict";

  const API = "http://localhost:8080";
  const CLAVE_SESION = "ledger.sesion";
  const CLAVE_AVISO = "ledger.aviso";
  const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const Sesion = {
    leer() {
      try {
        const s = JSON.parse(sessionStorage.getItem(CLAVE_SESION) || "null");
        return s && new Date(s.expires_at) > new Date() ? s : null;
      } catch (e) { return null; }
    },
    guardar(s) {
      try { sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s)); } catch (e) {  }
    },
    tomarAviso() {
      try {
        const aviso = sessionStorage.getItem(CLAVE_AVISO);
        sessionStorage.removeItem(CLAVE_AVISO);
        return aviso ? JSON.parse(aviso) : null;
      } catch (e) { return null; }
    }
  };

  if (Sesion.leer()) {
    location.replace("dashboard.html");
    return;
  }

  const $ = (sel, raiz = document) => raiz.querySelector(sel);

  function guilloche(canvas) {
    window.Guilloche.montar(canvas, {
      geometria: (ancho, alto) => window.matchMedia("(max-width: 960px)").matches
        ? { x: ancho * 0.86, y: alto * 0.38, escala: ancho * 0.42 }
        : { x: ancho * 0.98, y: alto * 0.06, escala: ancho * 0.38 }
    });
  }

  function mostrarErrorCampo(input, mensaje) {
    const error = document.getElementById(input.id + "-error");
    input.setAttribute("aria-invalid", mensaje ? "true" : "false");
    if (!error) return;
    error.textContent = mensaje || "";
    void error.offsetHeight;
    error.classList.toggle("visible", Boolean(mensaje));
  }

  function mostrarAlerta(texto, tipo) {
    const alerta = $("#alerta");
    alerta.className = "alerta" + (tipo ? " " + tipo : "");
    alerta.textContent = texto;
    alerta.hidden = false;

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

    setTimeout(() => location.assign("dashboard.html"), reducirMovimiento ? 0 : 450);
  }

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

  guilloche($(".marca canvas"));
  ({ login: iniciarLogin, registro: iniciarRegistro })[document.body.dataset.pagina]();
})();
