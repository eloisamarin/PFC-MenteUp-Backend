package com.umc.menteup.config;

import com.umc.menteup.config.DadosDemonstracaoSeeder.EmailsDemonstracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

// Apaga tudo o que foi criado pelos dados de demonstração (contas com e-mail seuemail+demo-...)
// Só roda com DEMO_LIMPAR=true; as contas reais e o que elas criaram nas próprias turmas não são tocados
// Roda antes do DadosDemonstracaoSeeder: com as duas opções ligadas, os dados são recriados do zero
@Component
@Order(9)
public class LimpezaDemonstracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LimpezaDemonstracao.class);

    private final boolean ativa;
    private final String emailBase;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public LimpezaDemonstracao(
            @Value("${menteup.demo.limpar:false}") boolean ativa,
            @Value("${menteup.demo.email-base:}") String emailBase,
            JdbcTemplate jdbc,
            PlatformTransactionManager gerenciadorDeTransacao
    ) {
        this.ativa = ativa;
        this.emailBase = emailBase;
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
        this.transacao = new TransactionTemplate(gerenciadorDeTransacao);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ativa) {
            return;
        }
        Optional<EmailsDemonstracao> emails = EmailsDemonstracao.de(emailBase);
        if (emails.isEmpty()) {
            log.warn("Limpeza da demonstração: informe em DEMO_EMAIL_BASE o mesmo e-mail usado para criar os dados");
            return;
        }
        Integer removidos = transacao.execute(status -> limpar(emails.get()));
        log.info("Limpeza da demonstração: {} conta(s) de demonstração removida(s) com os dados delas", removidos);
    }

    private int limpar(EmailsDemonstracao emails) {
        MapSqlParameterSource parametros = emails.parametros();

        // Contas, turmas, atividades e desafios da demonstração
        String contas = "SELECT id FROM tb_usuarios WHERE " + emails.condicao("usuario");
        String turmas = "SELECT id FROM tb_turma WHERE professor_id IN (" + contas + ")";
        String atividades = "SELECT id FROM tb_atividade WHERE usuario_id IN (" + contas + ")";
        String desafios = "SELECT id FROM tb_desafio WHERE turma_id IN (" + turmas + ") OR usuario_id IN (" + contas + ")";
        String perguntasDesafio = "SELECT id FROM tb_pergunta_desafio WHERE desafio_id IN (" + desafios + ")";
        String progressos = "SELECT id FROM progresso_atividade WHERE usuario_id IN (" + contas + ")"
                + " OR atividade_id IN (" + atividades + ")";

        // Na ordem certa: primeiro o que depende das outras tabelas
        List<String> comandos = List.of(
                "DELETE FROM tb_resposta_desafio WHERE usuario_id IN (" + contas + ")"
                        + " OR pergunta_id IN (" + perguntasDesafio + ")",
                "DELETE FROM tb_alternativa_desafio WHERE pergunta_id IN (" + perguntasDesafio + ")",
                "DELETE FROM tb_pergunta_desafio WHERE desafio_id IN (" + desafios + ")",
                "DELETE FROM tb_desafio WHERE id IN (" + desafios + ")",
                "DELETE FROM resposta_atividade_sequencia WHERE resposta_id IN "
                        + "(SELECT id FROM resposta_atividade WHERE progresso_id IN (" + progressos + "))",
                "DELETE FROM resposta_atividade WHERE progresso_id IN (" + progressos + ")",
                "DELETE FROM progresso_atividade WHERE id IN (" + progressos + ")",
                "DELETE FROM resultado_atividade WHERE usuario_id IN (" + contas + ")"
                        + " OR atividade_id IN (" + atividades + ")",
                "UPDATE resultado_atividade SET comentado_por_id = NULL WHERE comentado_por_id IN (" + contas + ")",
                "DELETE FROM alternativas WHERE pergunta_id IN "
                        + "(SELECT id FROM perguntas WHERE atividade_id IN (" + atividades + "))",
                "DELETE FROM perguntas WHERE atividade_id IN (" + atividades + ")",
                "DELETE FROM tb_atividade_turma WHERE atividade_id IN (" + atividades + ")"
                        + " OR turma_id IN (" + turmas + ")",
                "DELETE FROM tb_atividade WHERE id IN (" + atividades + ")",
                "DELETE FROM tb_solicitacao_turma WHERE aluno_id IN (" + contas + ") OR turma_id IN (" + turmas + ")",
                "UPDATE tb_solicitacao_turma SET respondida_por_id = NULL WHERE respondida_por_id IN (" + contas + ")",
                "DELETE FROM tb_turma_aluno WHERE aluno_id IN (" + contas + ") OR turma_id IN (" + turmas + ")",
                "DELETE FROM tb_turma WHERE id IN (" + turmas + ")",
                "DELETE FROM tb_usuario_conquista WHERE usuario_id IN (" + contas + ")",
                "DELETE FROM tb_gamificacao_usuario WHERE usuario_id IN (" + contas + ")",
                // Contas fictícias: o comprovante de aceite não precisa ser guardado
                "DELETE FROM tb_aceite_documento WHERE usuario_id IN (" + contas + ")",
                "DELETE FROM tb_token_redefinicao_senha WHERE usuario_id IN (" + contas + ")",
                "DELETE FROM tb_codigo_2fa WHERE " + emails.condicao("usuario"),
                "DELETE FROM log_sistema WHERE usuario_id IN (" + contas + ")");

        for (String comando : comandos) {
            jdbc.update(comando, parametros);
        }
        return jdbc.update("DELETE FROM tb_usuarios WHERE " + emails.condicao("usuario"), parametros);
    }
}
