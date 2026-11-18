package com.banco.ledger.api.application;

import com.banco.ledger.api.application.port.CifradorContrasenas;
import com.banco.ledger.api.application.port.GeneradorTokens;
import com.banco.ledger.api.application.port.RepositorioSesiones;
import com.banco.ledger.api.application.port.RepositorioUsuarios;
import com.banco.ledger.api.domain.usuario.CredencialesInvalidasException;
import com.banco.ledger.api.domain.usuario.DatosRegistro;
import com.banco.ledger.api.domain.usuario.PoliticaSesion;
import com.banco.ledger.api.domain.usuario.Sesion;
import com.banco.ledger.api.domain.usuario.SesionInvalidaException;
import com.banco.ledger.api.domain.usuario.Usuario;
import com.banco.ledger.api.domain.usuario.UsuarioYaExisteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTest {
    private static final Instant AHORA = Instant.parse("2026-11-17T09:00:00Z");
    private static final String TOKEN = "t".repeat(43);

    @Mock
    private RepositorioUsuarios usuarios;
    @Mock
    private RepositorioSesiones sesiones;
    @Mock
    private CifradorContrasenas cifrador;
    @Mock
    private GeneradorTokens tokens;
    @Mock
    private Clock clock;
    @Spy
    private PoliticaSesion politica = new PoliticaSesion(Duration.ofMinutes(30));
    @InjectMocks
    private ServicioAutenticacion servicio;

    private final DatosRegistro datos =
            new DatosRegistro("Ana Pérez", "1020345678", "ana@correo.com", "ana", "clave2026");

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(AHORA);
        lenient().when(tokens.nuevoToken()).thenReturn(TOKEN);
    }

    @Test
    void registroGuardaElHashYAbreUnaSesion() {
        when(cifrador.cifrar("clave2026")).thenReturn("$2a$10$hash");

        Sesion sesion = servicio.registrar(datos);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(1)).guardar(guardado.capture());
        assertThat(guardado.getValue().hashContrasena()).isEqualTo("$2a$10$hash");
        verify(sesiones, times(1)).guardar(sesion);
        assertThat(sesion.usuario()).isEqualTo("ana");
        assertThat(sesion.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(30)));
    }

    @Test
    void registroDuplicadoNoGuardaNada() {
        when(usuarios.existeUsuario("ana")).thenReturn(false);
        when(usuarios.existeEmail("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(datos))
                .isInstanceOf(UsuarioYaExisteException.class)
                .extracting(e -> ((UsuarioYaExisteException) e).campo()).isEqualTo("email");
        verify(usuarios, never()).guardar(any());
        verify(sesiones, never()).guardar(any());
    }

    @Test
    void loginCorrectoAbreSesion() {
        Usuario ana = Usuario.registrar(datos, "$2a$10$hash", AHORA);
        when(usuarios.buscarPorUsuarioOEmail("ana")).thenReturn(Optional.of(ana));
        when(cifrador.coincide("clave2026", "$2a$10$hash")).thenReturn(true);

        Sesion sesion = servicio.iniciarSesion(" ana ", "clave2026");

        assertThat(sesion.token()).isEqualTo(TOKEN);
        verify(sesiones, times(1)).guardar(sesion);
    }

    @Test
    void loginFallidoGuardaElIntentoYNoAbreSesion() {
        Usuario ana = Usuario.registrar(datos, "$2a$10$hash", AHORA);
        when(usuarios.buscarPorUsuarioOEmail("ana")).thenReturn(Optional.of(ana));
        when(cifrador.coincide(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> servicio.iniciarSesion("ana", "otra1234"))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(usuarios, times(1)).guardar(ana);
        assertThat(ana.intentosFallidos()).isEqualTo(1);
        verify(sesiones, never()).guardar(any());
    }

    @Test
    void usuarioInexistenteDaElMismoErrorGenerico() {
        when(usuarios.buscarPorUsuarioOEmail("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion("nadie", "clave2026"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void sesionVencidaSeBorraYNoAutoriza() {
        Sesion vencida = new Sesion(TOKEN, "ana", "Ana Pérez", AHORA.minusSeconds(1));
        when(sesiones.buscar(TOKEN)).thenReturn(Optional.of(vencida));

        assertThatThrownBy(() -> servicio.titularDe(TOKEN)).isInstanceOf(SesionInvalidaException.class);
        verify(sesiones, times(1)).eliminar(TOKEN);
    }

    @Test
    void sesionVigenteDevuelveElTitular() {
        when(sesiones.buscar(TOKEN)).thenReturn(Optional.of(new Sesion(TOKEN, "ana", "Ana Pérez",
                AHORA.plusSeconds(60))));

        assertThat(servicio.titularDe(TOKEN)).isEqualTo("ana");
        verify(sesiones, never()).eliminar(anyString());
    }
}
