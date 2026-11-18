package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.usuario.Usuario;

public interface CifradorContrasenas extends Usuario.VerificadorContrasena {
    String cifrar(String contrasenaPlana);
}
