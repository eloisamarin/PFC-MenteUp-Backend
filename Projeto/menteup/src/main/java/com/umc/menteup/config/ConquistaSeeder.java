package com.umc.menteup.config;

import com.umc.menteup.model.Conquista;
import com.umc.menteup.repository.ConquistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.umc.menteup.model.CategoriaConquista.*;
import static com.umc.menteup.model.RaridadeConquista.*;
import static com.umc.menteup.model.TipoRegraConquista.*;

///Cadastra as conquistas iniciais quando a aplicação sobe
@Component
@Order(1)
public class ConquistaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConquistaSeeder.class);

    private final ConquistaRepository conquistaRepository;

    public ConquistaSeeder(ConquistaRepository conquistaRepository) {
        this.conquistaRepository = conquistaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int inseridas = 0;

        for (Conquista conquista : catalogoInicial()) {
            if (!conquistaRepository.existsByCodigo(conquista.getCodigo())) {
                conquistaRepository.save(conquista);
                inseridas++;
            }
        }

        if (inseridas > 0) {
            log.info("Catálogo de conquistas: {} conquista(s) cadastrada(s).", inseridas);
        }
    }

    private static List<Conquista> catalogoInicial() {
        return List.of(
                ///Nível de XP
                new Conquista("NIVEL_INICIANTE", "Pequeno Explorador",
                        "Receba seus primeiros pontos de XP e comece a jornada.",
                        NIVEL, XP_ACUMULADO, 1, 20, "nivel-iniciante", BRONZE, 10),
                new Conquista("NIVEL_INTERMEDIARIO", "Em Constante Evolução",
                        "Alcance 1.000 XP acumulados.",
                        NIVEL, XP_ACUMULADO, 1000, 40, "nivel-intermediario", PRATA, 20),
                new Conquista("NIVEL_AVANCADO", "Conexões do Conhecimento",
                        "Alcance 2.000 XP acumulados.",
                        NIVEL, XP_ACUMULADO, 2000, 60, "nivel-avancado", PRATA, 30),
                new Conquista("NIVEL_MESTRE", "Mestre do Conhecimento",
                        "Alcance 3.000 XP acumulados.",
                        NIVEL, XP_ACUMULADO, 3000, 80, "nivel-mestre", OURO, 40),
                new Conquista("NIVEL_GENIO", "Gênio Inovador",
                        "Alcance 4.000 XP acumulados.",
                        NIVEL, XP_ACUMULADO, 4000, 120, "nivel-genio", ESPECIAL, 50),

                ///Atividades
                new Conquista("PRIMEIRA_ATIVIDADE", "Minha Primeira Jornada",
                        "Conclua sua primeira atividade.",
                        ATIVIDADE, CONTAGEM_ATIVIDADES, 1, 20, "primeira-atividade", BRONZE, 60),
                new Conquista("ATIVIDADE_PERFEITA", "Perfeição Absoluta",
                        "Acerte todas as questões de uma atividade.",
                        ATIVIDADE, APROVEITAMENTO, 1, 40, "atividade-perfeita", PRATA, 70),
                new Conquista("DEZ_ATIVIDADES", "Aprendiz Dedicado",
                        "Conclua 10 atividades.",
                        ATIVIDADE, CONTAGEM_ATIVIDADES, 10, 30, "dez-atividades", BRONZE, 80),
                new Conquista("VINTE_CINCO_ATIVIDADES", "Estudante Comprometido",
                        "Conclua 25 atividades.",
                        ATIVIDADE, CONTAGEM_ATIVIDADES, 25, 50, "vinte-cinco-atividades", PRATA, 90),
                new Conquista("CINQUENTA_ATIVIDADES", "Conhecimento em Expansão",
                        "Conclua 50 atividades.",
                        ATIVIDADE, CONTAGEM_ATIVIDADES, 50, 80, "cinquenta-atividades", OURO, 100),
                new Conquista("SETENTA_CINCO_ATIVIDADES", "Mestre das Atividades",
                        "Conclua 75 atividades.",
                        ATIVIDADE, CONTAGEM_ATIVIDADES, 75, 120, "setenta-cinco-atividades", ESPECIAL, 110),

                ///Ranking
                new Conquista("SUBIU_CINCO_POSICOES", "Subindo no Mundo",
                        "Suba pelo menos 5 posições no ranking.",
                        RANKING, EVOLUCAO_RANKING, 5, 60, "subiu-cinco-posicoes", PRATA, 120),
                new Conquista("PRIMEIRO_LUGAR", "O Primeiro Lugar",
                        "Alcance a primeira posição do ranking.",
                        RANKING, POSICAO_RANKING, 1, 150, "primeiro-lugar", ESPECIAL, 130),

                ///Intensivão
                new Conquista("INTENSIVAO_5", "Fogo Inicial",
                        "Acesse a plataforma por 5 dias consecutivos.",
                        INTENSIVAO, SEQUENCIA_DIAS, 5, 30, "intensivao-5", BRONZE, 140),
                new Conquista("INTENSIVAO_15", "Chama Persistente",
                        "Acesse a plataforma por 15 dias consecutivos.",
                        INTENSIVAO, SEQUENCIA_DIAS, 15, 60, "intensivao-15", PRATA, 150),
                new Conquista("INTENSIVAO_30", "Fogo Inabalável",
                        "Acesse a plataforma por 30 dias consecutivos.",
                        INTENSIVAO, SEQUENCIA_DIAS, 30, 100, "intensivao-30", OURO, 160),
                new Conquista("INTENSIVAO_60", "Chama Lendária",
                        "Acesse a plataforma por 60 dias consecutivos.",
                        INTENSIVAO, SEQUENCIA_DIAS, 60, 180, "intensivao-60", OURO, 170),
                new Conquista("INTENSIVAO_90", "Imparável",
                        "Acesse a plataforma por 90 dias consecutivos.",
                        INTENSIVAO, SEQUENCIA_DIAS, 90, 300, "intensivao-90", ESPECIAL, 180)
        );
    }
}
