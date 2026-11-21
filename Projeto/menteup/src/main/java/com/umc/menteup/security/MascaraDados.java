package com.umc.menteup.security;

// Mostra só uma parte de um dado pessoal nas telas em que o dado completo não é necessário
public final class MascaraDados {

    private MascaraDados() {
    }

    // "nikolas@hotmail.com" vira "ni***@hotmail.com": dá para diferenciar pessoas, sem expor o endereço
    public static String email(String email) {
        int arroba = email == null ? -1 : email.indexOf('@');

        if (arroba <= 0) {
            return "***";
        }

        String visivel = email.substring(0, Math.min(2, arroba));
        return visivel + "***" + email.substring(arroba);
    }
}
