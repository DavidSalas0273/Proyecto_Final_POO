package com.banco.ledger.api.security;

import com.banco.ledger.api.model.Usuario;

public interface CifradorContrasenas extends Usuario.VerificadorContrasena {
    String cifrar(String contrasenaPlana);
}
