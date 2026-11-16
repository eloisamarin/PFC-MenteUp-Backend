package com.umc.menteup.config;

import com.umc.menteup.model.TipoAtividade;

import java.util.ArrayList;
import java.util.List;

// Textos usados nos dados de demonstração: matérias, atividades, desafios, comentários e nomes
// Pode trocar os textos à vontade, a lógica fica no DadosDemonstracaoSeeder
final class ConteudoDemonstracao {

    private ConteudoDemonstracao() {
    }

    record AlternativaModelo(String texto, boolean correta, Integer ordem) {
    }

    record PerguntaModelo(String enunciado, List<AlternativaModelo> alternativas) {
    }

    record AtividadeModelo(String titulo, String descricao, TipoAtividade tipo, int pontuacao,
                           List<PerguntaModelo> perguntas) {
    }

    record PerguntaDesafioModelo(String enunciado, int tempoSegundos, String correta, List<String> erradas) {
    }

    record DesafioModelo(String titulo, String descricao, List<PerguntaDesafioModelo> perguntas) {
    }

    // Banco de conteúdo de cada matéria: os professores da mesma matéria usam as mesmas atividades e desafios
    record Materia(String nome, List<AtividadeModelo> atividades, List<DesafioModelo> desafios) {
    }

    static final List<String> COMENTARIOS = List.of(
            "Ótimo trabalho, continue assim!",
            "Reveja a matéria da última aula, você está quase lá.",
            "Excelente evolução em relação à atividade anterior.",
            "Atenção às questões de interpretação.",
            "Parabéns pelo empenho!",
            "Bom resultado! Tente responder com mais calma na próxima.",
            "Muito bem, você dominou o conteúdo.");

    static final List<String> PRIMEIROS_NOMES = List.of(
            "Ana", "Bruno", "Camila", "Diego", "Eduarda", "Felipe", "Gabriela", "Henrique", "Isabela", "João",
            "Larissa", "Lucas", "Mariana", "Mateus", "Natália", "Otávio", "Paula", "Rafael", "Sofia", "Thiago",
            "Valentina", "Vinícius", "Beatriz", "Caio", "Débora", "Enzo", "Fernanda", "Gustavo", "Helena", "Igor",
            "Júlia", "Leonardo", "Manuela", "Nicolas", "Olívia", "Pedro", "Rebeca", "Samuel", "Taís", "Yasmin");

    static final List<String> SOBRENOMES = List.of(
            "Silva", "Souza", "Oliveira", "Santos", "Pereira", "Costa", "Rodrigues", "Almeida", "Nascimento",
            "Lima", "Araújo", "Ferreira", "Carvalho", "Gomes", "Martins", "Rocha", "Ribeiro", "Barbosa",
            "Moreira", "Teixeira", "Cardoso", "Melo", "Freitas", "Mendes", "Castro");

    static final List<Materia> MATERIAS = List.of(matematica(), portugues(), ciencias(), historia(), geografia());

    private static Materia matematica() {
        return new Materia("Matemática",
                List.of(
                        atividade("Operações com frações", "Resolva as operações com frações e escolha a alternativa correta.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Quanto é 1/2 + 1/4?", "3/4", "2/6", "1/6", "2/4"),
                                quiz("Quanto é 2/3 de 9?", "6", "3", "4", "18"),
                                quiz("Qual fração é equivalente a 2/4?", "1/2", "2/8", "4/2", "1/4"),
                                quiz("Quanto é 3/5 de 10?", "6", "3", "5", "15")),
                        atividade("Números inteiros: verdadeiro ou falso", "Leia cada afirmação sobre números inteiros e diga se é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("-5 é maior que -2.", false),
                                vf("O produto de dois números negativos é positivo.", true),
                                vf("Zero é um número inteiro.", true),
                                vf("(-3) + 7 = -10.", false)),
                        atividade("Fórmulas de área", "Associe cada figura plana à fórmula que calcula a sua área.",
                                TipoAtividade.ASSOCIACAO, 100,
                                par("Quadrado", "lado × lado"),
                                par("Retângulo", "base × altura"),
                                par("Triângulo", "(base × altura) ÷ 2"),
                                par("Círculo", "π × raio²")),
                        atividade("Equações do 1º grau", "Complete as lacunas com o valor que torna cada igualdade verdadeira.",
                                TipoAtividade.LACUNAS, 120,
                                lacunas("Em 2x + 4 = 10, o valor de x é {{1}}.", List.of("3"), "4", "7"),
                                lacunas("Em x - 5 = 12, x vale {{1}}.", List.of("17"), "7", "60"),
                                lacunas("O dobro de 6 é {{1}} e o triplo de 6 é {{2}}.", List.of("12", "18"), "9", "24"),
                                lacunas("Se 3x = 21, então x = {{1}}.", List.of("7"), "6", "18")),
                        atividade("Ordem crescente", "Organize os itens na ordem correta, do menor para o maior.",
                                TipoAtividade.ORDENACAO, 100,
                                ordenar("Coloque os números em ordem crescente", "-3", "0", "1/2", "2", "5"),
                                ordenar("Coloque as frações em ordem crescente", "1/4", "1/3", "1/2", "3/4"),
                                ordenar("Ordene as etapas para resolver 2x + 3 = 11",
                                        "Subtrair 3 dos dois lados", "Obter 2x = 8",
                                        "Dividir os dois lados por 2", "Concluir que x = 4"),
                                ordenar("Coloque os números decimais em ordem crescente", "0,05", "0,5", "1,05", "1,5")),
                        atividade("Porcentagem no dia a dia", "Resolva os problemas de porcentagem que aparecem em situações do cotidiano.",
                                TipoAtividade.QUIZ, 120,
                                quiz("Quanto é 10% de 250?", "25", "2,5", "10", "50"),
                                quiz("Um produto de R$ 80 teve 25% de desconto. Qual o novo preço?", "R$ 60", "R$ 55", "R$ 64", "R$ 75"),
                                quiz("Qual porcentagem representa 3 de 12?", "25%", "3%", "30%", "36%"),
                                quiz("Um salário de R$ 2.000 teve aumento de 5%. Qual o novo valor?", "R$ 2.100", "R$ 2.005", "R$ 2.050", "R$ 2.500")),
                        atividade("Geometria plana", "Diga se cada afirmação sobre figuras e ângulos é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("A soma dos ângulos internos de um triângulo é 180°.", true),
                                vf("Todo quadrado é um retângulo.", true),
                                vf("Um hexágono tem 5 lados.", false),
                                vf("O ângulo reto mede 45°.", false)),
                        atividade("Potências e raízes", "Complete as lacunas com o resultado de cada potência ou raiz.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("2 elevado a 3 é igual a {{1}}.", List.of("8"), "6", "9"),
                                lacunas("A raiz quadrada de 81 é {{1}}.", List.of("9"), "8", "18"),
                                lacunas("10² = {{1}} e 10³ = {{2}}.", List.of("100", "1000"), "20", "30"),
                                lacunas("Todo número diferente de zero elevado a 0 é igual a {{1}}.", List.of("1"), "0", "10"))),
                List.of(
                        desafio("Desafio relâmpago: tabuada", "Perguntas rápidas de multiplicação para aquecer a turma.",
                                pd("Quanto é 7 × 8?", 20, "56", "54", "48", "64"),
                                pd("Quanto é 9 × 6?", 20, "54", "56", "45", "63"),
                                pd("Quanto é 12 × 11?", 25, "132", "121", "122", "144"),
                                pd("Quanto é 15 × 4?", 20, "60", "45", "50", "75"),
                                pd("Quanto é 11 × 11?", 20, "121", "111", "122", "112"),
                                pd("Quanto é 13 × 3?", 20, "39", "36", "33", "43"),
                                pd("Quanto é 25 × 4?", 20, "100", "75", "125", "90"),
                                pd("Quanto é 8 × 8?", 20, "64", "56", "72", "88"),
                                pd("Quanto é 14 × 5?", 20, "70", "60", "75", "65"),
                                pd("Quanto é 6 × 7?", 20, "42", "36", "48", "49")),
                        desafio("Desafio: frações e porcentagens", "Quem responde certo e mais rápido ganha mais pontos.",
                                pd("Metade de 3/4 é:", 30, "3/8", "3/2", "1/4", "1/8"),
                                pd("20% de 150 é:", 25, "30", "20", "15", "35"),
                                pd("Qual é maior: 2/3 ou 3/5?", 30, "2/3", "3/5", "São iguais", "Não dá para comparar"),
                                pd("0,25 corresponde a:", 20, "1/4", "1/2", "2/5", "1/25"),
                                pd("Quanto é 1/3 + 1/3?", 25, "2/3", "2/6", "1/9", "1/3"),
                                pd("50% de 80 é:", 20, "40", "50", "30", "8"),
                                pd("3/4 corresponde a quantos por cento?", 25, "75%", "34%", "43%", "70%"),
                                pd("Quanto é 1/5 de 100?", 20, "20", "5", "15", "25"),
                                pd("Quanto é 0,5 + 0,25?", 20, "0,75", "0,30", "0,525", "1,25"),
                                pd("Qual fração representa a metade de um inteiro?", 20, "1/2", "1/3", "2/1", "1/4")),
                        desafio("Desafio: geometria em ação", "Perguntas rápidas sobre figuras planas, ângulos e medidas.",
                                pd("Quantos lados tem um octógono?", 20, "8", "6", "7", "10"),
                                pd("A área de um quadrado de lado 5 cm é:", 25, "25 cm²", "20 cm²", "10 cm²", "15 cm²"),
                                pd("Um ângulo de 90° é chamado de:", 20, "Reto", "Agudo", "Obtuso", "Raso"),
                                pd("O perímetro de um retângulo de 3 cm por 4 cm é:", 30, "14 cm", "12 cm", "7 cm", "24 cm"),
                                pd("Quantos graus tem uma volta completa?", 20, "360°", "180°", "90°", "270°"),
                                pd("Um triângulo com os três lados iguais é chamado de:", 25, "Equilátero", "Isósceles", "Escaleno", "Retângulo"),
                                pd("Quantas faces tem um cubo?", 20, "6", "4", "8", "12"),
                                pd("A área de um retângulo de 2 cm por 6 cm é:", 25, "12 cm²", "8 cm²", "16 cm²", "14 cm²"),
                                pd("Quantos lados tem um pentágono?", 20, "5", "6", "4", "8"),
                                pd("Um ângulo maior que 90° e menor que 180° é:", 25, "Obtuso", "Agudo", "Reto", "Raso"))));
    }

    private static Materia portugues() {
        return new Materia("Língua Portuguesa",
                List.of(
                        atividade("Classes de palavras", "Identifique a classe gramatical das palavras em cada frase.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Na frase \"O menino correu rápido\", qual palavra é um verbo?", "correu", "menino", "rápido", "O"),
                                quiz("Qual das palavras é um adjetivo?", "bonito", "correr", "ontem", "mesa"),
                                quiz("Em \"Ela chegou cedo\", a palavra \"cedo\" é:", "Advérbio", "Substantivo", "Adjetivo", "Pronome"),
                                quiz("Qual das palavras é um pronome?", "nós", "casa", "feliz", "andar")),
                        atividade("Acentuação gráfica", "Diga se cada afirmação sobre acentuação é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("A palavra \"café\" é oxítona.", true),
                                vf("\"Lâmpada\" é uma palavra proparoxítona.", true),
                                vf("A palavra \"herói\" não leva acento.", false),
                                vf("Todas as palavras paroxítonas são acentuadas.", false)),
                        atividade("Figuras de linguagem", "Associe cada figura de linguagem à sua definição.",
                                TipoAtividade.ASSOCIACAO, 120,
                                par("Metáfora", "Comparação sem o uso de conectivo"),
                                par("Hipérbole", "Exagero intencional de uma ideia"),
                                par("Personificação", "Características humanas dadas a seres não humanos"),
                                par("Onomatopeia", "Palavra que imita um som")),
                        atividade("Concordância verbal", "Complete as frases com a forma verbal que concorda com o sujeito.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("Os alunos {{1}} cedo para a prova.", List.of("chegaram"), "chegou", "chegará"),
                                lacunas("Nós {{1}} a lição ontem.", List.of("fizemos"), "fez", "fizeram"),
                                lacunas("Eu e minha irmã {{1}} ao cinema.", List.of("fomos"), "foi", "foram"),
                                lacunas("Tu {{1}} muito bem e ele {{2}} também.", List.of("cantas", "canta"), "canto", "cantam")),
                        atividade("Estrutura do texto", "Organize os elementos na ordem em que aparecem em um texto.",
                                TipoAtividade.ORDENACAO, 100,
                                ordenar("Ordene os elementos da narrativa", "Situação inicial", "Conflito", "Clímax", "Desfecho"),
                                ordenar("Ordene as frases para formar um parágrafo",
                                        "Era uma manhã fria de inverno.", "Ana acordou cedo e foi até a janela.",
                                        "Lá fora, a cidade estava coberta de neblina.", "Ela decidiu ficar em casa lendo um livro."),
                                ordenar("Ordene as palavras para formar a frase", "O", "professor", "explicou", "a", "matéria"),
                                ordenar("Ordene as etapas de produção de um texto",
                                        "Planejar as ideias", "Escrever o rascunho", "Revisar o texto", "Publicar a versão final")),
                        atividade("Interpretação de texto", "Responda às perguntas sobre gêneros textuais e interpretação.",
                                TipoAtividade.QUIZ, 120,
                                quiz("Qual é a principal função de um texto instrucional?", "Orientar o leitor a realizar algo",
                                        "Contar uma história", "Descrever um lugar", "Defender uma opinião"),
                                quiz("Uma notícia de jornal pertence ao gênero:", "Jornalístico", "Lírico", "Dramático", "Épico"),
                                quiz("Em um texto argumentativo, a tese é:", "A opinião defendida pelo autor",
                                        "O título do texto", "Uma conclusão sem argumentos", "O resumo dos fatos"),
                                quiz("O narrador que participa da história é chamado de:", "Narrador-personagem",
                                        "Narrador observador", "Narrador onisciente", "Autor")),
                        atividade("Ortografia", "Diga se cada afirmação sobre a escrita das palavras é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("A palavra correta é \"exceção\".", true),
                                vf("O correto é escrever \"concerteza\".", false),
                                vf("\"Mas\" indica oposição, e \"mais\" indica quantidade.", true),
                                vf("A forma correta é \"a gente vamos\".", false)),
                        atividade("Uso da crase", "Complete as lacunas usando ou não o acento grave.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("Vou {{1}} escola amanhã.", List.of("à"), "a", "há"),
                                lacunas("Entreguei o livro {{1}} professora.", List.of("à"), "a", "ao"),
                                lacunas("Chegamos {{1}} 10 horas.", List.of("às"), "as", "há"),
                                lacunas("Ele saiu {{1}} pé.", List.of("a"), "à", "ao"))),
                List.of(
                        desafio("Desafio: sinônimos e antônimos", "Encontre o sinônimo ou o antônimo o mais rápido possível.",
                                pd("Qual é o sinônimo de \"feliz\"?", 20, "Alegre", "Triste", "Cansado", "Bravo"),
                                pd("Qual é o antônimo de \"grande\"?", 20, "Pequeno", "Enorme", "Largo", "Alto"),
                                pd("Qual é o sinônimo de \"rápido\"?", 20, "Veloz", "Lento", "Calmo", "Pesado"),
                                pd("Qual é o antônimo de \"claro\"?", 20, "Escuro", "Brilhante", "Limpo", "Leve"),
                                pd("Qual é o sinônimo de \"bonito\"?", 20, "Belo", "Feio", "Estranho", "Simples"),
                                pd("Qual é o antônimo de \"alto\"?", 20, "Baixo", "Grande", "Largo", "Comprido"),
                                pd("Qual é o sinônimo de \"começar\"?", 20, "Iniciar", "Terminar", "Parar", "Desistir"),
                                pd("Qual é o antônimo de \"cheio\"?", 20, "Vazio", "Lotado", "Pesado", "Completo"),
                                pd("Qual é o sinônimo de \"esperto\"?", 20, "Inteligente", "Lento", "Distraído", "Ingênuo"),
                                pd("Qual é o antônimo de \"rico\"?", 20, "Pobre", "Farto", "Nobre", "Caro")),
                        desafio("Desafio: gramática rápida", "Plural, feminino, sujeito e concordância em perguntas rápidas.",
                                pd("Qual é o plural de \"cidadão\"?", 25, "cidadãos", "cidadões", "cidadães", "cidadaos"),
                                pd("Qual frase está correta?", 30, "Havia muitas pessoas na festa.",
                                        "Haviam muitas pessoas na festa.", "Houveram muitas pessoas na festa.",
                                        "Hão muitas pessoas na festa."),
                                pd("Qual é o sujeito de \"Choveu muito ontem\"?", 30, "Inexistente", "Ontem", "Muito", "Oculto"),
                                pd("Qual é o feminino de \"ator\"?", 20, "atriz", "atora", "atoriz", "atriza"),
                                pd("Qual é o plural de \"pão\"?", 20, "pães", "pãos", "pões", "pãoes"),
                                pd("Qual é o aumentativo de \"casa\"?", 20, "casarão", "casinha", "casebre", "casona"),
                                pd("Qual é o coletivo de \"lobos\"?", 25, "alcateia", "cardume", "rebanho", "enxame"),
                                pd("Qual é o coletivo de \"abelhas\"?", 25, "enxame", "alcateia", "manada", "matilha"),
                                pd("Qual é o feminino de \"cavalo\"?", 20, "égua", "cavala", "potra", "vaca"),
                                pd("Qual é o plural de \"anel\"?", 20, "anéis", "anels", "aneis", "anéus"))));
    }

    private static Materia ciencias() {
        return new Materia("Ciências",
                List.of(
                        atividade("Sistema solar", "Responda às perguntas sobre os planetas e os movimentos da Terra.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Qual é o planeta mais próximo do Sol?", "Mercúrio", "Vênus", "Terra", "Marte"),
                                quiz("Qual é o maior planeta do sistema solar?", "Júpiter", "Saturno", "Netuno", "Terra"),
                                quiz("Quanto tempo a Terra leva para dar uma volta ao redor do Sol?", "Cerca de 365 dias",
                                        "24 horas", "30 dias", "12 anos"),
                                quiz("O que é a Lua?", "Um satélite natural da Terra", "Uma estrela", "Um planeta", "Um cometa")),
                        atividade("Células", "Diga se cada afirmação sobre as células é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("A célula é a menor unidade viva.", true),
                                vf("Células vegetais possuem parede celular.", true),
                                vf("Bactérias possuem núcleo definido.", false),
                                vf("As mitocôndrias são responsáveis pela fotossíntese.", false)),
                        atividade("Órgãos e funções", "Associe cada órgão do corpo humano à sua principal função.",
                                TipoAtividade.ASSOCIACAO, 100,
                                par("Coração", "Bombear o sangue pelo corpo"),
                                par("Pulmões", "Realizar as trocas gasosas"),
                                par("Estômago", "Iniciar a digestão das proteínas"),
                                par("Rins", "Filtrar o sangue e produzir a urina")),
                        atividade("Ciclos e etapas da natureza", "Organize as etapas e as camadas na ordem correta.",
                                TipoAtividade.ORDENACAO, 100,
                                ordenar("Ordene as etapas do ciclo da água", "Evaporação", "Condensação", "Precipitação", "Infiltração"),
                                ordenar("Ordene as camadas da Terra, da superfície para o centro",
                                        "Crosta", "Manto", "Núcleo externo", "Núcleo interno"),
                                ordenar("Ordene os níveis de organização dos seres vivos",
                                        "Célula", "Tecido", "Órgão", "Sistema", "Organismo"),
                                ordenar("Ordene as etapas do método científico", "Observação", "Hipótese", "Experimentação", "Conclusão")),
                        atividade("Fotossíntese", "Complete as frases sobre como as plantas produzem o próprio alimento.",
                                TipoAtividade.LACUNAS, 120,
                                lacunas("As plantas absorvem {{1}} do ar para fazer fotossíntese.", List.of("gás carbônico"), "oxigênio", "nitrogênio"),
                                lacunas("A fotossíntese acontece principalmente nas {{1}}.", List.of("folhas"), "raízes", "flores"),
                                lacunas("O pigmento verde das plantas se chama {{1}}.", List.of("clorofila"), "melanina", "hemoglobina"),
                                lacunas("Na fotossíntese, a planta produz {{1}} e libera {{2}}.", List.of("glicose", "oxigênio"), "água", "nitrogênio")),
                        atividade("Estados físicos da matéria", "Responda às perguntas sobre as mudanças de estado físico.",
                                TipoAtividade.QUIZ, 100,
                                quiz("A passagem do estado líquido para o gasoso se chama:", "Vaporização", "Fusão", "Solidificação", "Condensação"),
                                quiz("Em qual estado as partículas estão mais próximas umas das outras?", "Sólido", "Líquido", "Gasoso", "Plasma"),
                                quiz("O gelo derretendo é um exemplo de:", "Fusão", "Sublimação", "Evaporação", "Condensação"),
                                quiz("Ao nível do mar, a água ferve a:", "100 °C", "0 °C", "50 °C", "212 °C")),
                        atividade("Ecologia", "Diga se cada afirmação sobre os seres vivos e o ambiente é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("Os produtores de uma cadeia alimentar fazem fotossíntese.", true),
                                vf("Os decompositores não são importantes para o ambiente.", false),
                                vf("O desmatamento pode causar perda de biodiversidade.", true),
                                vf("Todos os animais são herbívoros.", false)),
                        atividade("Misturas e substâncias", "Complete as frases sobre misturas e métodos de separação.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("A água com sal dissolvido é uma mistura {{1}}.", List.of("homogênea"), "heterogênea", "gasosa"),
                                lacunas("Água e óleo formam uma mistura {{1}}.", List.of("heterogênea"), "homogênea", "simples"),
                                lacunas("A fórmula da água é {{1}}.", List.of("H2O"), "CO2", "O2"),
                                lacunas("Para separar a areia da água, usamos a {{1}}.", List.of("filtração"), "destilação", "fusão"))),
                List.of(
                        desafio("Desafio: corpo humano", "Perguntas rápidas sobre o funcionamento do corpo humano.",
                                pd("Quantos ossos tem o corpo humano adulto?", 25, "206", "106", "306", "150"),
                                pd("Qual é o maior órgão do corpo humano?", 20, "Pele", "Fígado", "Coração", "Pulmão"),
                                pd("Qual tipo sanguíneo é o doador universal?", 25, "O negativo", "AB positivo", "A positivo", "B negativo"),
                                pd("Onde fica o fêmur?", 20, "Na coxa", "No braço", "Na cabeça", "No tórax"),
                                pd("Qual órgão bombeia o sangue pelo corpo?", 20, "Coração", "Pulmão", "Fígado", "Rim"),
                                pd("Quantos dentes tem, em geral, um adulto?", 25, "32", "20", "28", "36"),
                                pd("Os pulmões fazem parte de qual sistema?", 20, "Respiratório", "Digestório", "Nervoso", "Urinário"),
                                pd("Qual órgão produz a insulina?", 25, "Pâncreas", "Estômago", "Fígado", "Baço"),
                                pd("Qual célula do sangue transporta oxigênio?", 25, "Hemácia", "Plaqueta", "Leucócito", "Neurônio"),
                                pd("A língua está ligada a qual sentido?", 20, "Paladar", "Olfato", "Tato", "Audição")),
                        desafio("Desafio: planeta Terra", "Atmosfera, oceanos e movimentos da Terra.",
                                pd("Qual gás é mais abundante na atmosfera?", 25, "Nitrogênio", "Oxigênio", "Gás carbônico", "Hidrogênio"),
                                pd("Qual é o maior oceano da Terra?", 20, "Pacífico", "Atlântico", "Índico", "Ártico"),
                                pd("A camada que protege a Terra dos raios ultravioleta é a:", 30, "Camada de ozônio",
                                        "Troposfera", "Crosta", "Litosfera"),
                                pd("O movimento da Terra em torno de si mesma se chama:", 20, "Rotação",
                                        "Translação", "Revolução", "Precessão"),
                                pd("Qual planeta é conhecido como planeta vermelho?", 20, "Marte", "Júpiter", "Vênus", "Saturno"),
                                pd("Quanto tempo dura a rotação da Terra?", 20, "Cerca de 24 horas", "Cerca de 365 dias", "Cerca de 30 dias", "Cerca de 12 horas"),
                                pd("A maior parte da superfície da Terra é coberta por:", 20, "Água", "Florestas", "Desertos", "Gelo"),
                                pd("Qual é a estrela mais próxima da Terra?", 20, "Sol", "Sirius", "Estrela Polar", "Alfa Centauri"),
                                pd("Rochas formadas pelo resfriamento do magma são chamadas de:", 30, "Magmáticas", "Sedimentares", "Metamórficas", "Calcárias"),
                                pd("O que causa as estações do ano?", 30, "A inclinação do eixo da Terra", "A distância até o Sol", "As fases da Lua", "Os ventos"))));
    }

    private static Materia historia() {
        return new Materia("História",
                List.of(
                        atividade("Grandes navegações", "Responda às perguntas sobre as viagens marítimas europeias.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Qual país iniciou as Grandes Navegações?", "Portugal", "Espanha", "Inglaterra", "França"),
                                quiz("Quem chegou à Índia por mar em 1498?", "Vasco da Gama", "Pedro Álvares Cabral", "Cristóvão Colombo", "Fernão de Magalhães"),
                                quiz("Qual instrumento ajudava na orientação em alto-mar?", "Bússola", "Telescópio", "Relógio de sol", "Termômetro"),
                                quiz("O que os europeus buscavam no Oriente?", "Especiarias", "Petróleo", "Café", "Borracha")),
                        atividade("Brasil Colônia", "Diga se cada afirmação sobre o período colonial é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("As capitanias hereditárias dividiram o território em faixas de terra.", true),
                                vf("O ciclo do ouro aconteceu principalmente em Minas Gerais.", true),
                                vf("A primeira capital do Brasil foi Brasília.", false),
                                vf("Os jesuítas não tiveram nenhum papel na colonização.", false)),
                        atividade("Personagens da história do Brasil", "Associe cada personagem ao acontecimento ligado a ele.",
                                TipoAtividade.ASSOCIACAO, 100,
                                par("Tiradentes", "Inconfidência Mineira"),
                                par("Dom Pedro I", "Independência do Brasil"),
                                par("Princesa Isabel", "Assinatura da Lei Áurea"),
                                par("Zumbi", "Quilombo dos Palmares")),
                        atividade("Linha do tempo", "Organize os acontecimentos e os períodos na ordem em que aconteceram.",
                                TipoAtividade.ORDENACAO, 120,
                                ordenar("Ordene os acontecimentos da história do Brasil", "Chegada dos portugueses",
                                        "Independência", "Abolição da escravidão", "Proclamação da República"),
                                ordenar("Ordene os períodos da história do Brasil", "Colônia", "Império", "República"),
                                ordenar("Ordene os ciclos econômicos do Brasil", "Pau-brasil", "Cana-de-açúcar", "Ouro", "Café"),
                                ordenar("Ordene os períodos da História", "Pré-História", "Idade Antiga", "Idade Média",
                                        "Idade Moderna", "Idade Contemporânea")),
                        atividade("Antiguidade clássica", "Complete as frases sobre a Grécia, Roma e o Egito antigos.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("A democracia surgiu na cidade de {{1}}.", List.of("Atenas"), "Esparta", "Roma"),
                                lacunas("Os {{1}} construíram as pirâmides de Gizé.", List.of("egípcios"), "romanos", "gregos"),
                                lacunas("O primeiro imperador romano foi {{1}}.", List.of("Augusto"), "Nero", "Calígula"),
                                lacunas("Na Grécia antiga, os Jogos {{1}} homenageavam Zeus.", List.of("Olímpicos"), "Romanos", "Pan-Americanos")),
                        atividade("República e Era Vargas", "Responda às perguntas sobre a história do Brasil republicano.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Em que ano Getúlio Vargas chegou ao poder?", "1930", "1889", "1945", "1964"),
                                quiz("Qual lei reuniu os direitos dos trabalhadores em 1943?", "CLT", "Lei Áurea", "Constituição de 1988", "Código Civil"),
                                quiz("Qual cidade se tornou a capital do Brasil em 1960?", "Brasília", "Rio de Janeiro", "Salvador", "São Paulo"),
                                quiz("A Constituição atual do Brasil é de:", "1988", "1946", "1967", "1891"))),
                List.of(
                        desafio("Desafio: Brasil Colônia e Império", "Datas e personagens que marcaram a história do Brasil.",
                                pd("Em que ano os portugueses chegaram ao Brasil?", 20, "1500", "1492", "1822", "1889"),
                                pd("Quem comandou a esquadra que chegou ao Brasil em 1500?", 25, "Pedro Álvares Cabral", "Cristóvão Colombo", "Dom Pedro I", "Tiradentes"),
                                pd("Em que ano o Brasil se tornou independente?", 20, "1822", "1500", "1889", "1888"),
                                pd("Quem foi o primeiro imperador do Brasil?", 20, "Dom Pedro I", "Dom Pedro II", "Deodoro da Fonseca", "Getúlio Vargas"),
                                pd("Qual lei aboliu a escravidão no Brasil?", 25, "Lei Áurea", "Lei do Ventre Livre", "Lei dos Sexagenários", "Lei Eusébio de Queirós"),
                                pd("Em que ano a escravidão foi abolida no Brasil?", 20, "1888", "1822", "1889", "1850"),
                                pd("Qual foi o primeiro produto explorado pelos portugueses no Brasil?", 25, "Pau-brasil", "Café", "Ouro", "Borracha"),
                                pd("Em que ano foi proclamada a República?", 20, "1889", "1888", "1822", "1930"),
                                pd("Quem proclamou a República no Brasil?", 25, "Deodoro da Fonseca", "Floriano Peixoto", "Dom Pedro II", "Getúlio Vargas"),
                                pd("Qual foi a primeira capital do Brasil?", 20, "Salvador", "Rio de Janeiro", "Brasília", "São Paulo")),
                        desafio("Desafio: História Geral", "Da Antiguidade ao século XX em perguntas rápidas.",
                                pd("Qual civilização construiu as pirâmides de Gizé?", 20, "Egípcios", "Romanos", "Gregos", "Maias"),
                                pd("Em qual cidade surgiu a democracia?", 20, "Atenas", "Roma", "Esparta", "Babilônia"),
                                pd("Em que ano caiu o Império Romano do Ocidente?", 30, "476", "1453", "1500", "33"),
                                pd("Qual período fica entre a Antiguidade e a Idade Moderna?", 25, "Idade Média", "Pré-História", "Idade Contemporânea", "Renascimento"),
                                pd("Qual revolução começou em 1789?", 20, "Revolução Francesa", "Revolução Industrial", "Revolução Russa", "Revolução Americana"),
                                pd("Em qual país começou a Revolução Industrial?", 25, "Inglaterra", "França", "Estados Unidos", "Alemanha"),
                                pd("Em que ano começou a Primeira Guerra Mundial?", 20, "1914", "1939", "1918", "1945"),
                                pd("Em que ano terminou a Segunda Guerra Mundial?", 20, "1945", "1939", "1918", "1950"),
                                pd("Qual muro foi derrubado em 1989?", 20, "Muro de Berlim", "Muralha da China", "Muro de Adriano", "Muro das Lamentações"),
                                pd("Quem chegou à América em 1492?", 20, "Cristóvão Colombo", "Vasco da Gama", "Pedro Álvares Cabral", "Fernão de Magalhães"))));
    }

    private static Materia geografia() {
        return new Materia("Geografia",
                List.of(
                        atividade("Regiões do Brasil", "Responda às perguntas sobre as cinco regiões brasileiras.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Quantas regiões tem o Brasil, segundo o IBGE?", "5", "4", "6", "7"),
                                quiz("Qual é a maior região do Brasil em área?", "Norte", "Nordeste", "Sudeste", "Centro-Oeste"),
                                quiz("Qual região tem mais habitantes?", "Sudeste", "Sul", "Norte", "Nordeste"),
                                quiz("A qual região pertence o estado da Bahia?", "Nordeste", "Sudeste", "Norte", "Centro-Oeste")),
                        atividade("Biomas brasileiros", "Diga se cada afirmação sobre os biomas é verdadeira ou falsa.",
                                TipoAtividade.VERDADEIRO_FALSO, 80,
                                vf("A Amazônia é a maior floresta tropical do mundo.", true),
                                vf("A Caatinga é típica da região Sul.", false),
                                vf("O Pantanal é uma grande planície que alaga na época das chuvas.", true),
                                vf("O Cerrado não existe no Centro-Oeste.", false)),
                        atividade("Estados e capitais", "Associe cada estado à sua capital.",
                                TipoAtividade.ASSOCIACAO, 100,
                                par("Bahia", "Salvador"),
                                par("Minas Gerais", "Belo Horizonte"),
                                par("Amazonas", "Manaus"),
                                par("Rio Grande do Sul", "Porto Alegre")),
                        atividade("Comparando tamanhos", "Organize os itens na ordem pedida em cada questão.",
                                TipoAtividade.ORDENACAO, 100,
                                ordenar("Ordene do menor para o maior", "Bairro", "Município", "Estado", "País", "Continente"),
                                ordenar("Ordene os planetas do mais próximo ao mais distante do Sol", "Mercúrio", "Vênus", "Terra", "Marte"),
                                ordenar("Ordene as regiões do Brasil da maior para a menor área",
                                        "Norte", "Centro-Oeste", "Nordeste", "Sudeste", "Sul"),
                                ordenar("Ordene os continentes do maior para o menor em área",
                                        "Ásia", "América", "África", "Europa", "Oceania")),
                        atividade("Cartografia", "Complete as frases sobre mapas, coordenadas e orientação.",
                                TipoAtividade.LACUNAS, 100,
                                lacunas("Os pontos cardeais são norte, sul, leste e {{1}}.", List.of("oeste"), "nordeste", "sudeste"),
                                lacunas("A linha do {{1}} divide a Terra em hemisférios norte e sul.", List.of("Equador"), "Trópico", "Greenwich"),
                                lacunas("O meridiano de {{1}} divide a Terra em leste e oeste.", List.of("Greenwich"), "Equador", "Capricórnio"),
                                lacunas("A {{1}} indica quantas vezes o mapa foi reduzido.", List.of("escala"), "legenda", "bússola")),
                        atividade("Clima e relevo", "Responda às perguntas sobre o clima, o relevo e os rios do Brasil.",
                                TipoAtividade.QUIZ, 100,
                                quiz("Qual é o ponto mais alto do Brasil?", "Pico da Neblina", "Pico da Bandeira", "Monte Roraima", "Pão de Açúcar"),
                                quiz("Qual é o clima predominante na região Sul?", "Subtropical", "Equatorial", "Semiárido", "Tropical úmido"),
                                quiz("Qual é o maior rio do Brasil em volume de água?", "Amazonas", "São Francisco", "Paraná", "Tietê"),
                                quiz("O clima semiárido é típico de qual região?", "Nordeste", "Sul", "Norte", "Sudeste"))),
                List.of(
                        desafio("Desafio: Brasil em números", "Estados, capitais, rios e biomas do Brasil.",
                                pd("Qual é o maior estado do Brasil em área?", 25, "Amazonas", "Pará", "Mato Grosso", "Minas Gerais"),
                                pd("Qual é a capital do Brasil?", 20, "Brasília", "Rio de Janeiro", "São Paulo", "Salvador"),
                                pd("Quantos estados tem o Brasil, sem contar o Distrito Federal?", 25, "26", "27", "25", "24"),
                                pd("Qual é a capital do Paraná?", 20, "Curitiba", "Londrina", "Florianópolis", "Maringá"),
                                pd("Qual rio é conhecido como Velho Chico?", 20, "São Francisco", "Amazonas", "Paraná", "Tocantins"),
                                pd("Qual é o menor estado do Brasil em área?", 25, "Sergipe", "Alagoas", "Rio de Janeiro", "Espírito Santo"),
                                pd("Qual é a capital do Ceará?", 20, "Fortaleza", "Recife", "Natal", "São Luís"),
                                pd("Qual estado tem mais habitantes?", 20, "São Paulo", "Minas Gerais", "Rio de Janeiro", "Bahia"),
                                pd("Qual bioma é uma grande planície alagável?", 20, "Pantanal", "Caatinga", "Pampa", "Cerrado"),
                                pd("Qual oceano banha o litoral brasileiro?", 20, "Atlântico", "Pacífico", "Índico", "Ártico")),
                        desafio("Desafio: volta ao mundo", "Países, capitais e paisagens de todos os continentes.",
                                pd("Qual é o maior país do mundo em área?", 20, "Rússia", "Canadá", "China", "Brasil"),
                                pd("Qual é a capital da Argentina?", 20, "Buenos Aires", "Montevidéu", "Santiago", "Lima"),
                                pd("Qual é o maior deserto quente do mundo?", 25, "Saara", "Atacama", "Gobi", "Kalahari"),
                                pd("Em qual continente fica o Egito?", 20, "África", "Ásia", "Europa", "Oceania"),
                                pd("Qual é a montanha mais alta do mundo?", 20, "Everest", "K2", "Aconcágua", "Kilimanjaro"),
                                pd("Qual é a capital do Japão?", 20, "Tóquio", "Pequim", "Seul", "Osaka"),
                                pd("Qual país tem o formato de uma bota?", 20, "Itália", "Grécia", "Espanha", "Portugal"),
                                pd("Qual é o rio mais extenso da África?", 25, "Nilo", "Congo", "Níger", "Zambeze"),
                                pd("Qual é a capital da França?", 20, "Paris", "Lyon", "Roma", "Madri"),
                                pd("Qual linha imaginária divide a Terra em norte e sul?", 25, "Equador", "Greenwich",
                                        "Trópico de Câncer", "Trópico de Capricórnio"))));
    }

    // Atalhos para montar o conteúdo

    private static AtividadeModelo atividade(String titulo, String descricao, TipoAtividade tipo, int pontuacao,
                                             PerguntaModelo... perguntas) {
        return new AtividadeModelo(titulo, descricao, tipo, pontuacao, List.of(perguntas));
    }

    // A primeira alternativa é a correta (a ordem de exibição é embaralhada no seeder)
    private static PerguntaModelo quiz(String enunciado, String correta, String... erradas) {
        List<AlternativaModelo> alternativas = new ArrayList<>();
        alternativas.add(new AlternativaModelo(correta, true, null));
        for (String errada : erradas) {
            alternativas.add(new AlternativaModelo(errada, false, null));
        }
        return new PerguntaModelo(enunciado, alternativas);
    }

    private static PerguntaModelo vf(String afirmacao, boolean verdadeira) {
        return new PerguntaModelo(afirmacao, List.of(
                new AlternativaModelo("Verdadeiro", verdadeira, null),
                new AlternativaModelo("Falso", !verdadeira, null)));
    }

    // Item da coluna A com o seu par da coluna B
    private static PerguntaModelo par(String itemA, String itemB) {
        return new PerguntaModelo(itemA, List.of(new AlternativaModelo(itemB, true, null)));
    }

    // Itens já na ordem correta
    private static PerguntaModelo ordenar(String enunciado, String... itens) {
        List<AlternativaModelo> alternativas = new ArrayList<>();
        for (int i = 0; i < itens.length; i++) {
            alternativas.add(new AlternativaModelo(itens[i], true, i + 1));
        }
        return new PerguntaModelo(enunciado, alternativas);
    }

    // Uma palavra correta para cada lacuna, na ordem das lacunas, mais as palavras extras
    private static PerguntaModelo lacunas(String texto, List<String> corretas, String... extras) {
        List<AlternativaModelo> alternativas = new ArrayList<>();
        for (int i = 0; i < corretas.size(); i++) {
            alternativas.add(new AlternativaModelo(corretas.get(i), true, i + 1));
        }
        for (String extra : extras) {
            alternativas.add(new AlternativaModelo(extra, false, null));
        }
        return new PerguntaModelo(texto, alternativas);
    }

    private static DesafioModelo desafio(String titulo, String descricao, PerguntaDesafioModelo... perguntas) {
        return new DesafioModelo(titulo, descricao, List.of(perguntas));
    }

    private static PerguntaDesafioModelo pd(String enunciado, int tempo, String correta, String... erradas) {
        return new PerguntaDesafioModelo(enunciado, tempo, correta, List.of(erradas));
    }
}
