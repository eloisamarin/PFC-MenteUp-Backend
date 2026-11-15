package com.umc.menteup.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.regex.Pattern;

// Descobre o IP de quem fez o pedido
// No Render, todo acesso passa pela Cloudflare, que informa o IP real no cabeçalho CF-Connecting-IP
// e troca qualquer valor que o próprio usuário tenha mandado nele
// Sem cabeçalho configurado (ou sem valor válido), usa o IP da conexão
@Component
public class IpDoCliente {

    // Só números, letras de A a F, pontos e dois-pontos (IPv4 ou IPv6), com até 45 caracteres
    private static final Pattern FORMATO_IP = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    private final List<String> cabecalhos;

    public IpDoCliente(@Value("${menteup.ip.cabecalhos:}") List<String> cabecalhos) {
        this.cabecalhos = cabecalhos.stream()
                .map(String::trim)
                .filter(nome -> !nome.isEmpty())
                .toList();
    }

    public String de(HttpServletRequest request) {
        for (String cabecalho : cabecalhos) {
            String valor = request.getHeader(cabecalho);
            if (pareceIp(valor)) {
                return valor.trim();
            }
        }
        return request.getRemoteAddr();
    }

    // Para quem não recebe o pedido diretamente (como o registro de logs); fora de um pedido devolve null
    public String daRequisicaoAtual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return null;
        }
        return de(atributos.getRequest());
    }

    static boolean pareceIp(String valor) {
        if (valor == null) {
            return false;
        }
        String limpo = valor.trim();
        return FORMATO_IP.matcher(limpo).matches() && (limpo.contains(".") || limpo.contains(":"));
    }
}
