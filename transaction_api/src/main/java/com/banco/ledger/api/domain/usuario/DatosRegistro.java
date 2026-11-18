package com.banco.ledger.api.domain.usuario;

import java.util.Locale;
import java.util.regex.Pattern;

public record DatosRegistro(String nombreCompleto, String documento, String email, String usuario,
                            String contrasena) {
    private static final Pattern DOCUMENTO = Pattern.compile("\\d{6,12}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern USUARIO = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final Pattern TIENE_LETRA = Pattern.compile(".*\\p{L}.*");
    private static final Pattern TIENE_NUMERO = Pattern.compile(".*\\d.*");
    public static final int LONGITUD_MINIMA_CONTRASENA = 8;

    public DatosRegistro {
        nombreCompleto = requerir(nombreCompleto, "nombre completo").replaceAll("\\s+", " ");
        documento = requerir(documento, "número de documento").replaceAll("[.\\s-]", "");
        email = requerir(email, "correo").toLowerCase(Locale.ROOT);
        usuario = requerir(usuario, "usuario");
        if (contrasena == null) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (nombreCompleto.length() < 3) {
            throw new IllegalArgumentException("Escribe tu nombre completo");
        }
        if (!DOCUMENTO.matcher(documento).matches()) {
            throw new IllegalArgumentException("El número de documento debe tener entre 6 y 12 dígitos");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato válido");
        }
        if (!USUARIO.matcher(usuario).matches()) {
            throw new IllegalArgumentException(
                    "El usuario debe tener de 3 a 20 caracteres: letras, números o guion bajo");
        }
        if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA
                || !TIENE_LETRA.matcher(contrasena).matches()
                || !TIENE_NUMERO.matcher(contrasena).matches()) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, con letras y números");
        }
    }

    @Override
    public String toString() {
        return "DatosRegistro[usuario=" + usuario + ", email=" + email + "]";
    }

    private static String requerir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
