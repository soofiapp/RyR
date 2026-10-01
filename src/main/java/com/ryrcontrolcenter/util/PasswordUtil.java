package com.ryrcontrolcenter.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

    private static final int ITERACIONES = 600000;
    private static final int TAMAÑO_SALT = 16;
    private static final int TAMAÑO_HASH = 256;

    public static String generarHash(String password) {
        try {
            byte[] salt = new byte[TAMAÑO_SALT];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERACIONES, TAMAÑO_HASH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return ITERACIONES + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al generar el hash de la contraseña", e);
        }
    }

    public static boolean verificarPassword(String password, String hashGuardado) {
        try {
            String[] partes = hashGuardado.split(":");
            int iteraciones = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] hashOriginal = Base64.getDecoder().decode(partes[2]);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteraciones, hashOriginal.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashNuevo = factory.generateSecret(spec).getEncoded();
            if (hashNuevo.length != hashOriginal.length) {
                return false;
            }
            int resultado = 0;
            for (int i = 0; i < hashNuevo.length; i++) {
                resultado |= hashNuevo[i] ^ hashOriginal[i];
            }
            return resultado == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
