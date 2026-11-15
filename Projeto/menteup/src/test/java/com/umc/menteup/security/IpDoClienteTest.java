package com.umc.menteup.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class IpDoClienteTest {

    private static MockHttpServletRequest pedido(String ipDaConexao) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ipDaConexao);
        return request;
    }

    @Test
    void comCabecalhoDaCloudflare_usaOIpRealDoUsuario() {
        IpDoCliente ip = new IpDoCliente(List.of("CF-Connecting-IP"));
        MockHttpServletRequest request = pedido("172.71.151.82");
        request.addHeader("CF-Connecting-IP", "177.45.12.149");

        assertThat(ip.de(request)).isEqualTo("177.45.12.149");
    }

    @Test
    void aceitaIpv6NoCabecalho() {
        IpDoCliente ip = new IpDoCliente(List.of("CF-Connecting-IP"));
        MockHttpServletRequest request = pedido("172.71.151.82");
        request.addHeader("CF-Connecting-IP", "2804:14c:5b:8000::1");

        assertThat(ip.de(request)).isEqualTo("2804:14c:5b:8000::1");
    }

    @Test
    void semOCabecalho_usaOIpDaConexao() {
        IpDoCliente ip = new IpDoCliente(List.of("CF-Connecting-IP"));

        assertThat(ip.de(pedido("10.0.0.7"))).isEqualTo("10.0.0.7");
    }

    @Test
    void valorQueNaoEIp_eIgnorado() {
        IpDoCliente ip = new IpDoCliente(List.of("CF-Connecting-IP"));
        MockHttpServletRequest request = pedido("10.0.0.7");
        request.addHeader("CF-Connecting-IP", "1.2.3.4, 5.6.7.8<script>");

        assertThat(ip.de(request)).isEqualTo("10.0.0.7");
    }

    @Test
    void semCabecalhoConfigurado_naoLeNenhumCabecalho() {
        IpDoCliente ip = new IpDoCliente(List.of(""));
        MockHttpServletRequest request = pedido("10.0.0.7");
        request.addHeader("CF-Connecting-IP", "177.45.12.149");

        assertThat(ip.de(request)).isEqualTo("10.0.0.7");
    }
}
