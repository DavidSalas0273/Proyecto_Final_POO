package com.banco.ledger.api.infrastructure.seguridad;

import com.banco.ledger.api.application.port.CifradorContrasenas;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador BCrypt: cada hash lleva su propia sal y un costo de 10 rondas, así que dos
 * usuarios con la misma contraseña tienen hashes distintos y un ataque de fuerza bruta es lento.
 */
@Component
public class BCryptCifradorContrasenas implements CifradorContrasenas {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String cifrar(String contrasenaPlana) {
        return encoder.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String hash) {
        return contrasenaPlana != null && encoder.matches(contrasenaPlana, hash);
    }
}
