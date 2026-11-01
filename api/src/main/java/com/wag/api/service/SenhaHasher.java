package com.wag.api.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Gera e valida hashes de senha usando PBKDF2, salt aleatório e comparação segura. */
final class SenhaHasher {

    // Os parâmetros acompanham o formato armazenado para permitir validar hashes existentes.
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaHasher() {
    }

    /** Cria um hash com salt exclusivo para que senhas iguais não gerem valores iguais. */
    static String hash(String senha) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(senha, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Compara uma senha informada com o hash armazenado sem comparar bytes manualmente. */
    static boolean corresponde(String senha, String hashArmazenado) {
        if (senha == null || hashArmazenado == null) {
            return false;
        }

        String[] partes = hashArmazenado.split("\\$", -1);
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }

        // O limite superior evita que um valor armazenado inválido provoque trabalho excessivo.
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 1 || iteracoes > 1_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[3]);
            byte[] hashInformado = derive(senha, salt, iteracoes);
            return MessageDigest.isEqual(hashEsperado, hashInformado);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Deriva a chave da senha com os parâmetros PBKDF2 especificados. */
    private static byte[] derive(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível processar a senha.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
