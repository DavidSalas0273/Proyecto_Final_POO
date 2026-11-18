package com.banco.ledger.api.domain.usuario;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad rica: cliente de la banca en línea.
 * <ul>
 *   <li>Nunca guarda la contraseña en texto plano, solo su hash.</li>
 *   <li>Protege la invariante de seguridad: tras {@link #MAX_INTENTOS} intentos fallidos
 *       el acceso queda bloqueado durante {@link #DURACION_BLOQUEO}.</li>
 *   <li>Sin setters: el estado cambia solo con {@link #autenticar}.</li>
 * </ul>
 */
public class Usuario {

    public static final int MAX_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final String usuario;
    private final String nombreCompleto;
    private final String documento;
    private final String email;
    private final String hashContrasena;
    private final Instant creadoEn;
    private int intentosFallidos;
    private Instant bloqueadoHasta;

    private Usuario(String usuario, String nombreCompleto, String documento, String email, String hashContrasena,
                    Instant creadoEn, int intentosFallidos, Instant bloqueadoHasta) {
        this.usuario = Objects.requireNonNull(usuario);
        this.nombreCompleto = Objects.requireNonNull(nombreCompleto);
        this.documento = Objects.requireNonNull(documento);
        this.email = Objects.requireNonNull(email);
        this.hashContrasena = Objects.requireNonNull(hashContrasena, "El hash de la contraseña es obligatorio");
        this.creadoEn = Objects.requireNonNull(creadoEn);
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** Abre una cuenta nueva. Recibe la contraseña ya cifrada por el puerto correspondiente. */
    public static Usuario registrar(DatosRegistro datos, String hashContrasena, Instant ahora) {
        return new Usuario(datos.usuario(), datos.nombreCompleto(), datos.documento(), datos.email(),
                hashContrasena, ahora, 0, null);
    }

    /** Reconstruye un usuario existente desde la persistencia. */
    public static Usuario reconstituir(String usuario, String nombreCompleto, String documento, String email,
                                       String hashContrasena, Instant creadoEn, int intentosFallidos,
                                       Instant bloqueadoHasta) {
        return new Usuario(usuario, nombreCompleto, documento, email, hashContrasena, creadoEn, intentosFallidos,
                bloqueadoHasta);
    }

    /**
     * Verifica la contraseña aplicando la política de bloqueo.
     *
     * @throws CuentaBloqueadaException     si el acceso está bloqueado en este momento
     * @throws CredencialesInvalidasException si la contraseña no coincide (y suma un intento fallido)
     */
    public void autenticar(String contrasena, VerificadorContrasena verificador, Instant ahora) {
        bloqueoVigente(ahora).ifPresent(hasta -> {
            throw new CuentaBloqueadaException(hasta);
        });
        if (!verificador.coincide(contrasena, hashContrasena)) {
            registrarIntentoFallido(ahora);
            throw new CredencialesInvalidasException();
        }
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public Optional<Instant> bloqueoVigente(Instant ahora) {
        return Optional.ofNullable(bloqueadoHasta).filter(hasta -> hasta.isAfter(ahora));
    }

    private void registrarIntentoFallido(Instant ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
            intentosFallidos = 0;
        }
    }

    public String usuario() {
        return usuario;
    }

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public String documento() {
        return documento;
    }

    public String email() {
        return email;
    }

    public String hashContrasena() {
        return hashContrasena;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Optional<Instant> bloqueadoHasta() {
        return Optional.ofNullable(bloqueadoHasta);
    }

    /** Puerto mínimo que el dominio necesita para comparar contraseñas sin conocer el algoritmo. */
    @FunctionalInterface
    public interface VerificadorContrasena {
        boolean coincide(String contrasenaPlana, String hash);
    }
}
