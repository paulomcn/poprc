package com.poprc.demo.security;

public final class CpfUtils {
    private CpfUtils() { }

    public static String normalizar(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        return cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String numero = normalizar(cpf);
        if (numero == null || numero.length() != 11 || numero.chars().distinct().count() == 1) return false;
        return digito(numero, 9) == numero.charAt(9) - '0'
                && digito(numero, 10) == numero.charAt(10) - '0';
    }

    private static int digito(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = 11 - (soma % 11);
        return resto >= 10 ? 0 : resto;
    }
}
