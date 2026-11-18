package com.banco.ledger.api.application;

import com.banco.ledger.api.application.port.CifradorContrasenas;
import com.banco.ledger.api.application.port.GeneradorTokens;
import com.banco.ledger.api.application.port.RepositorioSesiones;
import com.banco.ledger.api.application.port.RepositorioUsuarios;
import com.banco.ledger.api.domain.usuario.CredencialesInvalidasException;
import com.banco.ledger.api.domain.usuario.CuentaBloqueadaException;
import com.banco.ledger.api.domain.usuario.DatosRegistro;
import com.banco.ledger.api.domain.usuario.PoliticaSesion;
import com.banco.ledger.api.domain.usuario.Sesion;
import com.banco.ledger.api.domain.usuario.SesionInvalidaException;
import com.banco.ledger.api.domain.usuario.Usuario;
import com.banco.ledger.api.domain.usuario.UsuarioYaExisteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ServicioAutenticacion {
    private final RepositorioUsuarios usuarios;
    private final RepositorioSesiones sesiones;
    private final CifradorContrasenas cifrador;
    private final GeneradorTokens tokens;
    private final PoliticaSesion politica;
    private final Clock clock;

    public ServicioAutenticacion(RepositorioUsuarios usuarios, RepositorioSesiones sesiones,
                                 CifradorContrasenas cifrador, GeneradorTokens tokens, PoliticaSesion politica,
                                 Clock clock) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.cifrador = cifrador;
        this.tokens = tokens;
        this.politica = politica;
        this.clock = clock;
    }

    @Transactional
    public Sesion registrar(DatosRegistro datos) {
        exigirDisponible(!usuarios.existeUsuario(datos.usuario()), "username",
                "Ese usuario ya está en uso. Elige otro.");
        exigirDisponible(!usuarios.existeEmail(datos.email()), "email",
                "Ya existe una cuenta con ese correo.");
        exigirDisponible(!usuarios.existeDocumento(datos.documento()), "document",
                "Ya existe una cuenta con ese número de documento.");

        Usuario usuario = Usuario.registrar(datos, cifrador.cifrar(datos.contrasena()), clock.instant());
        usuarios.guardar(usuario);
        return abrirSesion(usuario);
    }

    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaException.class})
    public Sesion iniciarSesion(String identificador, String contrasena) {
        Usuario usuario = Optional.ofNullable(identificador)
                .map(String::trim)
                .flatMap(usuarios::buscarPorUsuarioOEmail)
                .orElseThrow(CredencialesInvalidasException::new);
        try {
            usuario.autenticar(contrasena, cifrador, clock.instant());
        } finally {
            usuarios.guardar(usuario);
        }
        return abrirSesion(usuario);
    }

    @Transactional
    public Optional<Sesion> sesionVigente(String token) {
        Instant ahora = clock.instant();
        Optional<Sesion> sesion = Optional.ofNullable(token).flatMap(sesiones::buscar);
        sesion.filter(s -> !s.estaVigente(ahora)).ifPresent(s -> sesiones.eliminar(s.token()));
        return sesion.filter(s -> s.estaVigente(ahora));
    }

    public String titularDe(String token) {
        return sesionVigente(token).map(Sesion::usuario).orElseThrow(SesionInvalidaException::new);
    }

    @Transactional
    public void cerrarSesion(String token) {
        Optional.ofNullable(token).ifPresent(sesiones::eliminar);
    }

    private Sesion abrirSesion(Usuario usuario) {
        Sesion sesion = new Sesion(tokens.nuevoToken(), usuario.usuario(), usuario.nombreCompleto(),
                clock.instant().plus(politica.duracion()));
        sesiones.guardar(sesion);
        return sesion;
    }

    private static void exigirDisponible(boolean disponible, String campo, String mensaje) {
        if (!disponible) {
            throw new UsuarioYaExisteException(campo, mensaje);
        }
    }
}
