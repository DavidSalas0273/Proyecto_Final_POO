package com.banco.ledger.api.infrastructure.persistence;

import com.banco.ledger.api.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabla usuarios. Modelo de persistencia separado de la entidad de dominio Usuario. */
@Entity
@Table(name = "usuarios")
public class UsuarioJpaEntity {

    @Id
    @Column(name = "usuario", length = 20)
    private String usuario;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "documento", nullable = false, unique = true, length = 12)
    private String documento;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    private String hashContrasena;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected UsuarioJpaEntity() {
        // requerido por JPA
    }

    private UsuarioJpaEntity(Usuario u) {
        this.usuario = u.usuario();
        this.nombreCompleto = u.nombreCompleto();
        this.documento = u.documento();
        this.email = u.email();
        this.hashContrasena = u.hashContrasena();
        this.creadoEn = u.creadoEn();
        sincronizarCon(u);
    }

    public static UsuarioJpaEntity desdeDominio(Usuario usuario) {
        return new UsuarioJpaEntity(usuario);
    }

    /** Solo el estado de seguridad cambia después del registro. */
    public void sincronizarCon(Usuario u) {
        this.intentosFallidos = u.intentosFallidos();
        this.bloqueadoHasta = u.bloqueadoHasta().orElse(null);
    }

    public Usuario aDominio() {
        return Usuario.reconstituir(usuario, nombreCompleto, documento, email, hashContrasena, creadoEn,
                intentosFallidos, bloqueadoHasta);
    }
}
