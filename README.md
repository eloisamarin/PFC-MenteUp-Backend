## 1. Identificação
- **Aluno(s):** Eloisa Marin - RA 11231104559
  Nikolas Alves da Silva - RA 11231104116
- **Projeto (PFC):** MenteUp
- **Branch:** feat/testes-integracao

## 2. Resumo da entrega
UsuarioController, TurmaController e AtividadeController (incluindo o progresso de atividade) passaram a ter testes de integração em todos os 21 endpoints, com requisições HTTP reais passando por segurança JWT, controller, service, repository e banco H2. Os testes garantem status, corpo da resposta, efeito persistido, regras de permissão por perfil e o fluxo que cruza turma, aluno, atividade, questionário e progresso.

## 3. Configuração do ambiente de teste
- **Estratégia:** @SpringBootTest RANDOM_PORT + RestClient
- **Banco de teste:** H2 em memória
- **Arquivo de configuração:** src/test/resources/application-test.properties
- **Isolamento de dados:** banco próprio dos testes de integração (menteup_integracao, ddl-auto=create-drop) e e-mails e nomes únicos com System.nanoTime(); cada teste cria seus dados pelos próprios endpoints, com helpers da classe IntegracaoBase
- **Componentes mockados e motivo:** EmailService, com @MockitoBean, por ser serviço externo (nenhum e-mail real é enviado). O Firebase, também externo, fica desligado pela propriedade firebase.enabled=false. Controller, service, repository, segurança e banco são reais.

## 4. Matriz de endpoints cobertos
| # | Método | Rota | Controller | Caminho feliz (status) | Erro(s) (status) | Efeito verificado |
|---|---|---|---|---|---|---|
| 1 | POST | /usuarios/cadastrar | UsuarioController | 201 | 400 | Usuário gravado com senha criptografada |
| 2 | GET | /usuarios/all | UsuarioController | 200 | - | - |
| 3 | GET | /usuarios/{id} | UsuarioController | 200 | 401, 404 | - |
| 4 | PUT | /usuarios/atualizar | UsuarioController | 200 | 400, 401, 404 | Nome e e-mail novos gravados |
| 5 | POST | /usuarios/logar | UsuarioController | 200 | 401 | Código 2FA gravado e enviado por e-mail |
| 6 | POST | /usuarios/validar-2fa | UsuarioController | 200 | 401 | Código fica inutilizado após o uso |
| 7 | POST | /usuarios/fcm-token | UsuarioController | 200 | 400, 401 | Token gravado no usuário |
| 8 | POST | /turmas | TurmaController | 201 | 400, 401, 403 | Turma existe no banco |
| 9 | GET | /turmas/all | TurmaController | 200 | 401 | - |
| 10 | GET | /turmas/{id} | TurmaController | 200 | 404 | - |
| 11 | GET | /turmas/nome/{nome} | TurmaController | 200 | - | - |
| 12 | POST | /turmas/{turmaId}/alunos/{alunoId} | TurmaController | 204 | 400, 403, 404, 409 | Matrícula gravada, sem duplicar |
| 13 | PUT | /turmas | TurmaController | 200 | 400, 403, 404 | Nome novo gravado, alunos mantidos |
| 14 | DELETE | /turmas/{id} | TurmaController | 204 | 403, 404 | GET seguinte retorna 404; atividades removidas |
| 15 | POST | /atividades | AtividadeController | 201 | 400, 401 | Atividade existe no banco |
| 16 | GET | /atividades | AtividadeController | 200 | - | - |
| 17 | GET | /atividades/{id} | AtividadeController | 200 | 403, 404 | - |
| 18 | GET | /atividades/titulo/{titulo} | AtividadeController | 200 | - | - |
| 19 | PUT | /atividades | AtividadeController | 200 | 400, 404 | Status e título novos gravados, perguntas mantidas |
| 20 | PUT | /atividades/{id}/progresso | AtividadeController | 200 | 400, 401, 403, 404, 409 | Progresso gravado e devolvido na consulta da atividade |
| 21 | DELETE | /atividades/{id} | AtividadeController | 204 | 401, 404, 409 | GET seguinte retorna 404 |

**Total de endpoints cobertos:** 21

## 5. Cenários de integração implementados
| # | Endpoint | Cenário | Tipo | Status | Verificado além do status | Arquivo de teste | Método de teste |
|---|---|---|---|---|---|---|---|
| 1 | POST /usuarios/cadastrar | Dados válidos | Feliz | 201 | Corpo, Content-Type, banco, senha criptografada | UsuarioControllerIntegrationTest | cadastrar_comDadosValidos_deveRetornar201ComIdESenhaCriptografada |
| 2 | POST /usuarios/cadastrar | E-mail já cadastrado | Erro | 400 | Registro original inalterado no banco | UsuarioControllerIntegrationTest | cadastrar_comEmailDuplicado_deveRetornar400ENaoAlterarOOriginal |
| 3 | POST /usuarios/cadastrar | E-mail inválido e senha curta | Erro | 400 | codigo e campos inválidos | UsuarioControllerIntegrationTest | cadastrar_comEmailInvalidoESenhaCurta_deveRetornar400ComCamposInvalidos |
| 4 | POST /usuarios/cadastrar | JSON malformado | Erro | 400 | codigo REQUISICAO_INVALIDA | UsuarioControllerIntegrationTest | cadastrar_comJsonMalformado_deveRetornar400ComCodigoRequisicaoInvalida |
| 5 | GET /usuarios/{id} | Id existente | Feliz | 200 | id e e-mail | UsuarioControllerIntegrationTest | buscarPorId_comIdExistente_deveRetornar200ComOsDados |
| 6 | GET /usuarios/{id} | Id inexistente | Erro | 404 | - | UsuarioControllerIntegrationTest | buscarPorId_comIdInexistente_deveRetornar404 |
| 7 | GET /usuarios/{id} | Sem token | Segurança | 401 | - | UsuarioControllerIntegrationTest | buscarPorId_semToken_deveRetornar401 |
| 8 | GET /usuarios/{id} | Token inválido | Segurança | 401 | - | UsuarioControllerIntegrationTest | buscarPorId_comTokenInvalido_deveRetornar401 |
| 9 | GET /usuarios/all | Com token | Feliz | 200 | Lista contém os usuários cadastrados | UsuarioControllerIntegrationTest | listarTodos_comToken_deveRetornar200ComUsuarioCadastrado |
| 10 | PUT /usuarios/atualizar | Dados válidos | Persistência | 200 | Corpo e dados novos no banco | UsuarioControllerIntegrationTest | atualizar_comDadosValidos_deveRetornar200EGravarOsNovosDados |
| 11 | PUT /usuarios/atualizar | Id inexistente | Erro | 404 | - | UsuarioControllerIntegrationTest | atualizar_comIdInexistente_deveRetornar404 |
| 12 | PUT /usuarios/atualizar | E-mail de outro usuário | Erro | 400 | mensagem e e-mail inalterado no banco | UsuarioControllerIntegrationTest | atualizar_comEmailDeOutroUsuario_deveRetornar400ComMensagem |
| 13 | PUT /usuarios/atualizar | Sem token | Segurança | 401 | - | UsuarioControllerIntegrationTest | atualizar_semToken_deveRetornar401 |
| 14 | POST /usuarios/logar | Credenciais válidas | Feliz | 200 | Sem token na resposta, código 2FA enviado por e-mail | UsuarioControllerIntegrationTest | logar_comCredenciaisValidas_deveRetornar200SemTokenEEnviarCodigo2FA |
| 15 | POST /usuarios/logar | Senha errada | Segurança | 401 | Nenhum e-mail enviado | UsuarioControllerIntegrationTest | logar_comSenhaErrada_deveRetornar401ENaoEnviarCodigo |
| 16 | POST /usuarios/validar-2fa | Código correto | Feliz | 200 | id, tipoUsuario, token acessa rota protegida | UsuarioControllerIntegrationTest | validar2fa_comCodigoCorreto_deveRetornar200ComTokenQueAcessaRotaProtegida |
| 17 | POST /usuarios/validar-2fa | Código errado | Segurança | 401 | - | UsuarioControllerIntegrationTest | validar2fa_comCodigoErrado_deveRetornar401 |
| 18 | POST /usuarios/validar-2fa | Código já utilizado | Segurança | 401 | - | UsuarioControllerIntegrationTest | validar2fa_comCodigoJaUtilizado_deveRetornar401 |
| 19 | POST /usuarios/fcm-token | Token válido | Persistência | 200 | Mensagem e token gravado no banco | UsuarioControllerIntegrationTest | registrarFcmToken_comTokenValido_deveRetornar200EGravarNoUsuario |
| 20 | POST /usuarios/fcm-token | Token em branco | Erro | 400 | codigo e campo inválido | UsuarioControllerIntegrationTest | registrarFcmToken_comTokenEmBranco_deveRetornar400ComCampoInvalido |
| 21 | POST /usuarios/fcm-token | Sem token de acesso | Segurança | 401 | - | UsuarioControllerIntegrationTest | registrarFcmToken_semToken_deveRetornar401 |
| 22 | POST /turmas | Professor com permissão | Segurança | 201 | id, nome e banco | TurmaControllerIntegrationTest | criar_comoProfessor_deveRetornar201ComIdEGravarNoBanco |
| 23 | POST /turmas | Administrador com permissão | Segurança | 201 | nome | TurmaControllerIntegrationTest | criar_comoAdministrador_deveRetornar201 |
| 24 | POST /turmas | Aluno sem permissão | Segurança | 403 | codigo, mensagem e nada gravado | TurmaControllerIntegrationTest | criar_comoAluno_deveRetornar403ComCodigoENaoGravar |
| 25 | POST /turmas | Sem token | Segurança | 401 | Nada gravado | TurmaControllerIntegrationTest | criar_semToken_deveRetornar401ENaoGravar |
| 26 | POST /turmas | Nome em branco | Erro | 400 | codigo e mensagem do campo | TurmaControllerIntegrationTest | criar_comNomeEmBranco_deveRetornar400ComCampoInvalido |
| 27 | POST /turmas | Requisição vinda do frontend | Feliz | 201 | Header Access-Control-Allow-Origin | TurmaControllerIntegrationTest | criar_comOrigemDoFrontend_deveRetornarHeaderCors |
| 28 | OPTIONS /turmas | Preflight CORS sem token | Segurança | 200 | Header Access-Control-Allow-Methods | TurmaControllerIntegrationTest | preflightCors_semToken_deveLiberarOMetodoParaOFrontend |
| 29 | GET /turmas/{id} | Id existente | Feliz | 200 | id e nome | TurmaControllerIntegrationTest | buscarPorId_comIdExistente_deveRetornar200ComOsDados |
| 30 | GET /turmas/{id} | Id inexistente | Erro | 404 | - | TurmaControllerIntegrationTest | buscarPorId_comIdInexistente_deveRetornar404 |
| 31 | GET /turmas/all | Com token | Feliz | 200 | Lista contém a turma criada | TurmaControllerIntegrationTest | listarTodas_comToken_deveRetornar200ComTurmaCriada |
| 32 | GET /turmas/all | Sem token | Segurança | 401 | - | TurmaControllerIntegrationTest | listarTodas_semToken_deveRetornar401 |
| 33 | GET /turmas/nome/{nome} | Nome em maiúsculas | Feliz | 200 | Devolve a turma | TurmaControllerIntegrationTest | buscarPorNome_ignorandoMaiusculas_deveRetornar200ComATurma |
| 34 | GET /turmas/nome/{nome} | Sem resultado | Feliz | 200 | Lista vazia | TurmaControllerIntegrationTest | buscarPorNome_semResultado_deveRetornar200ComListaVazia |
| 35 | POST /turmas/{turmaId}/alunos/{alunoId} | Professor vincula aluno | Relacionamento | 204 | Matrícula no banco e aluno no GET da turma | TurmaControllerIntegrationTest | vincularAluno_comoProfessor_deveRetornar204EMatricularNoBanco |
| 36 | POST /turmas/{turmaId}/alunos/{alunoId} | Vínculo repetido | Erro | 409 | codigo e matrícula sem duplicar | TurmaControllerIntegrationTest | vincularAluno_duasVezes_deveRetornar409ComCodigoESemDuplicar |
| 37 | POST /turmas/{turmaId}/alunos/{alunoId} | Usuário não é aluno | Erro | 400 | codigo e nada gravado | TurmaControllerIntegrationTest | vincularAluno_comUsuarioQueNaoEAluno_deveRetornar400ComCodigo |
| 38 | POST /turmas/{turmaId}/alunos/{alunoId} | Turma inexistente | Erro | 404 | codigo | TurmaControllerIntegrationTest | vincularAluno_comTurmaInexistente_deveRetornar404ComCodigo |
| 39 | POST /turmas/{turmaId}/alunos/{alunoId} | Aluno inexistente | Erro | 404 | codigo | TurmaControllerIntegrationTest | vincularAluno_comAlunoInexistente_deveRetornar404ComCodigo |
| 40 | POST /turmas/{turmaId}/alunos/{alunoId} | Aluno sem permissão | Segurança | 403 | codigo e nada gravado | TurmaControllerIntegrationTest | vincularAluno_comoAluno_deveRetornar403ENaoMatricular |
| 41 | PUT /turmas | Professor altera o nome | Persistência | 200 | Nome novo no banco e alunos mantidos | TurmaControllerIntegrationTest | atualizar_comoProfessor_deveRetornar200EManterOsAlunos |
| 42 | PUT /turmas | Sem id | Erro | 400 | codigo | TurmaControllerIntegrationTest | atualizar_semId_deveRetornar400ComCodigo |
| 43 | PUT /turmas | Id inexistente | Erro | 404 | codigo | TurmaControllerIntegrationTest | atualizar_comIdInexistente_deveRetornar404ComCodigo |
| 44 | PUT /turmas | Aluno sem permissão | Segurança | 403 | codigo e nome inalterado | TurmaControllerIntegrationTest | atualizar_comoAluno_deveRetornar403ENaoAlterarONome |
| 45 | DELETE /turmas/{id} | Administrador exclui | Persistência | 204 | GET seguinte retorna 404 | TurmaControllerIntegrationTest | excluir_comoAdministrador_deveRetornar204ETurmaDeixaDeExistir |
| 46 | DELETE /turmas/{id} | Turma com atividade e aluno | Relacionamento | 204 | Atividade removida, matrícula removida, aluno mantido | TurmaControllerIntegrationTest | excluir_turmaComAtividadeEAluno_deveRemoverAAtividadeEManterOAluno |
| 47 | DELETE /turmas/{id} | Id inexistente | Erro | 404 | codigo | TurmaControllerIntegrationTest | excluir_comIdInexistente_deveRetornar404ComCodigo |
| 48 | DELETE /turmas/{id} | Aluno sem permissão | Segurança | 403 | codigo e turma mantida | TurmaControllerIntegrationTest | excluir_comoAluno_deveRetornar403ETurmaContinuaExistindo |
| 49 | POST /atividades | Dados válidos | Feliz | 201 | Corpo, turma e banco | AtividadeControllerIntegrationTest | criar_comDadosValidos_deveRetornar201ComIdETurmaEGravarNoBanco |
| 50 | POST /atividades | Turma inexistente | Erro | 400 | mensagem e nada gravado | AtividadeControllerIntegrationTest | criar_comTurmaInexistente_deveRetornar400ComMensagemENaoGravar |
| 51 | POST /atividades | Título curto | Erro | 400 | codigo e campo inválido | AtividadeControllerIntegrationTest | criar_comTituloCurto_deveRetornar400ComCampoInvalido |
| 52 | POST /atividades | Sem turma | Erro | 400 | mensagem | AtividadeControllerIntegrationTest | criar_semTurma_deveRetornar400ENaoErroInterno |
| 53 | POST /atividades | Sem token | Segurança | 401 | Nada gravado | AtividadeControllerIntegrationTest | criar_semToken_deveRetornar401ENaoGravar |
| 54 | GET /atividades | Com token | Feliz | 200 | Lista contém a atividade criada | AtividadeControllerIntegrationTest | listarTodas_comToken_deveRetornar200ComAtividadeCriada |
| 55 | GET /atividades/titulo/{titulo} | Título em maiúsculas | Feliz | 200 | Devolve a atividade | AtividadeControllerIntegrationTest | buscarPorTitulo_ignorandoMaiusculas_deveRetornar200ComAAtividade |
| 56 | GET /atividades/titulo/{titulo} | Sem resultado | Feliz | 200 | Lista vazia | AtividadeControllerIntegrationTest | buscarPorTitulo_semResultado_deveRetornar200ComListaVazia |
| 57 | GET /atividades/{id} | Professor vê rascunho | Feliz | 200 | id, status e perguntas | AtividadeControllerIntegrationTest | buscarPorId_comoProfessor_deveRetornar200ComRascunhoEPerguntas |
| 58 | GET /atividades/{id} | Id inexistente | Erro | 404 | codigo | AtividadeControllerIntegrationTest | buscarPorId_comIdInexistente_deveRetornar404ComCodigo |
| 59 | GET /atividades/{id} | Aluno fora da turma | Segurança | 403 | codigo | AtividadeControllerIntegrationTest | buscarPorId_comAlunoForaDaTurma_deveRetornar403ComCodigo |
| 60 | GET /atividades/{id} | Aluno da turma em rascunho | Erro | 404 | codigo | AtividadeControllerIntegrationTest | buscarPorId_comAlunoDaTurmaEmRascunho_deveRetornar404 |
| 61 | GET /atividades/{id} | Aluno da turma em atividade publicada | Relacionamento | 200 | Perguntas sem expor o gabarito | AtividadeControllerIntegrationTest | buscarPorId_comAlunoDaTurmaEmPublicada_deveRetornar200SemExporGabarito |
| 62 | PUT /atividades | Publicar atividade | Persistência | 200 | Corpo, status e título novos no banco | AtividadeControllerIntegrationTest | atualizar_dePendenteParaEmAndamento_deveRetornar200EGravarOStatusNovo |
| 63 | PUT /atividades | Atividade com questionário | Relacionamento | 200 | Perguntas preservadas no GET seguinte | AtividadeControllerIntegrationTest | atualizar_atividadeComQuestionario_devePreservarAsPerguntas |
| 64 | PUT /atividades | Id inexistente | Erro | 404 | codigo | AtividadeControllerIntegrationTest | atualizar_comIdInexistente_deveRetornar404ComCodigo |
| 65 | PUT /atividades | Sem id | Erro | 400 | mensagem | AtividadeControllerIntegrationTest | atualizar_semId_deveRetornar400ENaoErroInterno |
| 66 | PUT /atividades | Sem turma | Erro | 400 | mensagem e turma inalterada no banco | AtividadeControllerIntegrationTest | atualizar_semTurma_deveRetornar400ENaoAlterar |
| 67 | PUT /atividades | Turma inexistente | Erro | 400 | mensagem e turma inalterada no banco | AtividadeControllerIntegrationTest | atualizar_comTurmaInexistente_deveRetornar400ENaoAlterar |
| 68 | DELETE /atividades/{id} | Id existente | Persistência | 204 | GET seguinte retorna 404 e banco vazio | AtividadeControllerIntegrationTest | excluir_comIdExistente_deveRetornar204EAtividadeDeixaDeExistir |
| 69 | DELETE /atividades/{id} | Atividade com progresso de aluno | Relacionamento | 409 | codigo e atividade mantida | AtividadeControllerIntegrationTest | excluir_atividadeComProgressoDeAluno_deveRetornar409EManterOsDados |
| 70 | DELETE /atividades/{id} | Id inexistente | Erro | 404 | - | AtividadeControllerIntegrationTest | excluir_comIdInexistente_deveRetornar404 |
| 71 | DELETE /atividades/{id} | Sem token | Segurança | 401 | Atividade mantida | AtividadeControllerIntegrationTest | excluir_semToken_deveRetornar401EAtividadeContinuaExistindo |
| 72 | PUT /atividades/{id}/progresso | Primeira resposta | Persistência | 200 | Percentual, contagens e progresso no banco | ProgressoAtividadeIntegrationTest | salvarProgresso_primeiraResposta_deveRetornar200Com50PorCentoEGravarNoBanco |
| 73 | PUT /atividades/{id}/progresso | Todas as perguntas respondidas | Feliz | 200 | Percentual 100 e contagens | ProgressoAtividadeIntegrationTest | salvarProgresso_todasAsPerguntas_deveRetornar200Com100PorCento |
| 74 | PUT /atividades/{id}/progresso | Mesma pergunta respondida de novo | Persistência | 200 | Alternativa trocada, sem duplicar a resposta | ProgressoAtividadeIntegrationTest | salvarProgresso_mesmaPerguntaDeNovo_deveTrocarAAlternativaSemDuplicar |
| 75 | PUT /atividades/{id}/progresso | Atividade inexistente | Erro | 404 | codigo | ProgressoAtividadeIntegrationTest | salvarProgresso_comAtividadeInexistente_deveRetornar404ComCodigo |
| 76 | PUT /atividades/{id}/progresso | Professor tenta responder | Segurança | 403 | codigo e nada gravado | ProgressoAtividadeIntegrationTest | salvarProgresso_comoProfessor_deveRetornar403ComCodigoENaoGravar |
| 77 | PUT /atividades/{id}/progresso | Sem token | Segurança | 401 | - | ProgressoAtividadeIntegrationTest | salvarProgresso_semToken_deveRetornar401 |
| 78 | PUT /atividades/{id}/progresso | Sem alternativa | Erro | 400 | codigo e mensagem do campo | ProgressoAtividadeIntegrationTest | salvarProgresso_semAlternativa_deveRetornar400ComCampoInvalido |
| 79 | PUT /atividades/{id}/progresso | Pergunta de outra atividade | Erro | 400 | codigo | ProgressoAtividadeIntegrationTest | salvarProgresso_comPerguntaDeOutraAtividade_deveRetornar400ComCodigo |
| 80 | PUT /atividades/{id}/progresso | Alternativa de outra pergunta | Erro | 400 | codigo | ProgressoAtividadeIntegrationTest | salvarProgresso_comAlternativaDeOutraPergunta_deveRetornar400ComCodigo |
| 81 | PUT /atividades/{id}/progresso | Atividade já concluída pelo aluno | Erro | 409 | codigo | ProgressoAtividadeIntegrationTest | salvarProgresso_aposConcluirAAtividade_deveRetornar409ComCodigo |
| 82 | PUT /atividades/{id}/progresso | Aluno fora da turma | Segurança | 403 | codigo e nada gravado | ProgressoAtividadeIntegrationTest | salvarProgresso_comAlunoForaDaTurma_deveRetornar403ComCodigoENaoGravar |
| 83 | PUT /atividades/{id}/progresso | Atividade em rascunho | Erro | 404 | codigo e nada gravado | ProgressoAtividadeIntegrationTest | salvarProgresso_emAtividadeRascunho_deveRetornar404ENaoGravar |
| 84 | PUT /atividades/{id}/progresso | Atividade encerrada | Erro | 409 | codigo e nada gravado | ProgressoAtividadeIntegrationTest | salvarProgresso_emAtividadeEncerrada_deveRetornar409ENaoGravar |
| 85 | PUT /atividades/{id}/progresso | Fluxo: turma, aluno, atividade, questionário e progresso | Relacionamento | 200 | Progresso em GET /atividades/{id} e GET /alunos/me/atividades | ProgressoAtividadeIntegrationTest | fluxoCompleto_turmaAlunoAtividadeEProgresso_deveAparecerNaAtividadeENaListaDoAluno |

Tipo: Feliz | Erro | Segurança | Persistência | Relacionamento
**Total de cenários de integração:** 85

## 6. Arquivos criados ou alterados
| Arquivo (caminho completo) | Criado / Alterado | Qtd. de testes |
|---|---|---|
| Projeto/menteup/src/test/java/com/umc/menteup/integration/IntegracaoBase.java | Criado | 0 |
| Projeto/menteup/src/test/java/com/umc/menteup/integration/UsuarioControllerIntegrationTest.java | Criado | 21 |
| Projeto/menteup/src/test/java/com/umc/menteup/integration/TurmaControllerIntegrationTest.java | Criado | 27 |
| Projeto/menteup/src/test/java/com/umc/menteup/integration/AtividadeControllerIntegrationTest.java | Criado | 23 |
| Projeto/menteup/src/test/java/com/umc/menteup/integration/ProgressoAtividadeIntegrationTest.java | Criado | 14 |
| Projeto/menteup/src/test/resources/application-test.properties | Criado | 0 |
| Projeto/menteup/src/test/java/com/umc/menteup/MenteupApplicationTests.java | Alterado | 0 |
| Projeto/menteup/src/test/java/com/umc/menteup/repository/TurmaRepositoryTest.java | Alterado | 0 |
| Projeto/menteup/src/test/java/com/umc/menteup/service/ProgressoAtividadeServiceTest.java | Alterado | 0 |
| Projeto/menteup/src/main/java/com/umc/menteup/controller/AtividadeController.java | Alterado | 0 |
| Projeto/menteup/src/main/java/com/umc/menteup/service/ProgressoAtividadeService.java | Alterado | 0 |

**Total de arquivos de teste:** 9   |   **Total de testes de integração:** 85

## 7. Como executar os testes
```
cd Projeto/menteup && ./mvnw clean test
```

## 8. Evidências
- **Resultado da execução:** `Tests run: 202, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS` na suíte completa. Dentro dela, os testes desta entrega: `Tests run: 21` em UsuarioControllerIntegrationTest, `Tests run: 27` em TurmaControllerIntegrationTest, `Tests run: 23` em AtividadeControllerIntegrationTest e `Tests run: 14` em ProgressoAtividadeIntegrationTest, todos com `Failures: 0, Errors: 0`.
- **Link do CI (se houver):** não se aplica

## 9. Decisões e dificuldades
- **Segurança:** O endpoint POST /turmas cobre os três casos: sem token retorna 401 (criar_semToken_deveRetornar401ENaoGravar), aluno sem permissão retorna 403 (criar_comoAluno_deveRetornar403ComCodigoENaoGravar) e professor com permissão retorna 201 (criar_comoProfessor_deveRetornar201ComIdEGravarNoBanco). Os tokens são obtidos pelos endpoints reais de login e 2FA, com e-mail único por execução. Há ainda testes de 401 e 403 em outros endpoints de usuários, turmas, atividades e progresso, listados na seção 5.
- **Bugs encontrados pelos testes (se houver):** (1) POST /atividades sem turma e PUT /atividades sem id retornavam 500, por NullPointerException e InvalidDataAccessApiUsageException; PUT /atividades sem turma também retornava 500. Agora os três casos retornam 400 com mensagem. Correção no commit [hash do commit]. (2) PUT /atividades/{id}/progresso não aplicava a regra de matrícula nem a de status: aluno fora da turma gravava progresso (200), e o mesmo ocorria em atividade em rascunho ou encerrada. Agora retorna 403, 404 e 409, com as mesmas regras de GET /atividades/{id} e POST /atividades/{id}/respostas. Correção no commit [hash do commit].
- **Dificuldades:** (1) A API não devolve o header Location, então o header verificado foi o de CORS (Access-Control-Allow-Origin e Access-Control-Allow-Methods), do qual o frontend depende, além do Content-Type; o comportamento de produção não foi alterado para isso. (2) O @ActiveProfiles("test") não apontava para nenhum arquivo: a configuração estava em src/test/resources/application.properties, que foi renomeado para application-test.properties e passou a sobrescrever todas as variáveis de ambiente; por isso MenteupApplicationTests e TurmaRepositoryTest passaram a declarar o perfil. (3) As classes antigas TurmaControllerTest e UsuarioControllerTest fazem deleteAll, o que colidiria com os dados relacionados dos novos testes; a solução foi dar um banco H2 próprio aos testes de integração. (4) A correção do progresso adicionou o MatriculaService ao construtor de ProgressoAtividadeService, o que exigiu declarar esse mock no ProgressoAtividadeServiceTest. (5) O login exige 2FA por e-mail: como o EmailService é mockado, os helpers leem o código gerado no Codigo2FARepository.


