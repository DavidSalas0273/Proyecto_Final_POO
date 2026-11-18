(function () {
  "use strict";

  const WS_URL = "ws://localhost:8081/ws";
  const API = "http://localhost:8080";
  const SVG_NS = "http://www.w3.org/2000/svg";
  const CLAVE_SESION = "ledger.sesion";
  const CLAVE_TEMA = "ledger.tema";

  const REGLAS = { umbralAltoValor: 10000, maxEnviosVentana: 5, ventanaSegundos: 60, limitePorOperacion: 1000000 };

  const MOTIVOS = {
    INSUFFICIENT_FUNDS: "Fondos insuficientes",
    CURRENCY_MISMATCH: "Divisa distinta a la de la cuenta",
    AMOUNT_LIMIT_EXCEEDED: "Supera el límite por operación",
    HIGH_VALUE_TRANSACTION: "Monto inusualmente alto",
    ANOMALOUS_FREQUENCY: "Demasiados envíos seguidos"
  };
  const EXPLICACIONES = {
    INSUFFICIENT_FUNDS: "La cuenta de origen no tenía saldo suficiente, así que no se movió dinero.",
    CURRENCY_MISMATCH: "Las cuentas operan en USD y la operación venía en otra divisa.",
    AMOUNT_LIMIT_EXCEEDED: "El monto supera el límite de US$ 1.000.000 por operación.",
    HIGH_VALUE_TRANSACTION: "El monto supera los US$ 10.000 que vigila el detector de fraude.",
    ANOMALOUS_FREQUENCY: "La misma cuenta hizo más de 5 envíos en menos de 60 segundos."
  };

  const SESION = (() => {
    try {
      const s = JSON.parse(sessionStorage.getItem(CLAVE_SESION) || "null");
      return s && new Date(s.expires_at) > new Date() ? s : null;
    } catch (e) { return null; }
  })();
  if (!SESION) {
    location.replace("login.html");
    return;
  }

  function terminarSesion(texto, tipo) {
    try {
      sessionStorage.removeItem(CLAVE_SESION);
      sessionStorage.setItem("ledger.aviso", JSON.stringify({ texto, tipo }));
    } catch (e) {  }
    location.replace("login.html");
  }
  const sesionVencida = () => terminarSesion("Tu sesión venció. Vuelve a ingresar.", "aviso");

  const estado = {
    movimientos: new Map(),
    cuentas: new Map(),
    pendientes: new Map(),
    filtro: "todos",
    busqueda: "",
    soloMios: true,
    alertasMias: false,
    usuario: SESION.username,
    vista: "inicio",
    alertasNoVistas: 0,
    alertadas: new Set()
  };

  const $ = (id) => document.getElementById(id);

  function dinero(valor, divisa) {
    try {
      return new Intl.NumberFormat("es-CO", { style: "currency", currency: divisa || "USD", minimumFractionDigits: 2 })
        .format(Number(valor || 0));
    } catch (e) {
      return (divisa || "") + " " + Number(valor || 0).toFixed(2);
    }
  }
  const compacto = new Intl.NumberFormat("es-CO", { notation: "compact", maximumFractionDigits: 1 });
  const hora = iso => iso ? new Date(iso).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" }) : "";
  const mismoUsuario = (a, b) => (a || "").trim().toLowerCase() === (b || "").trim().toLowerCase();
  const esSim = u => /^sim_/i.test(u || "");
  const esDenegado = m => m.status === "REJECTED";
  const esRecarga = m => m.type === "DEPOSIT";
  const pct = (parte, total) => total ? Math.round((parte / total) * 100) : 0;
  const color = variable => getComputedStyle(document.documentElement).getPropertyValue(variable).trim();

  function el(tag, clase, texto) {
    const nodo = document.createElement(tag);
    if (clase) nodo.className = clase;
    if (texto !== undefined) nodo.textContent = texto;
    return nodo;
  }
  function svg(tag, atributos = {}, texto) {
    const nodo = document.createElementNS(SVG_NS, tag);
    Object.entries(atributos).forEach(([k, v]) => nodo.setAttribute(k, v));
    if (texto !== undefined) nodo.textContent = texto;
    return nodo;
  }

  const ICONOS = {
    inicio: ["M3 11 12 4l9 7", "M5 10v10h5v-6h4v6h5V10"],
    mover: ["M7 7h13", "M16 3l4 4-4 4", "M17 17H4", "M8 13l-4 4 4 4"],
    movimientos: ["M9 6h12", "M9 12h12", "M9 18h12", "M4 6h.01", "M4 12h.01", "M4 18h.01"],
    alertas: ["M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z", "M12 9v4", "M12 17h.01"],
    simulador: ["M9 3h6", "M10 3v6l-5.5 9.5A2 2 0 0 0 6.2 21h11.6a2 2 0 0 0 1.7-2.5L14 9V3", "M7.5 15h9"],
    analisis: ["M3 3v18h18", "M7 16v-4", "M12 16V8", "M17 16v-7"],
    enviado: ["M7 17 17 7", "M8 7h9v9"],
    recibido: ["M17 7 7 17", "M16 17H7V8"],
    recarga: ["M12 5v14", "M5 12h14"],
    salir: ["M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9"],
    menu: ["M3 6h18", "M3 12h18", "M3 18h18"],
    check: ["M20 6 9 17l-5-5"],
    reloj: ["M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z", "M12 7v5l3 2"],
    sol: ["M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0z", "M12 2v2", "M12 20v2", "M4.9 4.9l1.4 1.4", "M17.7 17.7l1.4 1.4",
      "M2 12h2", "M20 12h2", "M4.9 19.1l1.4-1.4", "M17.7 6.3l1.4-1.4"],
    luna: ["M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"]
  };

  function dibujarIcono(nodo, nombre) {
    nodo.setAttribute("viewBox", "0 0 24 24");
    nodo.setAttribute("fill", "none");
    nodo.setAttribute("stroke", "currentColor");
    nodo.setAttribute("stroke-width", "2");
    nodo.setAttribute("stroke-linecap", "round");
    nodo.setAttribute("stroke-linejoin", "round");
    nodo.setAttribute("aria-hidden", "true");
    nodo.replaceChildren(...(ICONOS[nombre] || []).map(d => svg("path", { d })));
  }
  function icono(nombre) {
    const nodo = document.createElementNS(SVG_NS, "svg");
    nodo.setAttribute("class", "icono-svg");
    dibujarIcono(nodo, nombre);
    return nodo;
  }

  function temaActual() {
    return document.documentElement.dataset.theme
      || (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  }
  function pintarControlesTema() {
    const oscuro = temaActual() === "dark";
    document.querySelectorAll("[data-icono=tema]").forEach(n => dibujarIcono(n, oscuro ? "sol" : "luna"));
    document.querySelectorAll("[data-texto-tema]").forEach(n => { n.textContent = oscuro ? "Modo claro" : "Modo oscuro"; });
    document.querySelectorAll("[data-accion=tema]").forEach(b => {
      b.setAttribute("aria-label", oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
      b.title = oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro";
    });
  }
  function cambiarTema() {
    const nuevo = temaActual() === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nuevo;
    try { localStorage.setItem(CLAVE_TEMA, nuevo); } catch (e) {  }
    pintarControlesTema();
    renderizar();
  }

  function toast(texto, tipo) {
    const t = el("div", "toast " + (tipo || ""), texto);
    $("toasts").append(t);
    setTimeout(() => {
      t.classList.add("saliendo");
      setTimeout(() => t.remove(), 220);
    }, 5000);
  }

  function describirParticipantes(m) {
    return esRecarga(m) ? "Recarga a " + m.to_user : m.from_user + " → " + m.to_user;
  }

  function alertaEnPantalla(m) {
    const sospechosa = Boolean(m.suspicious);
    const clave = m.tx_id + (sospechosa ? ":sos" : ":den");
    if (estado.alertadas.has(clave)) return;
    estado.alertadas.add(clave);
    estado.alertasNoVistas += estado.vista === "alertas" ? 0 : 1;
    pintarContadorAlertas();

    const codigo = sospechosa ? m.fraud_reason : m.reason;
    const caja = el("div", "alerta-pantalla " + (sospechosa ? "peligro" : "advertencia"));
    caja.setAttribute("role", "alert");
    const signo = el("span", "signo " + (sospechosa ? "peligro" : "advertencia"));
    signo.append(icono("alertas"));
    const cuerpo = el("div");
    cuerpo.append(el("h3", "", sospechosa ? "Transacción sospechosa detectada" : "Transacción denegada"));
    const detalle = el("p");
    detalle.append(el("span", "detalle", describirParticipantes(m) + " · " + dinero(m.amount, m.currency)));
    cuerpo.append(detalle, el("p", "", EXPLICACIONES[codigo] || MOTIVOS[codigo] || "El sistema bloqueó la operación."));
    const acciones = el("div", "alerta-acciones");
    const ver = el("button", "principal", "Ver alertas");
    ver.type = "button";
    const cerrar = el("button", "", "Cerrar");
    cerrar.type = "button";
    acciones.append(ver, cerrar);
    cuerpo.append(acciones);
    caja.append(signo, cuerpo);

    const contenedor = $("alertas-pantalla");
    contenedor.append(caja);
    while (contenedor.children.length > 3) contenedor.firstElementChild.remove();

    const quitar = () => {
      if (!caja.isConnected) return;
      caja.classList.add("saliendo");
      setTimeout(() => caja.remove(), 200);
    };
    ver.addEventListener("click", () => { quitar(); location.hash = "#alertas"; });
    cerrar.addEventListener("click", quitar);
    setTimeout(quitar, 12000);
  }

  function pintarContadorAlertas() {
    const c = $("contador-alertas");
    c.hidden = estado.alertasNoVistas === 0;
    c.textContent = estado.alertasNoVistas > 99 ? "99+" : String(estado.alertasNoVistas);
  }

  const VISTAS = ["inicio", "mover", "movimientos", "alertas", "simulador", "analisis"];
  let pestanaPendiente = null;

  function irA(nombre) {
    const vista = VISTAS.includes(nombre) ? nombre : "inicio";
    estado.vista = vista;
    document.querySelectorAll("main .vista").forEach(s => { s.hidden = s.dataset.vista !== vista; });
    document.querySelectorAll(".menu a").forEach(a => {
      if (a.dataset.vista === vista) a.setAttribute("aria-current", "page");
      else a.removeAttribute("aria-current");
    });
    if (vista === "alertas") { estado.alertasNoVistas = 0; pintarContadorAlertas(); }
    if (vista === "mover" && pestanaPendiente) { cambiarPestana(pestanaPendiente); pestanaPendiente = null; }
    cerrarMenuMovil();
    window.scrollTo({ top: 0 });
    renderizar();
  }

  function cerrarMenuMovil() {
    const app = $("app");
    if (!app.classList.contains("menu-abierto")) return;
    app.classList.remove("menu-abierto");
    $("velo").classList.remove("visible");
    setTimeout(() => { $("velo").hidden = true; }, 260);
    $("boton-menu").setAttribute("aria-expanded", "false");
  }
  function alternarMenu() {
    const app = $("app");
    if (matchMedia("(max-width: 900px)").matches) {
      const abrir = !app.classList.contains("menu-abierto");
      if (!abrir) return cerrarMenuMovil();
      app.classList.add("menu-abierto");
      $("velo").hidden = false;
      void $("velo").offsetHeight;
      $("velo").classList.add("visible");
      $("boton-menu").setAttribute("aria-expanded", "true");
    } else {
      app.classList.toggle("compacta");
      $("boton-menu").setAttribute("aria-expanded", String(!app.classList.contains("compacta")));
      renderizar();
    }
  }

  const cuentaPropia = () => [...estado.cuentas.values()].find(c => mismoUsuario(c.user, estado.usuario));
  const saldoPropio = () => { const c = cuentaPropia(); return c ? Number(c.balance) : 0; };

  function pintarSaldo() {
    const cuenta = cuentaPropia();
    const texto = dinero(cuenta ? cuenta.balance : 0, cuenta ? cuenta.currency : "USD");
    $("saldo-actual").textContent = texto;
    $("saldo-mini").textContent = texto;
    $("saldo-nota").textContent = cuenta
      ? "Cuenta @" + cuenta.user + ", actualizada a las " + hora(cuenta.updated_at)
      : "Tu cuenta está en $0: recárgala para empezar a enviar";
    validarMontoEnvio();
  }

  function validarMontoEnvio() {
    const monto = Number($("form-enviar").amount.value || 0);
    $("aviso-saldo").hidden = !(monto > 0 && monto > saldoPropio());
  }

  function cambiarPestana(cual) {
    const enviar = cual === "enviar";
    $("tab-enviar").setAttribute("aria-selected", String(enviar));
    $("tab-recargar").setAttribute("aria-selected", String(!enviar));
    $("form-enviar").hidden = !enviar;
    $("form-recargar").hidden = enviar;
    document.querySelector(".tabs").dataset.activa = cual;
  }

  function prepararEnvio(destino) {
    pestanaPendiente = "enviar";
    location.hash = "#mover";
    if (estado.vista === "mover") cambiarPestana("enviar");
    const f = $("form-enviar");
    f.to_user.value = destino;
    setTimeout(() => f.amount.focus(), 0);
  }

  function pintarBarras(idLista, opciones) {
    const lista = [...estado.cuentas.values()].filter(c => !esSim(c.user))
      .sort((a, b) => Number(b.balance) - Number(a.balance));
    const max = opciones.maximo || 8;
    const propia = lista.find(c => mismoUsuario(c.user, estado.usuario));
    let visibles = lista.slice(0, max);
    if (propia && !visibles.includes(propia)) visibles = [...visibles.slice(0, max - 1), propia];
    const tope = Math.max(1, ...visibles.map(c => Number(c.balance)));

    $(idLista).replaceChildren(...visibles.map(c => {
      const esPropia = mismoUsuario(c.user, estado.usuario);
      const li = el("li");
      const boton = el("button", "barra" + (esPropia ? " propia" : ""));
      boton.type = "button";
      boton.title = esPropia ? "Tu cuenta: recárgala" : "Enviar dinero a " + c.user;
      const fila = el("div", "barra-fila");
      fila.append(el("b", "", esPropia ? c.user + " (tú)" : c.user), el("span", "", dinero(c.balance, c.currency)));
      const pista = el("div", "pista");
      const relleno = el("span", "relleno");
      pista.append(relleno);
      boton.append(fila, pista);
      boton.addEventListener("click", () => {
        if (esPropia) { pestanaPendiente = "recargar"; location.hash = "#mover"; cambiarPestana("recargar"); }
        else prepararEnvio(c.user);
      });
      li.append(boton);
      requestAnimationFrame(() => { relleno.style.transform = "scaleX(" + (Number(c.balance) / tope) + ")"; });
      return li;
    }));
    return { total: lista.length, visibles: visibles.length };
  }

  function pintarCuentasMover() {
    const r = pintarBarras("cuentas", { maximo: 10 });
    $("cuentas-vacio").hidden = r.total > 0;
    const ocultas = r.total - r.visibles;
    $("cuentas-mas").hidden = ocultas <= 0;
    $("cuentas-mas").textContent = "y " + ocultas + (ocultas === 1 ? " cuenta más" : " cuentas más") + " con menos saldo; escribe su usuario en «Para».";
    $("lista-usuarios").replaceChildren(...[...estado.cuentas.values()].filter(c => !esSim(c.user))
      .map(c => { const o = el("option"); o.value = c.user; return o; }));
  }

  function direccion(m) {
    if (esRecarga(m)) return "recarga";
    if (mismoUsuario(m.from_user, estado.usuario)) return "enviado";
    if (mismoUsuario(m.to_user, estado.usuario)) return "recibido";
    return "tercero";
  }
  const esPropio = m => direccion(m) !== "tercero" && (!esRecarga(m) || mismoUsuario(m.to_user, estado.usuario));

  function titulo(m) {
    const d = direccion(m);
    if (d === "recarga") return mismoUsuario(m.to_user, estado.usuario) ? "Recargaste tu cuenta" : "Recarga a " + m.to_user;
    if (esDenegado(m)) {
      if (d === "enviado") return "Intentaste enviar a " + m.to_user;
      if (d === "recibido") return m.from_user + " intentó enviarte";
    } else {
      if (d === "enviado") return "Enviaste a " + m.to_user;
      if (d === "recibido") return "Recibiste de " + m.from_user;
    }
    return m.from_user + " → " + m.to_user;
  }

  function crearFila(m, nuevo) {
    const d = direccion(m);
    const clases = ["mov", d];
    if (esDenegado(m)) clases.push("denegada");
    if (m.suspicious) clases.push("sospechosa");
    if (nuevo) clases.push("nuevo");
    const li = el("li", clases.join(" "));
    li.dataset.txId = m.tx_id;

    const caja = el("div", "icono");
    const conAlerta = m.suspicious || esDenegado(m);
    caja.append(icono(conAlerta ? "alertas" : d === "tercero" ? "mover" : d));
    caja.title = conAlerta ? (m.suspicious ? "Sospechosa" : "Denegada") : { enviado: "Envío", recibido: "Recibido", recarga: "Recarga", tercero: "Transferencia" }[d];
    li.append(caja);

    const info = el("div", "info");
    info.append(el("b", "", titulo(m)));
    info.append(el("small", "", hora(m.timestamp) + " · #" + m.tx_id.slice(0, 8)));
    if (conAlerta) {
      const motivo = el("span", "motivo");
      motivo.append(icono("alertas"), document.createTextNode(m.suspicious
        ? "Sospechosa: " + (MOTIVOS[m.fraud_reason] || m.fraud_reason)
        : "Denegada: " + (MOTIVOS[m.reason] || m.reason || "operación no permitida")));
      info.append(motivo);
    }
    li.append(info);

    const lado = el("div", "lado");
    const entra = d === "recibido" || (d === "recarga");
    const signo = esDenegado(m) ? "" : entra ? "+" : "−";
    lado.append(el("span", "monto" + (entra && !esDenegado(m) ? " mas" : ""), signo + dinero(m.amount, m.currency)));
    const etiquetas = el("div", "etiquetas");
    if (esSim(m.from_user) || esSim(m.to_user)) etiquetas.append(el("span", "badge sim", "Simulación"));
    const badge = el("span", "badge " + (m.suspicious ? "SUSPICIOUS" : m.status));
    if (conAlerta) badge.append(icono("alertas"));
    badge.append(document.createTextNode(m.suspicious ? "Sospechosa" : esDenegado(m) ? "Denegada" : "Completada"));
    etiquetas.append(badge);
    lado.append(etiquetas);
    li.append(lado);
    return li;
  }

  const ordenados = () => [...estado.movimientos.values()].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

  function pintarInicio(lista, resaltar) {
    const propios = lista.filter(esPropio);
    const completados = propios.filter(m => !esDenegado(m));
    const suma = arr => arr.reduce((s, m) => s + (Number(m.amount) || 0), 0);
    const enviados = completados.filter(m => direccion(m) === "enviado");
    const recibidos = completados.filter(m => direccion(m) === "recibido");
    const recargas = completados.filter(m => direccion(m) === "recarga");
    const denegadas = propios.filter(esDenegado).length;
    const sospechosas = propios.filter(m => m.suspicious).length;
    const total = propios.length;

    const tarjeta = (id, arr, singular, plural) => {
      $("v-" + id).textContent = dinero(suma(arr), "USD");
      $("d-" + id).textContent = arr.length + " " + (arr.length === 1 ? singular : plural);
      $("c-" + id).style.transform = "scaleX(" + (total ? arr.length / total : 0) + ")";
      $("p-" + id).textContent = pct(arr.length, total) + " %";
    };
    tarjeta("enviado", enviados, "envío", "envíos");
    tarjeta("recibido", recibidos, "recibido", "recibidos");
    tarjeta("recargas", recargas, "recarga", "recargas");
    $("v-aprobacion").textContent = total ? pct(completados.length, total) + " %" : "—";
    $("d-aprobacion").textContent = total
      ? denegadas + (denegadas === 1 ? " denegada" : " denegadas") + " · " + sospechosas + (sospechosas === 1 ? " sospechosa" : " sospechosas")
      : "Sin movimientos todavía";
    $("c-aprobacion").style.transform = "scaleX(" + (total ? completados.length / total : 0) + ")";
    $("p-aprobacion").textContent = pct(completados.length, total) + " %";

    const texto = $("actividad-texto");
    texto.replaceChildren();
    if (!total) {
      texto.textContent = "Aún no has hecho transacciones. Recarga tu cuenta para empezar.";
    } else {
      const ultimo = propios[0];
      texto.append("Has hecho ", el("b", "", String(total)), total === 1 ? " movimiento. " : " movimientos. ",
        "El último: ", el("b", "", titulo(ultimo).toLowerCase() + " por " + dinero(ultimo.amount, ultimo.currency)),
        " a las " + hora(ultimo.timestamp) + ".");
    }
    pintarDona("dona-personal", "leyenda-personal", propios, filtro => {
      aplicarFiltro(filtro);
      $("solo-mios").checked = true;
      estado.soloMios = true;
      location.hash = "#movimientos";
    });

    $("lista-ultimos").replaceChildren(...propios.slice(0, 6).map(m => crearFila(m, m.tx_id === resaltar)));
    $("ultimos-vacio").hidden = total > 0;
    $("saludo-nombre").textContent = (SESION.full_name || SESION.username).split(" ")[0];
  }

  function pasaFiltros(m) {
    const d = direccion(m);
    const filtros = {
      todos: () => true,
      enviados: () => d === "enviado" || (!estado.soloMios && d === "tercero"),
      recibidos: () => d === "recibido",
      recargas: () => esRecarga(m),
      denegados: () => esDenegado(m),
      sospechosos: () => m.suspicious
    };
    const texto = estado.busqueda;
    const coincide = !texto || [m.from_user, m.to_user, m.tx_id].some(v => (v || "").toLowerCase().includes(texto));
    return filtros[estado.filtro]() && coincide && (!estado.soloMios || esPropio(m));
  }

  function pintarRegistro(lista, resaltar) {
    const visibles = lista.filter(pasaFiltros);
    $("movs").replaceChildren(...visibles.map(m => crearFila(m, m.tx_id === resaltar)));
    $("movs-vacio").hidden = visibles.length > 0;
    $("movs-vacio").textContent = lista.length ? "No hay movimientos con estos filtros." : "Esperando movimientos…";
    const base = estado.soloMios ? lista.filter(esPropio).length : lista.length;
    $("movs-contador").textContent = visibles.length === base ? base + " en total" : visibles.length + " de " + base;
  }

  function aplicarFiltro(filtro) {
    estado.filtro = filtro;
    document.querySelectorAll(".filtros button").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.filtro === filtro)));
    renderizar();
  }

  function pintarAlertas(lista) {
    const alertas = lista.filter(m => (esDenegado(m) || m.suspicious) && (!estado.alertasMias || esPropio(m)));
    $("n-sospechosas").textContent = alertas.filter(m => m.suspicious).length;
    $("n-denegadas").textContent = alertas.filter(esDenegado).length;
    $("alertas-vacio").hidden = alertas.length > 0;
    $("lista-alertas").replaceChildren(...alertas.map(m => {
      const sospechosa = Boolean(m.suspicious);
      const codigo = sospechosa ? m.fraud_reason : m.reason;
      const fila = el("article", "alerta-fila " + (sospechosa ? "peligro" : "advertencia"));
      const signo = el("span", "signo " + (sospechosa ? "peligro" : "advertencia"));
      signo.append(icono("alertas"));
      const cuerpo = el("div");
      cuerpo.append(el("h3", "", (sospechosa ? "Sospechosa: " : "Denegada: ") + (MOTIVOS[codigo] || codigo || "operación no permitida")));
      cuerpo.append(el("p", "", describirParticipantes(m) + " · " + dinero(m.amount, m.currency)));
      cuerpo.append(el("p", "", EXPLICACIONES[codigo] || ""));
      cuerpo.append(el("small", "", hora(m.timestamp) + " · #" + m.tx_id.slice(0, 8)
        + (esSim(m.from_user) || esSim(m.to_user) ? " · simulación" : "")));
      const lado = el("div", "etiquetas");
      const badge = el("span", "badge " + (sospechosa ? "SUSPICIOUS" : "REJECTED"));
      badge.append(icono("alertas"), document.createTextNode(sospechosa ? "Sospechosa" : "Denegada"));
      lado.append(badge);
      fila.append(signo, cuerpo, lado);
      return fila;
    }));
  }

  function pintarDona(idSvg, idLeyenda, lista, alFiltrar) {
    const total = lista.length;
    const segmentos = [
      { etiqueta: "Completados", valor: lista.filter(m => !esDenegado(m) && !m.suspicious).length, color: "--serie-ok", filtro: "todos" },
      { etiqueta: "Denegados", valor: lista.filter(esDenegado).length, color: "--serie-warn", filtro: "denegados" },
      { etiqueta: "Sospechosos", valor: lista.filter(m => m.suspicious).length, color: "--serie-danger", filtro: "sospechosos" }
    ];
    const lienzo = $(idSvg);
    lienzo.replaceChildren();
    const r = 60, grosor = 18, C = 2 * Math.PI * r, cx = 75, cy = 75;
    lienzo.append(svg("circle", { cx, cy, r, fill: "none", stroke: color("--line-soft"), "stroke-width": grosor }));
    const separacion = segmentos.filter(s => s.valor > 0).length > 1 ? 3 : 0;
    let acumulado = 0;
    segmentos.forEach(s => {
      if (!s.valor) return;
      const largo = (s.valor / total) * C;
      const arco = svg("circle", {
        cx, cy, r, fill: "none", stroke: color(s.color), "stroke-width": grosor,
        "stroke-dasharray": `${Math.max(0.5, largo - separacion)} ${C}`,
        "stroke-dashoffset": -acumulado, transform: `rotate(-90 ${cx} ${cy})`
      });
      arco.append(svg("title", {}, `${s.etiqueta}: ${s.valor} (${pct(s.valor, total)} %)`));
      lienzo.append(arco);
      acumulado += largo;
    });
    lienzo.append(svg("text", { x: cx, y: cy + 4, "text-anchor": "middle", class: "centro-valor" }, String(total)));
    lienzo.append(svg("text", { x: cx, y: cy + 22, "text-anchor": "middle" }, total === 1 ? "movimiento" : "movimientos"));

    $(idLeyenda).replaceChildren(...segmentos.map(s => {
      const li = el("li");
      const item = el(alFiltrar ? "button" : "div");
      if (alFiltrar) {
        item.type = "button";
        item.title = "Ver estos movimientos";
        item.addEventListener("click", () => alFiltrar(s.filtro));
      }
      const muestra = el("i", "muestra");
      muestra.style.background = color(s.color);
      const cifra = el("span", "cifra", String(s.valor));
      cifra.append(el("small", "", pct(s.valor, total) + " %"));
      item.append(muestra, el("b", "", s.etiqueta), cifra);
      li.append(item);
      return li;
    }));
    return segmentos;
  }

  const INTERVALOS = [60e3, 5 * 60e3, 15 * 60e3, 30 * 60e3, 3600e3, 3 * 3600e3, 6 * 3600e3, 12 * 3600e3, 864e5, 7 * 864e5];
  const NOMBRES_INTERVALO = { 60000: "1 minuto", 300000: "5 minutos", 900000: "15 minutos", 1800000: "30 minutos",
    3600000: "1 hora", 10800000: "3 horas", 21600000: "6 horas", 43200000: "12 horas", 86400000: "1 día", 604800000: "1 semana" };

  function agrupar(lista) {
    const tiempos = lista.map(m => +new Date(m.timestamp)).filter(Number.isFinite);
    if (!tiempos.length) return null;
    const tMax = Math.max(...tiempos), tMin = Math.min(...tiempos);
    const tam = INTERVALOS.find(i => (tMax - tMin) / i <= 23) || INTERVALOS[INTERVALOS.length - 1];
    const fin = Math.floor(tMax / tam) * tam;
    const n = Math.max(8, Math.floor((fin - Math.floor(tMin / tam) * tam) / tam) + 1);
    const inicio = fin - (n - 1) * tam;
    const cubetas = Array.from({ length: n }, (_, i) => ({ t: inicio + i * tam, monto: 0, cantidad: 0 }));
    lista.forEach(m => {
      const i = Math.floor((+new Date(m.timestamp) - inicio) / tam);
      if (i < 0 || i >= n || esDenegado(m)) return;
      cubetas[i].monto += Number(m.amount) || 0;
      cubetas[i].cantidad++;
    });
    return { cubetas, tam };
  }
  const etiquetaTiempo = (t, tam) => tam >= 864e5
    ? new Date(t).toLocaleDateString("es-CO", { day: "numeric", month: "short" })
    : new Date(t).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" });
  function techo(v) {
    if (v <= 0) return 1;
    const p = Math.pow(10, Math.floor(Math.log10(v)));
    return [1, 2, 2.5, 5, 10].find(x => v / p <= x) * p;
  }

  function pintarVolumen(lista) {
    const contenedor = $("grafico-volumen");
    const datos = agrupar(lista);
    contenedor.replaceChildren();
    if (!datos) {
      $("volumen-nota").textContent = "";
      contenedor.append(el("p", "vacio", "Aún no hay movimientos para graficar."));
      return;
    }
    const { cubetas, tam } = datos;
    const n = cubetas.length;
    const totalMovido = cubetas.reduce((s, c) => s + c.monto, 0);
    $("volumen-nota").textContent = dinero(totalMovido, "USD") + " en total · intervalos de " + NOMBRES_INTERVALO[tam];
    const W = Math.max(280, contenedor.clientWidth || 600), H = 230;
    const m = { t: 10, r: 14, b: 26, l: 54 };
    const iw = W - m.l - m.r, ih = H - m.t - m.b;
    const maximo = techo(Math.max(...cubetas.map(c => c.monto)));
    const x = i => m.l + (n === 1 ? iw / 2 : (i * iw) / (n - 1));
    const y = v => m.t + ih - (v / maximo) * ih;
    const serie = color("--serie-ok");

    const lienzo = svg("svg", { width: W, height: H, viewBox: `0 0 ${W} ${H}`, role: "img",
      "aria-label": "Dinero movido por intervalo. Total " + dinero(totalMovido, "USD") });
    const defs = svg("defs");
    const degradado = svg("linearGradient", { id: "relleno-volumen", x1: 0, y1: 0, x2: 0, y2: 1 });
    degradado.append(svg("stop", { offset: "0%", "stop-color": serie, "stop-opacity": 0.28 }),
      svg("stop", { offset: "100%", "stop-color": serie, "stop-opacity": 0.02 }));
    defs.append(degradado);
    lienzo.append(defs);
    [0, 0.5, 1].forEach(f => {
      const v = maximo * f;
      lienzo.append(svg("line", { x1: m.l, x2: W - m.r, y1: y(v), y2: y(v), stroke: color("--line-soft"), "stroke-width": 1 }));
      lienzo.append(svg("text", { x: m.l - 8, y: y(v) + 4, "text-anchor": "end" }, "$" + compacto.format(v)));
    });
    [0, Math.floor((n - 1) / 2), n - 1].forEach((i, k) => {
      lienzo.append(svg("text", { x: x(i), y: H - 6, "text-anchor": k === 0 ? "start" : k === 2 ? "end" : "middle" },
        etiquetaTiempo(cubetas[i].t, tam)));
    });
    const puntos = cubetas.map((c, i) => [x(i), y(c.monto)]);
    const linea = puntos.map((p, i) => (i ? "L" : "M") + p[0].toFixed(1) + " " + p[1].toFixed(1)).join(" ");
    lienzo.append(svg("path", { d: linea + ` L${x(n - 1)} ${y(0)} L${x(0)} ${y(0)} Z`, fill: "url(#relleno-volumen)" }));
    lienzo.append(svg("path", { d: linea, fill: "none", stroke: serie, "stroke-width": 2, "stroke-linejoin": "round", "stroke-linecap": "round" }));
    const ultimo = puntos[n - 1];
    lienzo.append(svg("circle", { cx: ultimo[0], cy: ultimo[1], r: 5, fill: serie, stroke: color("--surface"), "stroke-width": 2 }));

    const guia = svg("line", { y1: m.t, y2: m.t + ih, stroke: color("--muted"), "stroke-width": 1, "stroke-dasharray": "3 3", opacity: 0 });
    const marcador = svg("circle", { r: 5, fill: serie, stroke: color("--surface"), "stroke-width": 2, opacity: 0 });
    const zona = svg("rect", { x: m.l, y: 0, width: iw, height: H, fill: "transparent" });
    lienzo.append(guia, marcador, zona);
    contenedor.append(lienzo);
    const tip = el("div", "tooltip");
    contenedor.append(tip);
    const mostrar = evento => {
      const caja = lienzo.getBoundingClientRect();
      const i = Math.max(0, Math.min(n - 1, Math.round(((evento.clientX - caja.left - m.l) / iw) * (n - 1))));
      const c = cubetas[i];
      guia.setAttribute("x1", x(i)); guia.setAttribute("x2", x(i)); guia.setAttribute("opacity", 1);
      marcador.setAttribute("cx", x(i)); marcador.setAttribute("cy", y(c.monto)); marcador.setAttribute("opacity", 1);
      tip.replaceChildren(el("b", "", dinero(c.monto, "USD")),
        el("span", "", etiquetaTiempo(c.t, tam) + " – " + etiquetaTiempo(c.t + tam, tam) + " · " + c.cantidad
          + (c.cantidad === 1 ? " movimiento" : " movimientos")));
      const ancho = tip.offsetWidth;
      tip.style.transform = `translate(${Math.min(Math.max(0, x(i) - ancho / 2), W - ancho)}px, ${Math.max(0, y(c.monto) - 58)}px)`;
      tip.classList.add("visible");
    };
    zona.addEventListener("pointermove", mostrar);
    zona.addEventListener("pointerdown", mostrar);
    zona.addEventListener("pointerleave", () => {
      guia.setAttribute("opacity", 0); marcador.setAttribute("opacity", 0); tip.classList.remove("visible");
    });
  }

  function pintarAnalisis(lista) {
    const segmentos = pintarDona("dona-global", "leyenda-global", lista, filtro => {
      estado.soloMios = false;
      $("solo-mios").checked = false;
      aplicarFiltro(filtro);
      location.hash = "#movimientos";
    });
    const total = lista.length;
    $("g-total").textContent = total;
    [["ok", 0], ["den", 1], ["sos", 2]].forEach(([id, i]) => {
      $("g-" + id).textContent = segmentos[i].valor;
      $("gc-" + id).style.transform = "scaleX(" + (total ? segmentos[i].valor / total : 0) + ")";
      $("gp-" + id).textContent = pct(segmentos[i].valor, total) + " %";
    });
    pintarVolumen(lista);
    pintarBarras("saldos-globales", { maximo: 8 });
  }

  let pendiente = false;
  let resaltarSiguiente = null;
  function renderizar(resaltar) {
    if (resaltar) resaltarSiguiente = resaltar;
    if (pendiente) return;
    pendiente = true;

    const programar = document.hidden ? (fn => setTimeout(fn, 0)) : requestAnimationFrame;
    programar(() => {
      pendiente = false;
      const lista = ordenados();
      const r = resaltarSiguiente;
      resaltarSiguiente = null;
      pintarSaldo();
      const pinta = {
        inicio: () => pintarInicio(lista, r),
        mover: () => pintarCuentasMover(),
        movimientos: () => pintarRegistro(lista, r),
        alertas: () => pintarAlertas(lista),
        simulador: () => {},
        analisis: () => pintarAnalisis(lista)
      };
      pinta[estado.vista]();
    });
  }

  const ESPERADOS = {
    completada: { texto: "Completada", icono: "check" },
    denegada: { texto: "Denegada", icono: "alertas" },
    sospechosa: { texto: "Sospechosa", icono: "alertas" }
  };
  const transferencia = (from, to, amount, currency, etiqueta, esperado) =>
    ({ ruta: "/transfer", cuerpo: { from_user: from, to_user: to, amount, currency: currency || "USD" }, etiqueta, esperado });
  const recarga = (user, amount, etiqueta, esperado, currency) =>
    ({ ruta: "/deposit", cuerpo: { user, amount, currency: currency || "USD" }, etiqueta, esperado: esperado || { estado: "completada" }, preparacion: true });

  const ESCENARIOS = [
    { id: "fondos", titulo: "Envío sin fondos", esperado: "denegada",
      descripcion: "Una cuenta nueva, con saldo $0, intenta enviar $100.",
      pasos: (o, d) => [transferencia(o, d, 100, "USD", "Envío de $100 sin saldo", { estado: "denegada", motivo: "INSUFFICIENT_FUNDS" })] },
    { id: "alto", titulo: "Monto inusualmente alto", esperado: "sospechosa",
      descripcion: "Se recargan $25.000 y se envían $15.000 de una sola vez.",
      pasos: (o, d) => [recarga(o, 25000, "Recarga de preparación: $25.000"),
        transferencia(o, d, 15000, "USD", "Envío de $15.000", { estado: "sospechosa", motivo: "HIGH_VALUE_TRANSACTION" })] },
    { id: "rafaga", titulo: "Ráfaga de envíos", esperado: "sospechosa",
      descripcion: "Seis envíos de $1 en pocos segundos desde la misma cuenta.",
      pasos: (o, d) => [recarga(o, 50, "Recarga de preparación: $50"),
        ...Array.from({ length: 6 }, (_, i) => transferencia(o, d, 1, "USD", `Envío ${i + 1} de 6 ($1)`,
          i < 5 ? { estado: "completada" } : { estado: "sospechosa", motivo: "ANOMALOUS_FREQUENCY" }))] },
    { id: "divisa", titulo: "Divisa distinta", esperado: "denegada",
      descripcion: "La cuenta opera en USD y se intenta enviar €10.",
      pasos: (o, d) => [recarga(o, 100, "Recarga de preparación: $100"),
        transferencia(o, d, 10, "EUR", "Envío de €10", { estado: "denegada", motivo: "CURRENCY_MISMATCH" })] },
    { id: "limite", titulo: "Límite por operación", esperado: "denegada",
      descripcion: "Se intenta recargar $2.000.000, por encima del límite de $1.000.000.",
      pasos: o => [Object.assign(recarga(o, 2000000, "Recarga de $2.000.000",
        { estado: "denegada", motivo: "AMOUNT_LIMIT_EXCEEDED" }), { preparacion: false })] },
    { id: "normal", titulo: "Transferencia normal", esperado: "completada",
      descripcion: "Se recargan $500 y se envían $50. Sirve como control: no debe alertar.",
      pasos: (o, d) => [recarga(o, 500, "Recarga de preparación: $500"),
        transferencia(o, d, 50, "USD", "Envío de $50", { estado: "completada" })] }
  ];

  const simulacion = { porTx: new Map() };

  function usuariosSimulados() {
    const id = Math.random().toString(36).slice(2, 7);
    return { origen: "sim_" + id, destino: "sim_" + id + "_b" };
  }

  function textoEsperado(e) {
    return ESPERADOS[e.estado].texto + (e.motivo ? " (" + MOTIVOS[e.motivo].toLowerCase() + ")" : "");
  }

  function pintarEscenarios() {
    $("escenarios").replaceChildren(...ESCENARIOS.map(e => {
      const card = el("article", "panel escenario");
      card.append(el("h3", "", e.titulo), el("p", "", e.descripcion));
      const pie = el("div", "pie");
      const esperado = el("span", "esperado " + e.esperado);
      esperado.append(icono(ESPERADOS[e.esperado].icono), document.createTextNode("Se espera: " + ESPERADOS[e.esperado].texto));
      const boton = el("button", "btn compacto");
      boton.type = "button";
      boton.append(icono("simulador"), document.createTextNode("Simular"));
      boton.addEventListener("click", () => {
        const u = usuariosSimulados();
        ejecutarCorrida(boton, e.titulo, e.pasos(u.origen, u.destino), u.origen);
      });
      pie.append(esperado, boton);
      card.append(pie);
      return card;
    }));
  }

  async function ejecutarCorrida(boton, nombre, pasos, origen) {
    $("bitacora-vacia").hidden = true;
    const corrida = el("article", "corrida");
    const cabecera = el("div", "corrida-cabecera");
    cabecera.append(el("b", "", nombre), el("small", "", "Cuenta de prueba " + origen + " · " + hora(new Date().toISOString())));
    const listaPasos = el("ol", "pasos");
    const veredicto = el("p", "veredicto");
    veredicto.hidden = true;
    corrida.append(cabecera, listaPasos, veredicto);
    $("bitacora").prepend(corrida);
    const registro = { pasos: [], veredicto };

    const filas = pasos.map(p => {
      const li = el("li", "paso");
      const chip = el("span", "estado-paso");
      chip.append(icono("reloj"), document.createTextNode("En cola"));
      const esperado = el("span", "esperado-texto", p.preparacion ? "Preparación" : "Se espera: " + textoEsperado(p.esperado));
      li.append(el("span", "", p.etiqueta), chip, esperado);
      listaPasos.append(li);
      const info = { paso: p, chip, esperado, resuelto: false, registro };
      registro.pasos.push(info);
      return info;
    });

    if (boton) boton.disabled = true;
    try {
      for (const info of filas) {
        try {
          const json = await post(info.paso.ruta, info.paso.cuerpo, null, false);
          simulacion.porTx.set(json.tx_id, info);
          const yaLlego = estado.movimientos.get(json.tx_id);
          if (yaLlego) actualizarPaso(yaLlego);
        } catch (e) {
          marcarChip(info.chip, "denegada", "Rechazada por la API");
          info.esperado.textContent = e.message;
          info.esperado.className = "esperado-texto difiere";
          info.resuelto = true;
        }
      }
    } finally {
      if (boton) boton.disabled = false;
    }
    revisarCorrida(registro);
  }

  function marcarChip(chip, clase, texto) {
    chip.className = "estado-paso " + clase;
    chip.replaceChildren(icono(clase === "completada" ? "check" : clase ? "alertas" : "reloj"), document.createTextNode(texto));
  }

  function actualizarPaso(m) {
    const info = simulacion.porTx.get(m.tx_id);
    if (!info) return;
    const obtenido = m.suspicious ? "sospechosa" : esDenegado(m) ? "denegada" : "completada";
    const codigo = m.suspicious ? m.fraud_reason : m.reason;
    marcarChip(info.chip, obtenido, ESPERADOS[obtenido].texto + (codigo ? " · " + (MOTIVOS[codigo] || codigo) : ""));
    const esperado = info.paso.esperado;
    if (obtenido === esperado.estado) {
      info.esperado.textContent = info.paso.preparacion ? "Preparación lista" : "Coincide con lo esperado";
      info.esperado.className = "esperado-texto coincide";
      info.resuelto = true;
    } else if (esperado.estado === "sospechosa" && obtenido === "completada") {
      info.esperado.textContent = "Completada; el detector de fraude la está analizando…";
      info.esperado.className = "esperado-texto";
      info.resuelto = false;

      clearTimeout(info.espera);
      info.espera = setTimeout(() => {
        if (info.resuelto) return;
        info.esperado.textContent = "No se marcó como sospechosa (se esperaba " + textoEsperado(esperado).toLowerCase() + ")";
        info.esperado.className = "esperado-texto difiere";
        info.resuelto = true;
        revisarCorrida(info.registro);
      }, 20000);
    } else {
      info.esperado.textContent = "No coincide: se esperaba " + textoEsperado(esperado).toLowerCase();
      info.esperado.className = "esperado-texto difiere";
      info.resuelto = true;
    }
    revisarCorrida(info.registro);
  }

  function revisarCorrida(registro) {
    if (!registro.pasos.every(p => p.resuelto)) return;
    const bien = registro.pasos.every(p => p.esperado.classList.contains("coincide"));
    registro.veredicto.hidden = false;
    registro.veredicto.className = "veredicto " + (bien ? "ok" : "mal");
    registro.veredicto.replaceChildren(icono(bien ? "check" : "alertas"),
      document.createTextNode(bien ? "El sistema respondió como se esperaba." : "El resultado no coincide con lo esperado; revisa los pasos marcados."));
  }

  function predecir(saldo, monto, envios, divisa) {
    const resultados = [];
    let disponible = saldo;
    let completados = 0;
    for (let i = 0; i < envios; i++) {
      let r;
      if (divisa !== "USD") r = { estado: "denegada", motivo: "CURRENCY_MISMATCH" };
      else if (monto > REGLAS.limitePorOperacion) r = { estado: "denegada", motivo: "AMOUNT_LIMIT_EXCEEDED" };
      else if (monto > disponible + 1e-9) r = { estado: "denegada", motivo: "INSUFFICIENT_FUNDS" };
      else {
        disponible -= monto;
        completados++;
        if (monto > REGLAS.umbralAltoValor) r = { estado: "sospechosa", motivo: "HIGH_VALUE_TRANSACTION" };
        else if (completados > REGLAS.maxEnviosVentana) r = { estado: "sospechosa", motivo: "ANOMALOUS_FREQUENCY" };
        else r = { estado: "completada" };
      }
      resultados.push(r);
    }
    return resultados;
  }

  function leerFormularioSim() {
    const f = $("form-sim");
    return {
      saldo: Math.max(0, Number(f.saldo.value) || 0),
      monto: Number(f.monto.value) || 0,
      envios: Math.min(10, Math.max(1, Math.round(Number(f.envios.value) || 1))),
      divisa: f.divisa.value
    };
  }

  function pintarPrediccion() {
    const d = leerFormularioSim();
    const p = $("prediccion");
    if (d.monto <= 0) { p.textContent = "Escribe un monto mayor que cero."; return; }
    const r = predecir(d.saldo, d.monto, d.envios, d.divisa);
    const cuenta = clave => {
      const n = r.filter(x => x.estado === clave).length;
      return n + " " + clave + (n === 1 ? "" : "s");
    };
    const motivos = [...new Set(r.filter(x => x.motivo).map(x => MOTIVOS[x.motivo].toLowerCase()))];
    p.replaceChildren("Según las reglas del banco se espera: ",
      el("b", "", cuenta("completada") + ", " + cuenta("denegada") + " y " + cuenta("sospechosa")),
      motivos.length ? " (" + motivos.join(", ") + ")." : ".");
  }

  function conectar() {
    const ws = new WebSocket(WS_URL);
    ws.onopen = () => { $("conexion").classList.add("on"); $("conexion-texto").textContent = "En vivo"; };
    ws.onclose = () => {
      $("conexion").classList.remove("on");
      $("conexion-texto").textContent = "Reconectando…";
      setTimeout(conectar, 2000);
    };
    ws.onmessage = evento => {
      const datos = JSON.parse(evento.data);
      if (Array.isArray(datos.accounts)) {
        estado.cuentas.clear();
        datos.accounts.forEach(c => estado.cuentas.set(c.user.toLowerCase(), c));
      }
      if (datos.type === "SNAPSHOT") {
        estado.movimientos.clear();
        (datos.transactions || []).forEach(m => {
          estado.movimientos.set(m.tx_id, m);

          if (esDenegado(m)) estado.alertadas.add(m.tx_id + ":den");
          if (m.suspicious) estado.alertadas.add(m.tx_id + ":sos");
        });
        renderizar();
      }
      if (datos.type === "UPSERT" && datos.transaction) upsert(datos.transaction);
      else renderizar();
    };
  }

  function upsert(m) {
    const descripcion = estado.pendientes.get(m.tx_id);
    if (descripcion && !esDenegado(m)) {
      estado.pendientes.delete(m.tx_id);
      toast("Listo: " + descripcion, "ok");
    } else if (descripcion) {
      estado.pendientes.delete(m.tx_id);
    }
    if (esDenegado(m) || m.suspicious) alertaEnPantalla(m);
    estado.movimientos.set(m.tx_id, m);
    actualizarPaso(m);
    renderizar(m.tx_id);
  }

  async function post(ruta, cuerpo, descripcion, conSesion = true) {
    const headers = { "Content-Type": "application/json" };
    if (conSesion) headers.Authorization = "Bearer " + SESION.token;
    const respuesta = await fetch(API + ruta, { method: "POST", headers, body: JSON.stringify(cuerpo) });
    const json = await respuesta.json().catch(() => ({}));
    if (respuesta.status === 401 && conSesion) {
      sesionVencida();
      throw new Error("Tu sesión venció");
    }
    if (!respuesta.ok) throw new Error(json.message || ("Error " + respuesta.status));
    if (descripcion) estado.pendientes.set(json.tx_id, descripcion);
    return json;
  }

  async function ejecutar(boton, accion) {
    boton.disabled = true;
    try {
      await accion();
    } catch (e) {
      toast(e.message.startsWith("Failed") ? "No se pudo contactar con el banco (" + API + ")" : e.message, "danger");
    } finally {
      boton.disabled = false;
    }
  }

  async function cerrarSesion() {
    $("salir").disabled = true;
    try {
      await fetch(API + "/auth/logout", { method: "POST", headers: { Authorization: "Bearer " + SESION.token } });
    } catch (e) {  }
    terminarSesion("Cerraste tu sesión.", "info");
  }

  async function verificarSesion() {
    try {
      const r = await fetch(API + "/auth/me", { headers: { Authorization: "Bearer " + SESION.token } });
      if (r.status === 401) sesionVencida();
    } catch (e) {  }
    setTimeout(sesionVencida, Math.max(0, new Date(SESION.expires_at) - new Date()));
  }

  document.querySelectorAll("svg[data-icono]").forEach(n => {
    n.setAttribute("class", "icono-svg");
    if (n.dataset.icono !== "tema") dibujarIcono(n, n.dataset.icono);
  });
  pintarControlesTema();
  document.querySelectorAll("[data-accion=tema]").forEach(b => b.addEventListener("click", cambiarTema));
  $("boton-menu").addEventListener("click", alternarMenu);
  $("velo").addEventListener("click", cerrarMenuMovil);
  document.addEventListener("keydown", e => { if (e.key === "Escape") cerrarMenuMovil(); });
  $("salir").addEventListener("click", cerrarSesion);

  document.querySelectorAll("[data-ir]").forEach(b => b.addEventListener("click", () => {
    if (b.dataset.pestana) pestanaPendiente = b.dataset.pestana;
    if (location.hash === "#" + b.dataset.ir) irA(b.dataset.ir);
    else location.hash = "#" + b.dataset.ir;
  }));
  document.querySelectorAll("a[data-pestana]").forEach(a => a.addEventListener("click", () => {
    pestanaPendiente = a.dataset.pestana;
    if (location.hash === a.getAttribute("href")) irA("mover");
  }));
  window.addEventListener("hashchange", () => irA(location.hash.slice(1)));

  $("tab-enviar").addEventListener("click", () => cambiarPestana("enviar"));
  $("tab-recargar").addEventListener("click", () => cambiarPestana("recargar"));
  $("form-enviar").amount.addEventListener("input", validarMontoEnvio);
  $("form-enviar").addEventListener("submit", e => {
    e.preventDefault();
    const f = e.target;
    const cuerpo = { from_user: estado.usuario, to_user: f.to_user.value.trim(), amount: Number(f.amount.value), currency: f.currency.value };
    ejecutar(f.querySelector("button[type=submit]"), async () => {
      await post("/transfer", cuerpo, "Envío de " + dinero(cuerpo.amount, cuerpo.currency) + " a " + cuerpo.to_user);
      toast("Envío en proceso…");
      f.amount.value = "";
      validarMontoEnvio();
    });
  });
  $("form-recargar").addEventListener("submit", e => {
    e.preventDefault();
    const f = e.target;
    const cuerpo = { user: estado.usuario, amount: Number(f.amount.value), currency: f.currency.value };
    ejecutar(f.querySelector("button[type=submit]"), async () => {
      await post("/deposit", cuerpo, "Recarga de " + dinero(cuerpo.amount, cuerpo.currency));
      toast("Recarga en proceso…");
      f.amount.value = "";
    });
  });
  document.querySelectorAll(".chips button").forEach(b => b.addEventListener("click", () => {
    const campo = $("form-recargar").amount;
    campo.value = Number(campo.value || 0) + Number(b.dataset.monto);
  }));

  document.querySelectorAll(".filtros button").forEach(b => b.addEventListener("click", () => aplicarFiltro(b.dataset.filtro)));
  $("buscar").addEventListener("input", e => { estado.busqueda = e.target.value.trim().toLowerCase(); renderizar(); });
  $("solo-mios").addEventListener("change", e => { estado.soloMios = e.target.checked; renderizar(); });
  $("alertas-mias").addEventListener("change", e => { estado.alertasMias = e.target.checked; renderizar(); });

  pintarEscenarios();
  pintarPrediccion();
  $("form-sim").addEventListener("input", pintarPrediccion);
  $("form-sim").addEventListener("submit", e => {
    e.preventDefault();
    const d = leerFormularioSim();
    if (d.monto <= 0) return;
    const u = usuariosSimulados();
    const prediccion = predecir(d.saldo, d.monto, d.envios, d.divisa);
    const pasos = [];
    if (d.saldo > 0) pasos.push(recarga(u.origen, d.saldo, "Recarga inicial de " + dinero(d.saldo, "USD")));
    prediccion.forEach((r, i) => pasos.push(transferencia(u.origen, u.destino, d.monto, d.divisa,
      "Envío " + (i + 1) + " de " + d.envios + " (" + dinero(d.monto, d.divisa) + ")", r)));
    ejecutarCorrida(e.target.querySelector("button[type=submit]"), "Simulación personalizada", pasos, u.origen);
  });

  if (window.ResizeObserver) {
    let anchoPrevio = 0;
    new ResizeObserver(([entrada]) => {
      const ancho = Math.round(entrada.contentRect.width);
      if (ancho !== anchoPrevio && estado.vista === "analisis") { anchoPrevio = ancho; renderizar(); }
    }).observe($("grafico-volumen"));
  }
  matchMedia("(prefers-color-scheme: dark)").addEventListener?.("change", () => { pintarControlesTema(); renderizar(); });

  $("perfil-usuario").textContent = "@" + SESION.username;
  $("perfil-nombre").textContent = SESION.full_name || "Cliente";
  irA(location.hash.slice(1) || "inicio");
  window.Guilloche.montar(document.querySelector("#saldo canvas"), {
    geometria: (ancho, alto) => ({ x: ancho * 0.95, y: alto * 0.08, escala: Math.min(ancho, alto) * 0.55 }),
    duracion: 1100
  });
  verificarSesion();
  conectar();
})();
