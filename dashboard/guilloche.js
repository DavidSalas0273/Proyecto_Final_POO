window.Guilloche = (function () {
  "use strict";

  const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const CURVAS = [
    { R: 1.00, r: 0.115, d: 0.36, alfa: 0.55, tono: "acento" },
    { R: 0.92, r: 0.095, d: 0.30, alfa: 0.35, tono: "claro" },
    { R: 0.70, r: 0.075, d: 0.25, alfa: 0.45, tono: "acento" },
    { R: 0.52, r: 0.130, d: 0.20, alfa: 0.25, tono: "claro" }
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

  function montar(canvas, opciones) {
    if (!canvas || !canvas.getContext) return;
    const ctx = canvas.getContext("2d");
    const acento = getComputedStyle(document.documentElement).getPropertyValue("--accent").trim() || "#1e7a5c";
    const colores = { acento, claro: "#f2f5f2" };
    const opacidad = opciones.opacidad ?? 1;
    let trazos = [];
    let centro = [0, 0];

    function preparar() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const { width, height } = canvas.getBoundingClientRect();
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const g = opciones.geometria(width, height);
      centro = [g.x, g.y];
      trazos = CURVAS.map(c => ({ c, p: puntos(c, g.escala) }));
    }

    function dibujar(progreso) {
      const { width, height } = canvas.getBoundingClientRect();
      ctx.clearRect(0, 0, width, height);
      ctx.lineWidth = 0.7;
      trazos.forEach(({ c, p }) => {
        const hasta = Math.max(2, Math.floor(p.length * progreso));
        ctx.globalAlpha = c.alfa * opacidad;
        ctx.strokeStyle = colores[c.tono];
        ctx.beginPath();
        ctx.moveTo(centro[0] + p[0][0], centro[1] + p[0][1]);
        for (let i = 1; i < hasta; i++) ctx.lineTo(centro[0] + p[i][0], centro[1] + p[i][1]);
        ctx.stroke();
      });
      ctx.globalAlpha = 1;
    }

    preparar();
    const duracion = opciones.duracion ?? 1400;
    if (reducirMovimiento || duracion <= 0) {
      dibujar(1);
    } else {
      const inicio = performance.now();
      const easeOut = t => 1 - Math.pow(1 - t, 4);
      (function cuadro(ahora) {
        const t = Math.min(1, (ahora - inicio) / duracion);
        dibujar(easeOut(t));
        if (t < 1) requestAnimationFrame(cuadro);
      })(inicio);
    }

    let pendiente;
    const redibujar = () => {
      cancelAnimationFrame(pendiente);
      pendiente = requestAnimationFrame(() => { preparar(); dibujar(1); });
    };
    if (window.ResizeObserver) {
      new ResizeObserver(redibujar).observe(canvas);
    } else {
      window.addEventListener("resize", redibujar);
    }
  }

  return { montar };
})();
