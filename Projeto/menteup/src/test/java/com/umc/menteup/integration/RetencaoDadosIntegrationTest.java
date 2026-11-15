package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.Codigo2FA;
import com.umc.menteup.model.LogSistema;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.model.StatusCadastro;
import com.umc.menteup.model.TokenRedefinicaoSenha;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.TokenRedefinicaoSenhaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.RetencaoDadosService;

// O que passou do prazo sai do banco, o que ainda está no prazo fica
class RetencaoDadosIntegrationTest extends IntegracaoBase {

    @Autowired
    private RetencaoDadosService retencaoDadosService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRedefinicaoSenhaRepository tokenRepository;

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void apagaCodigosELinksVencidosEMantemOsAtuais() {
        Usuario dono = salvarUsuario("ALUNO");
        LocalDateTime agora = LocalDateTime.now();

        String email = dono.getUsuario();
        Codigo2FA codigoVelho = codigo2FARepository.save(new Codigo2FA(email, "111111", agora.minusDays(3)));
        Codigo2FA codigoAtual = codigo2FARepository.save(new Codigo2FA(email, "222222", agora.plusMinutes(5)));

        TokenRedefinicaoSenha linkVelho = tokenRepository.save(
                new TokenRedefinicaoSenha(dono, hashAleatorio(), agora.minusDays(10)));
        TokenRedefinicaoSenha linkAtual = tokenRepository.save(
                new TokenRedefinicaoSenha(dono, hashAleatorio(), agora.plusHours(1)));

        retencaoDadosService.executar();

        assertThat(codigo2FARepository.findById(codigoVelho.getId())).isEmpty();
        assertThat(codigo2FARepository.findById(codigoAtual.getId())).isPresent();
        assertThat(tokenRepository.findById(linkVelho.getId())).isEmpty();
        assertThat(tokenRepository.findById(linkAtual.getId())).isPresent();
    }

    @Test
    void anonimizaPedidoDeProfessorRejeitadoHaMaisDeTrintaDias() {
        Usuario rejeitadoAntigo = salvarProfessor(StatusCadastro.REJEITADA, LocalDateTime.now().minusDays(31));
        Usuario rejeitadoRecente = salvarProfessor(StatusCadastro.REJEITADA, LocalDateTime.now().minusDays(5));
        Usuario pendenteAntigo = salvarProfessor(StatusCadastro.PENDENTE, LocalDateTime.now().minusDays(60));

        retencaoDadosService.executar();

        assertThat(recarregar(rejeitadoAntigo).getUsuario()).endsWith("@menteup.invalid");
        assertThat(recarregar(rejeitadoAntigo).getNomeUsuario()).isEqualTo("Usuário removido");
        assertThat(recarregar(rejeitadoRecente).getUsuario()).isEqualTo(rejeitadoRecente.getUsuario());
        assertThat(recarregar(pendenteAntigo).getUsuario()).isEqualTo(pendenteAntigo.getUsuario());
    }

    @Test
    void anonimizaConviteNuncaAceitoHaMaisDeTrintaDias() {
        Usuario conviteAntigo = salvarConvite(LocalDateTime.now().minusDays(31));
        Usuario conviteRecente = salvarConvite(LocalDateTime.now().minusDays(2));

        // Conta antiga, de antes desta mudança: sem data de criação, não é tocada
        Usuario conviteSemData = salvarConvite(LocalDateTime.now());
        jdbc.update("UPDATE tb_usuarios SET criado_em = NULL WHERE id = ?", conviteSemData.getId());

        retencaoDadosService.executar();

        Usuario antigo = recarregar(conviteAntigo);
        assertThat(antigo.getUsuario()).endsWith("@menteup.invalid");
        assertThat(antigo.isPrimeiroAcessoPendente()).isFalse();
        assertThat(recarregar(conviteRecente).getUsuario()).isEqualTo(conviteRecente.getUsuario());
        assertThat(recarregar(conviteSemData).getUsuario()).isEqualTo(conviteSemData.getUsuario());
    }

    @Test
    void apagaLogsComMaisDeSeisMesesERegistraODescarte() {
        Usuario dono = salvarUsuario("ALUNO");
        LogSistema velho = salvarLog(dono);
        LogSistema recente = salvarLog(dono);
        jdbc.update("UPDATE log_sistema SET data_hora = ? WHERE id = ?",
                LocalDateTime.now().minusMonths(6).minusDays(1), velho.getId());

        Map<String, Integer> descartados = retencaoDadosService.executar();

        assertThat(logSistemaRepository.findById(velho.getId())).isEmpty();
        assertThat(logSistemaRepository.findById(recente.getId())).isPresent();
        assertThat(descartados.get("logs")).isGreaterThanOrEqualTo(1);
        assertThat(logSistemaRepository.findAll())
                .anyMatch(l -> l.getAcao() == AcaoLog.DESCARTAR_DADOS);
    }

    private Usuario salvarUsuario(String tipo) {
        Usuario usuario = new Usuario();
        usuario.setNomeUsuario("Pessoa Retenção");
        usuario.setUsuario("retencao" + System.nanoTime() + "@teste.com");
        usuario.setSenha("senha-fake-" + UUID.randomUUID());
        usuario.setTipoUsuario(tipo);
        return usuarioRepository.save(usuario);
    }

    private Usuario salvarProfessor(StatusCadastro status, LocalDateTime statusAlteradoEm) {
        Usuario professor = salvarUsuario("PROFESSOR");
        professor.setStatusCadastro(status);
        professor.setStatusAlteradoEm(statusAlteradoEm);
        return usuarioRepository.save(professor);
    }

    private Usuario salvarConvite(LocalDateTime criadoEm) {
        Usuario convidado = new Usuario();
        convidado.setNomeUsuario("Convidado Retenção");
        convidado.setUsuario("convite" + System.nanoTime() + "@teste.com");
        convidado.setSenha("senha-fake-" + UUID.randomUUID());
        convidado.setTipoUsuario("ALUNO");
        convidado.setPrimeiroAcessoPendente(true);
        convidado.setCriadoEm(criadoEm);
        return usuarioRepository.save(convidado);
    }

    private LogSistema salvarLog(Usuario usuario) {
        LogSistema entrada = new LogSistema();
        entrada.setUsuario(usuario);
        entrada.setAcao(AcaoLog.LOGIN);
        entrada.setResultado(ResultadoLog.SUCESSO);
        entrada.setDescricao("Log de teste da retenção");
        return logSistemaRepository.save(entrada);
    }

    private Usuario recarregar(Usuario usuario) {
        return usuarioRepository.findById(usuario.getId()).orElseThrow();
    }

    private static String hashAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
    }
}
