# 🎓 MenteUp

## Plataforma Web de Gamificação Educacional

O **MenteUp** é uma plataforma web de gamificação educacional que tem como objetivo incentivar o engajamento e a participação dos estudantes no processo de aprendizagem por meio de recursos de gamificação, como atividades, questionários, pontuação e ranking.

Além disso, o sistema permite que professores acompanhem o desempenho e a participação dos estudantes e disponibiliza um perfil administrativo para gerenciamento de usuários, logs de acesso e auditoria das ações realizadas na plataforma.

---

# 🎯 Objetivo

Desenvolver uma plataforma educacional baseada em gamificação para incentivar o engajamento dos estudantes e auxiliar os professores no acompanhamento do processo de aprendizagem.

## Objetivos específicos

- Promover a resolução de atividades e desafios; 

- Permitir a criação e disponibilização de questionários pelos professores; 

- Atribuir pontuações aos estudantes de acordo com seu desempenho nas atividades; 

- Estabelecer um sistema de ranqueamento; 

- Converter pontos acumulados em conquistas. 

---

# 🚀 Funcionalidades

## 👨‍🎓 Estudante

- Cadastro e login.
- Visualização de atividades.
- Participação em questionários.
- Participação em desafios.
- Acúmulo de pontos.
- Visualização do ranking.
- Acompanhamento do progresso.
- Visualização do histórico de atividades.

## 👨‍🏫 Professor

- Cadastro e login.
- Criação de atividades.
- Criação de questionários.
- Edição e exclusão de atividades.
- Gerenciamento de turmas.
- Visualização dos estudantes.
- Acompanhamento do desempenho.
- Visualização do ranking.
- Consulta ao histórico de alterações das atividades.

## 🛡️ Administrador

- Gerenciamento de usuários.
- Gerenciamento de alunos e professores.
- Controle de status dos usuários.
- Gerenciamento de turmas.
- Visualização de logs de acesso.
- Visualização de logs de auditoria.
- Acompanhamento das ações realizadas no sistema.
- Controle de permissões de acesso.
---
## 1. Identificação
- **Aluna:** [Nome completo - RA: 11231104559]
- **Aluno:** [Nikolas Alves da Silva - RA: 11231104116]
- **Projeto (PFC):** MenteUp
- **Branch:** feat/testes-automatizados

## 2. Resumo da entrega
Testes automatizados (JUnit 5, Mockito e Spring Boot Test) para os módulos de Atividade, Questionário, Turma e Usuário do MenteUp. As regras de negócio de AtividadeService, ProgressoAtividadeService, QuestionarioService e ResultadoAtividadeService passaram a ter testes unitários isolados com mocks. Os endpoints de atividades, questionário, turmas e usuários e a persistência de turmas passaram a ter testes de integração com H2 em memória, autenticação JWT real e verificação de status e corpo das respostas.

## 3. Cenários de testes unitários implementados
| # | Classe testada | Método / regra | Cenário | Tipo | Arquivo de teste | Método de teste |
|---|----------------|----------------|---------|------|------------------|-----------------|
| 1 | AtividadeService | criarAtividade() | Atividade sem turma lança IllegalArgumentException com mensagem | Violação | AtividadeServiceTest | rejeitaSemTurma |
| 2 | AtividadeService | criarAtividade() | Turma sem id é rejeitada | Violação | AtividadeServiceTest | rejeitaTurmaSemId |
| 3 | AtividadeService | criarAtividade() | Turma inexistente lança exceção "Turma não encontrada." | Violação | AtividadeServiceTest | rejeitaTurmaInexistente |
| 4 | AtividadeService | criarAtividade() | Salva e notifica só os alunos com token FCM | Feliz | AtividadeServiceTest | salvaENotificaSomenteAlunosComToken |
| 5 | AtividadeService | criarAtividade() | Turma sem alunos: salva e não notifica ninguém | Limite | AtividadeServiceTest | turmaSemAlunosNaoNotifica |
| 6 | AtividadeService | listarTodas() | Delega a listagem ao repository | Feliz | AtividadeServiceTest | listarTodasDelegaParaORepository |
| 7 | AtividadeService | buscarPorId() | Retorna o Optional do repository | Feliz | AtividadeServiceTest | buscarPorIdRetornaOptionalDoRepository |
| 8 | AtividadeService | buscarPorTitulo() | Busca ignorando maiúsculas e minúsculas | Feliz | AtividadeServiceTest | buscarPorTituloUsaBuscaIgnorandoMaiusculas |
| 9 | AtividadeService | atualizarAtividade() | Atividade inexistente: retorna vazio e não salva | Violação | AtividadeServiceTest | naoExiste |
| 10 | AtividadeService | atualizarAtividade() | Copia os campos editáveis e salva | Feliz | AtividadeServiceTest | copiaCamposESalva |
| 11 | AtividadeService | deletarAtividade() | Atividade inexistente: retorna false e não apaga | Violação | AtividadeServiceTest | retornaFalseENaoApagaQuandoNaoExiste |
| 12 | AtividadeService | deletarAtividade() | Atividade existente: apaga e retorna true | Feliz | AtividadeServiceTest | apagaERetornaTrueQuandoExiste |
| 13 | AtividadeService | buscarQuestionario() | Usuário do token inexistente: 401 USUARIO_NAO_ENCONTRADO | Violação | AtividadeServiceTest | usuarioInexistente |
| 14 | AtividadeService | buscarQuestionario() | Atividade inexistente: 404 ATIVIDADE_NAO_ENCONTRADA | Violação | AtividadeServiceTest | atividadeInexistente |
| 15 | AtividadeService | buscarQuestionario() | Aluno não matriculado na turma: 403 ALUNO_FORA_DA_TURMA | Violação | AtividadeServiceTest | alunoForaDaTurma |
| 16 | AtividadeService | buscarQuestionario() | Aluno não enxerga atividade em rascunho (PENDENTE) | Violação | AtividadeServiceTest | alunoNaoVeRascunho |
| 17 | AtividadeService | buscarQuestionario() | Aluno que já concluiu recebe o resultado | Feliz | AtividadeServiceTest | alunoComResultado |
| 18 | AtividadeService | buscarQuestionario() | Aluno com atividade em andamento recebe o progresso (50%) | Feliz | AtividadeServiceTest | alunoComProgresso |
| 19 | AtividadeService | buscarQuestionario() | Aluno que não começou: resultado e progresso nulos | Limite | AtividadeServiceTest | alunoSemNada |
| 20 | AtividadeService | buscarQuestionario() | Professor vê rascunho sem as regras de aluno | Feliz | AtividadeServiceTest | professorVeRascunhoSemRegrasDeAluno |
| 21 | AtividadeService | listarParaAluno() | Usuário do token inexistente: 401 | Violação | AtividadeServiceTest | usuarioInexistente |
| 22 | AtividadeService | listarParaAluno() | Quem não é aluno recebe 403 SOMENTE_ALUNO | Violação | AtividadeServiceTest | naoAluno |
| 23 | AtividadeService | listarParaAluno() | Sem atividades visíveis: lista vazia e nenhuma consulta extra | Limite | AtividadeServiceTest | semAtividadesVisiveis |
| 24 | AtividadeService | listarParaAluno() | Rascunhos (PENDENTE) são excluídos da busca | Feliz | AtividadeServiceTest | rascunhoEhExcluidoDaBusca |
| 25 | AtividadeService | listarParaAluno() | Classifica em CONCLUIDA, EM_ANDAMENTO e PENDENTE mantendo a ordem | Feliz | AtividadeServiceTest | classificaCadaAtividade |
| 26 | AtividadeService | listarParaAluno() | Resultado tem prioridade sobre progresso na mesma atividade | Limite | AtividadeServiceTest | resultadoTemPrioridadeSobreProgresso |
| 27 | AtividadeService | listarParaAluno() | Parametrizado: 0 de 3 perguntas respondidas = 0% | Limite | AtividadeServiceTest | deveArredondarPercentualConformePerguntasRespondidas |
| 28 | AtividadeService | listarParaAluno() | Parametrizado: 1 de 3 perguntas respondidas = 33% | Limite | AtividadeServiceTest | deveArredondarPercentualConformePerguntasRespondidas |
| 29 | AtividadeService | listarParaAluno() | Parametrizado: 2 de 3 perguntas respondidas = 67% | Limite | AtividadeServiceTest | deveArredondarPercentualConformePerguntasRespondidas |
| 30 | AtividadeService | listarParaAluno() | Parametrizado: 3 de 3 perguntas respondidas = 100% | Limite | AtividadeServiceTest | deveArredondarPercentualConformePerguntasRespondidas |
| 31 | AtividadeService | listarParaAluno() | Atividade sem perguntas: 0%, sem divisão por zero | Limite | AtividadeServiceTest | atividadeSemPerguntasNaoDividePorZero |
| 32 | ProgressoAtividadeService | salvarResposta() | Usuário do token inexistente: 401 USUARIO_NAO_ENCONTRADO | Violação | ProgressoAtividadeServiceTest | usuarioInexistente |
| 33 | ProgressoAtividadeService | salvarResposta() | Professor tentando responder: 403 SOMENTE_ALUNO | Violação | ProgressoAtividadeServiceTest | professorNaoResponde |
| 34 | ProgressoAtividadeService | salvarResposta() | Atividade inexistente: 404 ATIVIDADE_NAO_ENCONTRADA | Violação | ProgressoAtividadeServiceTest | atividadeInexistente |
| 35 | ProgressoAtividadeService | salvarResposta() | Atividade já concluída: 409 ATIVIDADE_JA_RESPONDIDA | Violação | ProgressoAtividadeServiceTest | atividadeJaConcluida |
| 36 | ProgressoAtividadeService | salvarResposta() | Pergunta de outra atividade: 400 PERGUNTA_INVALIDA | Violação | ProgressoAtividadeServiceTest | perguntaDeOutraAtividade |
| 37 | ProgressoAtividadeService | salvarResposta() | Alternativa de outra pergunta: 400 ALTERNATIVA_INVALIDA | Violação | ProgressoAtividadeServiceTest | alternativaDeOutraPergunta |
| 38 | ProgressoAtividadeService | salvarResposta() | Atividade sem perguntas: 400 PERGUNTA_INVALIDA | Limite | ProgressoAtividadeServiceTest | atividadeSemPerguntas |
| 39 | ProgressoAtividadeService | salvarResposta() | Primeira resposta cria o progresso do aluno e salva | Feliz | ProgressoAtividadeServiceTest | criaProgressoComAResposta |
| 40 | ProgressoAtividadeService | salvarResposta() | Devolve o progresso calculado (1 de 2 = 50%) | Feliz | ProgressoAtividadeServiceTest | devolveProgresso |
| 41 | ProgressoAtividadeService | salvarResposta() | Registra INICIAR_ATIVIDADE e depois RESPONDER_QUESTAO | Feliz | ProgressoAtividadeServiceTest | registraIniciarEResponder |
| 42 | ProgressoAtividadeService | salvarResposta() | Nova resposta acrescenta no mesmo progresso (2 de 2 = 100%) | Feliz | ProgressoAtividadeServiceTest | acrescentaNoMesmoProgresso |
| 43 | ProgressoAtividadeService | salvarResposta() | Não registra INICIAR_ATIVIDADE de novo | Feliz | ProgressoAtividadeServiceTest | naoReiniciaAtividade |
| 44 | ProgressoAtividadeService | salvarResposta() | Mesma pergunta troca a alternativa sem duplicar a resposta | Limite | ProgressoAtividadeServiceTest | trocaAAlternativaSemDuplicar |
| 45 | ProgressoAtividadeService | salvarResposta() | Trocar a alternativa registra RESPONDER_QUESTAO e não INICIAR | Feliz | ProgressoAtividadeServiceTest | trocarAlternativaRegistraLog |
| 46 | ProgressoAtividadeService | salvarResposta() | Percentual arredondado (2 de 3 = 67%) | Limite | ProgressoAtividadeServiceTest | percentualArredondado |
| 47 | ProgressoAtividadeService | salvarResposta() | Log RESPONDER_QUESTAO leva atividadeId, perguntaId e alternativaId | Feliz | ProgressoAtividadeServiceTest | dadosDoLogDeResposta |
| 48 | QuestionarioService | cadastrarQuesitonario() | Atividade inexistente lança exceção "Atividade não encontrada", sem salvar nem registrar log | Violação | QuestionarioServiceTest | atividadeInexistente |
| 49 | QuestionarioService | cadastrarQuesitonario() | Cria perguntas e alternativas com vínculo pai/filho correto | Feliz | QuestionarioServiceTest | criaPerguntasEAlternativasVinculadas |
| 50 | QuestionarioService | cadastrarQuesitonario() | Mantém as perguntas existentes e acrescenta as novas no fim | Limite | QuestionarioServiceTest | acrescentaSemApagarAsExistentes |
| 51 | QuestionarioService | cadastrarQuesitonario() | Registra um log CRIAR_QUESTAO por pergunta criada | Feliz | QuestionarioServiceTest | umLogPorPerguntaCriada |
| 52 | QuestionarioService | cadastrarQuesitonario() | Log leva o id da atividade e o enunciado da pergunta | Feliz | QuestionarioServiceTest | dadosDoLog |
| 53 | QuestionarioService | cadastrarQuesitonario() | Falha no save propaga a exceção e não registra log de sucesso | Violação | QuestionarioServiceTest | saveFalhaNaoGeraLog |
| 54 | ResultadoAtividadeService | concluirAtividade() | Corrige as respostas e salva o resultado | Feliz | ResultadoAtividadeServiceTest | corrigeESalvaResultado |
| 55 | ResultadoAtividadeService | concluirAtividade() | Gabarito completo dá a pontuação total | Limite | ResultadoAtividadeServiceTest | gabaritoCompletoDaPontuacaoTotal |
| 56 | ResultadoAtividadeService | concluirAtividade() | Professor não pode responder | Violação | ResultadoAtividadeServiceTest | professorNaoPodeResponder |
| 57 | ResultadoAtividadeService | concluirAtividade() | Aluno fora da turma recebe 403 | Violação | ResultadoAtividadeServiceTest | alunoForaDaTurmaRecebe403 |
| 58 | ResultadoAtividadeService | concluirAtividade() | Segunda submissão recebe 409 | Violação | ResultadoAtividadeServiceTest | segundaSubmissaoRecebe409 |
| 59 | ResultadoAtividadeService | concluirAtividade() | Envio simultâneo barrado pela constraint do banco vira 409 | Limite | ResultadoAtividadeServiceTest | corridaBarradaPelaConstraintVira409 |
| 60 | ResultadoAtividadeService | concluirAtividade() | Atividade encerrada não aceita respostas | Violação | ResultadoAtividadeServiceTest | atividadeEncerradaNaoAceitaRespostas |
| 61 | ResultadoAtividadeService | concluirAtividade() | Rascunho não existe para o aluno | Violação | ResultadoAtividadeServiceTest | rascunhoNaoExisteParaOAluno |
| 62 | ResultadoAtividadeService | concluirAtividade() | Pergunta respondida duas vezes é rejeitada | Violação | ResultadoAtividadeServiceTest | perguntaRespondidaDuasVezesEhRejeitada |
| 63 | ResultadoAtividadeService | concluirAtividade() | Faltando resposta de alguma pergunta é rejeitado | Limite | ResultadoAtividadeServiceTest | faltandoPerguntaEhRejeitado |
| 64 | ResultadoAtividadeService | concluirAtividade() | Pergunta de outra atividade é rejeitada | Violação | ResultadoAtividadeServiceTest | perguntaDeOutraAtividadeEhRejeitada |
| 65 | ResultadoAtividadeService | concluirAtividade() | Alternativa de outra pergunta é rejeitada | Violação | ResultadoAtividadeServiceTest | alternativaDeOutraPerguntaEhRejeitada |

Tipo: Feliz | Violação | Limite
**Total de cenários unitários:** 65

## 4. Cenários de testes de integração implementados
| # | Camadas envolvidas | Cenário | Arquivo de teste | Método de teste | Recurso usado |
|---|--------------------|---------|------------------|-----------------|---------------|
| 1 | Repository+BD | Salva turma com aluno e recupera os mesmos dados do banco | TurmaRepositoryTest | deveSalvarERecuperarTurmaComAlunos | @DataJpaTest+H2 |
| 2 | Repository+BD | Consulta customizada contarMatricula devolve 1 para aluno matriculado | TurmaRepositoryTest | deveContarUmaMatriculaQuandoAlunoEstaNaTurma | @DataJpaTest+H2 |
| 3 | Repository+BD | Consulta customizada contarMatricula devolve 0 para aluno de fora | TurmaRepositoryTest | deveContarZeroMatriculasQuandoAlunoNaoEstaNaTurma | @DataJpaTest+H2 |
| 4 | Repository+BD | Busca de turma por nome ignorando maiúsculas | TurmaRepositoryTest | deveBuscarTurmaPorNomeIgnorandoMaiusculas | @DataJpaTest+H2 |
| 5 | Controller+Security+Service+BD | POST /turmas por professor retorna 201 com id e nome e grava no banco | TurmaControllerTest | professorDeveCriarTurma | @SpringBootTest+TestRestTemplate+H2+JWT |
| 6 | Controller+Security+Service+BD | POST /turmas por administrador retorna 201 | TurmaControllerTest | administradorDeveCriarTurma | @SpringBootTest+TestRestTemplate+H2+JWT |
| 7 | Controller+Security+Service+BD | POST /turmas por aluno retorna 403 com codigo SOMENTE_PROFESSOR_OU_ADMINISTRADOR | TurmaControllerTest | alunoNaoDeveCriarTurma | @SpringBootTest+TestRestTemplate+H2+JWT |
| 8 | Controller+Security | GET /turmas/all sem token retorna 401 | TurmaControllerTest | semTokenDeveRetornar401 | @SpringBootTest+TestRestTemplate+H2 |
| 9 | Controller+Security+Service+BD | Professor vincula aluno à turma e a matrícula fica no banco | TurmaControllerTest | professorDeveVincularAluno | @SpringBootTest+TestRestTemplate+H2+JWT |
| 10 | Controller+Security+Service+BD | Aluno não vincula alunos à turma (403) | TurmaControllerTest | alunoNaoDeveVincularAluno | @SpringBootTest+TestRestTemplate+H2+JWT |
| 11 | Controller+Security+Service+BD | Vincular o mesmo aluno duas vezes é rejeitado | TurmaControllerTest | naoDeveVincularAlunoDuasVezes | @SpringBootTest+TestRestTemplate+H2+JWT |
| 12 | Controller+Security+Service+BD | PUT /turmas atualiza o nome sem perder os alunos | TurmaControllerTest | professorDeveAtualizarNomeMantendoAlunos | @SpringBootTest+TestRestTemplate+H2+JWT |
| 13 | Controller+Security+Service+BD | Aluno não exclui turma (403) | TurmaControllerTest | alunoNaoDeveExcluirTurma | @SpringBootTest+TestRestTemplate+H2+JWT |
| 14 | Controller+Security+Service+BD | DELETE /turmas/{id} por administrador apaga a turma do banco | TurmaControllerTest | administradorDeveExcluirTurma | @SpringBootTest+TestRestTemplate+H2+JWT |
| 15 | Controller+Service+BD | POST /usuarios/cadastrar retorna 201 com id, nome e e-mail e grava no banco | UsuarioControllerTest | deveCadastrarUsuario | @SpringBootTest+TestRestTemplate+H2 |
| 16 | Controller+Service+BD | Cadastro com e-mail já existente retorna 400 | UsuarioControllerTest | naoDeveDuplicarUsuario | @SpringBootTest+TestRestTemplate+H2 |
| 17 | Controller+Security+Service+BD | PUT /usuarios/atualizar retorna 200 com nome e e-mail novos | UsuarioControllerTest | deveAtualizarUmUsuario | @SpringBootTest+TestRestTemplate+H2+JWT |
| 18 | Controller+Security+Service+BD | GET /usuarios/all retorna 200 com os usuários cadastrados | UsuarioControllerTest | deveListarTodosUsuarios | @SpringBootTest+TestRestTemplate+H2+JWT |
| 19 | Contexto Spring completo | Aplicação sobe com H2 e sem depender do Firebase | MenteupApplicationTests | deveSubirOContextoSemDependerDoFirebase | @SpringBootTest+H2 |
| 20 | Controller+Validação+ExceptionHandler | GET /atividades lista todas | AtividadeControllerTest | listaTodas | MockMvc standalone+Mockito |
| 21 | Controller+Validação+ExceptionHandler | GET /atividades?titulo= busca por título | AtividadeControllerTest | buscaPorTitulo | MockMvc standalone+Mockito |
| 22 | Controller+Validação+ExceptionHandler | Busca por título sem resultado retorna 200 com lista vazia | AtividadeControllerTest | buscaPorTituloSemResultado | MockMvc standalone+Mockito |
| 23 | Controller+Validação+ExceptionHandler | GET /atividades/{id} retorna 200 com o questionário | AtividadeControllerTest | retornaQuestionario | MockMvc standalone+Mockito |
| 24 | Controller+Validação+ExceptionHandler | GET /atividades/{id} retorna 404 ATIVIDADE_NAO_ENCONTRADA | AtividadeControllerTest | naoEncontrada | MockMvc standalone+Mockito |
| 25 | Controller+Validação+ExceptionHandler | GET /atividades/{id} retorna 403 ALUNO_FORA_DA_TURMA | AtividadeControllerTest | alunoForaDaTurma | MockMvc standalone+Mockito |
| 26 | Controller+Validação+ExceptionHandler | POST /atividades retorna 201 e registra log CRIAR_ATIVIDADE | AtividadeControllerTest | criaERegistraLog | MockMvc standalone+Mockito |
| 27 | Controller+Validação+ExceptionHandler | POST /atividades com turma inexistente retorna 400 | AtividadeControllerTest | turmaInexistente | MockMvc standalone+Mockito |
| 28 | Controller+Validação+ExceptionHandler | POST /atividades com título de menos de 5 caracteres retorna 400 VALIDACAO_FALHOU | AtividadeControllerTest | tituloCurto | MockMvc standalone+Mockito |
| 29 | Controller+Validação+ExceptionHandler | POST /atividades com pontuação menor que 1 retorna 400 VALIDACAO_FALHOU | AtividadeControllerTest | pontuacaoZero | MockMvc standalone+Mockito |
| 30 | Controller+Validação+ExceptionHandler | POST /atividades com JSON inválido retorna 400 REQUISICAO_INVALIDA | AtividadeControllerTest | jsonMalformado | MockMvc standalone+Mockito |
| 31 | Controller+Validação+ExceptionHandler | PUT de PENDENTE para EM_ANDAMENTO registra PUBLICAR_ATIVIDADE | AtividadeControllerTest | publicar | MockMvc standalone+Mockito |
| 32 | Controller+Validação+ExceptionHandler | PUT sem mudar status registra EDITAR_ATIVIDADE | AtividadeControllerTest | editarSemMudarStatus | MockMvc standalone+Mockito |
| 33 | Controller+Validação+ExceptionHandler | PUT de EM_ANDAMENTO para CONCLUIDO é edição, não publicação | AtividadeControllerTest | concluirNaoEPublicar | MockMvc standalone+Mockito |
| 34 | Controller+Validação+ExceptionHandler | PUT preserva as perguntas existentes | AtividadeControllerTest | preservaPerguntas | MockMvc standalone+Mockito |
| 35 | Controller+Validação+ExceptionHandler | PUT de atividade inexistente retorna 404 | AtividadeControllerTest | naoExiste | MockMvc standalone+Mockito |
| 36 | Controller+Validação+ExceptionHandler | PUT com nova turma inexistente retorna 400 | AtividadeControllerTest | turmaInexistente | MockMvc standalone+Mockito |
| 37 | Controller+Validação+ExceptionHandler | PUT /atividades/{id}/progresso retorna 200 com o progresso | AtividadeControllerTest | salvaResposta | MockMvc standalone+Mockito |
| 38 | Controller+Validação+ExceptionHandler | Progresso sem alternativaId retorna 400 VALIDACAO_FALHOU | AtividadeControllerTest | semAlternativa | MockMvc standalone+Mockito |
| 39 | Controller+Validação+ExceptionHandler | Progresso propaga erro de negócio do service (409) | AtividadeControllerTest | erroDeNegocioDoService | MockMvc standalone+Mockito |
| 40 | Controller+Validação+ExceptionHandler | DELETE /atividades/{id} retorna 204 e registra EXCLUIR_ATIVIDADE | AtividadeControllerTest | excluiERegistraLog | MockMvc standalone+Mockito |
| 41 | Controller+Validação+ExceptionHandler | DELETE de atividade inexistente retorna 404 | AtividadeControllerTest | inexistente | MockMvc standalone+Mockito |
| 42 | Controller+Validação+ExceptionHandler | POST /atividades/{id}/questionario retorna 200 com o questionário | QuestionarioControllerTest | cadastraEDevolveQuestionario | MockMvc standalone+Mockito |
| 43 | Controller+Validação+ExceptionHandler | Resposta não expõe o gabarito das alternativas | QuestionarioControllerTest | naoVazaGabarito | MockMvc standalone+Mockito |
| 44 | Controller+Validação+ExceptionHandler | Converte o JSON recebido no QuestionarioRequest | QuestionarioControllerTest | convertePayloadParaRequest | MockMvc standalone+Mockito |
| 45 | Controller+Validação+ExceptionHandler | Lista de perguntas vazia retorna 400 VALIDACAO_FALHOU | QuestionarioControllerTest | listaDePerguntasVazia | MockMvc standalone+Mockito |
| 46 | Controller+Validação+ExceptionHandler | Enunciado em branco retorna 400 VALIDACAO_FALHOU | QuestionarioControllerTest | enunciadoEmBranco | MockMvc standalone+Mockito |
| 47 | Controller+Validação+ExceptionHandler | Pergunta sem alternativas retorna 400 VALIDACAO_FALHOU | QuestionarioControllerTest | perguntaSemAlternativas | MockMvc standalone+Mockito |
| 48 | Controller+Validação+ExceptionHandler | Texto da alternativa vazio retorna 400 VALIDACAO_FALHOU | QuestionarioControllerTest | textoDaAlternativaVazio | MockMvc standalone+Mockito |
| 49 | Controller+Validação+ExceptionHandler | Alternativa sem o campo correta retorna 400 VALIDACAO_FALHOU | QuestionarioControllerTest | alternativaSemCampoCorreta | MockMvc standalone+Mockito |
| 50 | Controller+Validação+ExceptionHandler | JSON inválido retorna 400 REQUISICAO_INVALIDA | QuestionarioControllerTest | jsonMalformado | MockMvc standalone+Mockito |
| 51 | Controller+Validação+ExceptionHandler | Corpo ausente retorna 400 REQUISICAO_INVALIDA | QuestionarioControllerTest | semCorpo | MockMvc standalone+Mockito |
| 52 | Controller+Validação+ExceptionHandler | Propaga 404 ATIVIDADE_NAO_ENCONTRADA do service | QuestionarioControllerTest | propagaErroDeNegocio | MockMvc standalone+Mockito |

**Total de cenários de integração:** 52

## 5. Arquivos de teste criados ou alterados
| Arquivo (caminho completo) | Criado / Alterado | Qtd. de testes |
|----------------------------|-------------------|----------------|
| Projeto/menteup/src/test/java/com/umc/menteup/service/AtividadeServiceTest.java | Criado | 31 |
| Projeto/menteup/src/test/java/com/umc/menteup/service/ProgressoAtividadeServiceTest.java | Criado | 16 |
| Projeto/menteup/src/test/java/com/umc/menteup/service/QuestionarioServiceTest.java | Criado | 6 |
| Projeto/menteup/src/test/java/com/umc/menteup/service/ResultadoAtividadeServiceTest.java | Criado | 12 |
| Projeto/menteup/src/test/java/com/umc/menteup/repository/TurmaRepositoryTest.java | Criado | 4 |
| Projeto/menteup/src/test/java/com/umc/menteup/controller/AtividadeControllerTest.java | Criado | 22 |
| Projeto/menteup/src/test/java/com/umc/menteup/controller/QuestionarioControllerTest.java | Criado | 11 |
| Projeto/menteup/src/test/java/com/umc/menteup/controller/TurmaControllerTest.java | Criado | 10 |
| Projeto/menteup/src/test/java/com/umc/menteup/controller/UsuarioControllerTest.java | Criado | 4 |
| Projeto/menteup/src/test/java/com/umc/menteup/MenteupApplicationTests.java | Alterado | 1 |
| Projeto/menteup/src/test/java/com/umc/menteup/util/JwtHelper.java | Criado | 0 |
| Projeto/menteup/src/test/java/com/umc/menteup/util/TestBuilder.java | Criado | 0 |
| Projeto/menteup/src/test/resources/application.properties | Criado | 0 |

**Total de arquivos de teste:** 13  |  **Total de testes:** 117

## 6. Como executar os testes
```
cd Projeto/menteup && ./mvnw clean test
```

## 7. Evidências
- **Resultado da execução:** `Tests run: 117, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`
- **Link do CI (se houver):** não se aplica

## 8. Decisões e dificuldades
- **O que foi mockado e por quê:** 
  - Nos testes unitários dos services, os repositories, o LogSistemaService, o NotificacaoService e o MatriculaService foram mockados com Mockito. Assim cada regra de negócio é testada isolada, sem subir o Spring, sem banco e sem enviar notificação real; o verify (incluindo never()) confirma que nada é salvo nem registrado em log quando a regra bloqueia a operação. Nos testes de AtividadeController e QuestionarioController, os services foram mockados e o controller roda em MockMvc standalone, para validar status HTTP, Bean Validation, conversão de JSON e o tratamento de erros da API. Nos testes de Turma, Usuário e no MenteupApplicationTests a aplicação sobe completa com H2 em memória e JWT real; só o EmailService foi trocado por @MockitoBean, para não enviar e-mail real (inclusive o do 2FA). 
- **Bugs encontrados pelos testes (se houver):** nenhum
- **Dificuldades:** 
  - O FirebaseConfig impedia o contexto do Spring de subir nos testes por falta de credenciais, ele passou a depender da propriedade firebase.enabled, que continua ligada por padrão, então o comportamento de produção não mudou.
  - O login exige 2FA, então foi criado o JwtHelper, que lê o código gerado no Codigo2FARepository para obter o token nos testes.




---

