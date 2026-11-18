package com.banco.ledger.api.infrastructure.persistence;

import com.banco.ledger.api.domain.usuario.Sesion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Immutable
@Table(name = "sesiones")
public class SesionJpaEntity {
    @Id
    @Column(name = "token", length = 64)
    private String token;

    @Column(name = "usuario", nullable = false, length = 20)
    private String usuario;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    protected SesionJpaEntity() {
    }

    private SesionJpaEntity(Sesion sesion) {
        this.token = sesion.token();
        this.usuario = sesion.usuario();
        this.nombreCompleto = sesion.nombreCompleto();
        this.expiraEn = sesion.expiraEn();
    }

    public static SesionJpaEntity desdeDominio(Sesion sesion) {
        return new SesionJpaEntity(sesion);
    }

    public Sesion aDominio() {
        return new Sesion(token, usuario, nombreCompleto, expiraEn);
    }
}
