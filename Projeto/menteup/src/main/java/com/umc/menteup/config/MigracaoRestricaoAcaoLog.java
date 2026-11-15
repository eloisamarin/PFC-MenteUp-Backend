package com.umc.menteup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Quando a tabela de logs foi criada, o banco guardou a lista de ações que existiam na época
// Com o ddl-auto=update essa lista nunca é atualizada, e um log com ação nova seria recusado sem aviso
// Quando a aplicação sobe, apaga essa trava antiga (a ação continua sendo conferida pelo Java)
// Se a trava não existe (banco novo ou já ajustado), não faz nada
@Component
@Order(0)
public class MigracaoRestricaoAcaoLog implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoRestricaoAcaoLog.class);
    private static final String RESTRICAO = "log_sistema_acao_check";

    private final JdbcTemplate jdbc;

    public MigracaoRestricaoAcaoLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE LOWER(table_name) = 'log_sistema' AND LOWER(constraint_name) = ?",
                Integer.class, RESTRICAO);

        if (existe == null || existe == 0) {
            return;
        }

        jdbc.execute("ALTER TABLE log_sistema DROP CONSTRAINT " + RESTRICAO);
        log.info("Logs: restrição antiga da coluna acao removida.");
    }
}
