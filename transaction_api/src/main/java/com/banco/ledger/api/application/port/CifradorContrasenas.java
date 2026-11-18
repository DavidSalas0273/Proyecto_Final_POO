package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.usuario.Usuario;

/** Puerto: cifra y verifica contraseñas sin que el dominio conozca el algoritmo (BCrypt). */
public interface CifradorContrasenas extends Usuario.VerificadorContrasena {

    String cifrar(String contrasenaPlana);
}
