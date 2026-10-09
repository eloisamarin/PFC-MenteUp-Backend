package com.umc.menteup.service;

import com.umc.menteup.dto.AtividadeAlunoResponse;
import com.umc.menteup.dto.QuestionarioResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    private static final String USERNAME = "aluno@teste.com";
    private static final long ALUNO_ID = 7L;
    private static final long TURMA_ID = 5L;
    private static final long ATIVIDADE_ID = 10L;

    @Mock private AtividadeRepository atividadeRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private NotificacaoService notificacaoService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResultadoAtividadeRepository resultadoAtividadeRepository;
    @Mock private ProgressoAtividadeRepository progressoAtividadeRepository;
    @Mock private MatriculaService matriculaService;

    @InjectMocks
    private AtividadeService service;

    @Nested
    @DisplayName("criarAtividade")
    class CriarAtividade {

        @Test
        @DisplayName("rejeita atividade sem turma")
        void rejeitaSemTurma() {
            Atividade nova = atividade(null, StatusAtividade.PENDENTE, 0);
            nova.setTurma(null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.criarAtividade(nova)
            );

            assertEquals("A turma da atividade é obrigatória.", ex.getMessage());
            verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita atividade cuja turma não tem id")
        void rejeitaTurmaSemId() {
            Atividade nova = atividade(null, StatusAtividade.PENDENTE, 0);
            nova.setTurma(new Turma()); // id == null

            assertThrows(IllegalArgumentException.class, () -> service.criarAtividade(nova));

            verify(turmaRepository, never()).findById(any());
            verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita turma que não existe no banco")
        void rejeitaTurmaInexistente() {
            Atividade nova = atividade(null, StatusAtividade.PENDENTE, 0);
            when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.criarAtividade(nova)
            );

            assertEquals("Turma não encontrada.", ex.getMessage());
            verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("salva usando a turma carregada do banco e notifica só alunos com token FCM")
        void salvaENotificaSomenteAlunosComToken() {
            Turma turmaDoBanco = turma(TURMA_ID);

            Usuario comToken = aluno(1L, "ALUNO");
            comToken.setFcmToken("token-valido");
            Usuario tokenEmBranco = aluno(2L, "ALUNO");
            tokenEmBranco.setFcmToken("   ");
            Usuario semToken = aluno(3L, "ALUNO");
            semToken.setFcmToken(null);

            // Turma.alunos inicializada
            turmaDoBanco.getAlunos().add(comToken);
            turmaDoBanco.getAlunos().add(tokenEmBranco);
            turmaDoBanco.getAlunos().add(semToken);

            Atividade nova = atividade(null, StatusAtividade.EM_ANDAMENTO, 0);
            // a turma que chega do front só tem o id; a "de verdade" vem do banco
            nova.setTurma(turmaSomenteComId(TURMA_ID));

            when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turmaDoBanco));
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));

            Atividade salva = service.criarAtividade(nova);

            assertSame(turmaDoBanco, salva.getTurma(), "deve trocar a turma 'rascunho' pela entidade do banco");
            verify(notificacaoService, times(1)).notificarNovaAtividade(salva, "token-valido");
            verify(notificacaoService, times(1)).notificarNovaAtividade(any(Atividade.class), anyString());
        }

        @Test
        @DisplayName("turma sem alunos: salva e não notifica ninguém")
        void turmaSemAlunosNaoNotifica() {
            Turma turmaDoBanco = turma(TURMA_ID);
            Atividade nova = atividade(null, StatusAtividade.EM_ANDAMENTO, 0);

            when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turmaDoBanco));
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));

            service.criarAtividade(nova);

            verify(atividadeRepository).save(nova);
            verifyNoInteractions(notificacaoService);
        }
    }

    @Nested
    @DisplayName("consultas simples")
    class Consultas {

        @Test
        void listarTodasDelegaParaORepository() {
            List<Atividade> esperadas = List.of(atividade(1L, StatusAtividade.EM_ANDAMENTO, 0));
            when(atividadeRepository.findAll()).thenReturn(esperadas);

            assertSame(esperadas, service.listarTodas());
        }

        @Test
        void buscarPorIdRetornaOptionalDoRepository() {
            Atividade a = atividade(ATIVIDADE_ID, StatusAtividade.EM_ANDAMENTO, 0);
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(a));
            when(atividadeRepository.findById(999L)).thenReturn(Optional.empty());

            assertEquals(Optional.of(a), service.buscarPorId(ATIVIDADE_ID));
            assertTrue(service.buscarPorId(999L).isEmpty());
        }

        @Test
        void buscarPorTituloUsaBuscaIgnorandoMaiusculas() {
            List<Atividade> encontradas = List.of(atividade(1L, StatusAtividade.EM_ANDAMENTO, 0));
            when(atividadeRepository.findAllByTituloContainingIgnoreCase("FRAC")).thenReturn(encontradas);

            assertSame(encontradas, service.buscarPorTitulo("FRAC"));
        }
    }

    @Nested
    @DisplayName("atualizarAtividade")
    class AtualizarAtividade {

        @Test
        @DisplayName("retorna vazio e não salva quando a atividade não existe")
        void naoExiste() {
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.empty());

            Optional<Atividade> resultado = service.atualizarAtividade(
                    ATIVIDADE_ID, atividade(null, StatusAtividade.EM_ANDAMENTO, 0));

            assertTrue(resultado.isEmpty());
            verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("copia os campos editáveis para a entidade existente e salva")
        void copiaCamposESalva() {
            Atividade existente = atividade(ATIVIDADE_ID, StatusAtividade.PENDENTE, 0);

            Atividade novosDados = new Atividade();
            novosDados.setTitulo("Titulo novo");
            novosDados.setDescricao("Descricao nova da atividade");
            novosDados.setPontuacao(50);
            novosDados.setStatus(StatusAtividade.EM_ANDAMENTO);
            Turma outraTurma = turma(99L);
            novosDados.setTurma(outraTurma);

            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(existente));
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Atividade> resultado = service.atualizarAtividade(ATIVIDADE_ID, novosDados);

            assertTrue(resultado.isPresent());
            Atividade salva = resultado.get();
            assertSame(existente, salva, "atualiza a entidade existente, não cria outra");
            assertEquals(ATIVIDADE_ID, salva.getId());
            assertEquals("Titulo novo", salva.getTitulo());
            assertEquals("Descricao nova da atividade", salva.getDescricao());
            assertEquals(50, salva.getPontuacao());
            assertEquals(StatusAtividade.EM_ANDAMENTO, salva.getStatus());
            assertSame(outraTurma, salva.getTurma());
            verify(atividadeRepository).save(existente);
        }
    }

    @Nested
    @DisplayName("deletarAtividade")
    class DeletarAtividade {

        @Test
        void retornaFalseENaoApagaQuandoNaoExiste() {
            when(atividadeRepository.existsById(ATIVIDADE_ID)).thenReturn(false);

            assertFalse(service.deletarAtividade(ATIVIDADE_ID));

            verify(atividadeRepository, never()).deleteById(any());
        }

        @Test
        void apagaERetornaTrueQuandoExiste() {
            when(atividadeRepository.existsById(ATIVIDADE_ID)).thenReturn(true);

            assertTrue(service.deletarAtividade(ATIVIDADE_ID));

            verify(atividadeRepository).deleteById(ATIVIDADE_ID);
        }
    }

    // =========================================================================
    // buscarQuestionario: regras de acesso e montagem da resposta
    // =========================================================================

    @Nested
    @DisplayName("buscarQuestionario")
    class BuscarQuestionario {

        @Test
        @DisplayName("401 quando o usuário do token não existe mais")
        void usuarioInexistente() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.buscarQuestionario(ATIVIDADE_ID, USERNAME));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals("USUARIO_NAO_ENCONTRADO", ex.getCodigo());
            verifyNoInteractions(atividadeRepository);
        }

        @Test
        @DisplayName("404 quando a atividade não existe")
        void atividadeInexistente() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno(ALUNO_ID, "ALUNO")));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.buscarQuestionario(ATIVIDADE_ID, USERNAME));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("ATIVIDADE_NAO_ENCONTRADA", ex.getCodigo());
        }

        @Test
        @DisplayName("403 quando o aluno não está matriculado na turma da atividade")
        void alunoForaDaTurma() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(ATIVIDADE_ID, StatusAtividade.EM_ANDAMENTO, 2);
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
            doThrow(ApiException.proibido("ALUNO_FORA_DA_TURMA", "Você não pertence à turma desta atividade."))
                    .when(matriculaService).exigirMatricula(atividade, aluno);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.buscarQuestionario(ATIVIDADE_ID, USERNAME));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals("ALUNO_FORA_DA_TURMA", ex.getCodigo());
            verifyNoInteractions(resultadoAtividadeRepository, progressoAtividadeRepository);
        }

        @Test
        @DisplayName("aluno não enxerga atividade em rascunho (PENDENTE): responde como se não existisse")
        void alunoNaoVeRascunho() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade rascunho = atividade(ATIVIDADE_ID, StatusAtividade.PENDENTE, 2);
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(rascunho));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.buscarQuestionario(ATIVIDADE_ID, USERNAME));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("ATIVIDADE_NAO_ENCONTRADA", ex.getCodigo());
            verifyNoInteractions(resultadoAtividadeRepository, progressoAtividadeRepository);
        }

        @Test
        @DisplayName("aluno que já concluiu: devolve o resultado e nem consulta o progresso")
        void alunoComResultado() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(ATIVIDADE_ID, StatusAtividade.EM_ANDAMENTO, 2);
            ResultadoAtividade resultado = resultado(atividade, aluno, 2, 10);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
            when(resultadoAtividadeRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(resultado));

            QuestionarioResponse resposta = service.buscarQuestionario(ATIVIDADE_ID, USERNAME);

            assertNotNull(resposta.meuResultado());
            assertEquals(2, resposta.meuResultado().acertos());
            assertEquals(10, resposta.meuResultado().pontuacao());
            assertEquals(2, resposta.meuResultado().totalQuestoes());
            assertNull(resposta.meuProgresso());
            assertEquals(2, resposta.perguntas().size());
            verify(matriculaService).exigirMatricula(atividade, aluno);
            verifyNoInteractions(progressoAtividadeRepository);
        }

        @Test
        @DisplayName("aluno com atividade em andamento: devolve o progresso (1 de 2 = 50%)")
        void alunoComProgresso() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(ATIVIDADE_ID, StatusAtividade.EM_ANDAMENTO, 2);
            ProgressoAtividade progresso = progresso(atividade, aluno, 1);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
            when(resultadoAtividadeRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());
            when(progressoAtividadeRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(progresso));

            QuestionarioResponse resposta = service.buscarQuestionario(ATIVIDADE_ID, USERNAME);

            assertNull(resposta.meuResultado());
            assertNotNull(resposta.meuProgresso());
            assertEquals(50, resposta.meuProgresso().percentualConclusao());
            assertEquals(1, resposta.meuProgresso().respondidas());
            assertEquals(2, resposta.meuProgresso().totalQuestoes());
            assertEquals(1, resposta.meuProgresso().respostas().size());
        }

        @Test
        @DisplayName("aluno que ainda não começou: resultado e progresso nulos")
        void alunoSemNada() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(ATIVIDADE_ID, StatusAtividade.EM_ANDAMENTO, 2);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
            when(resultadoAtividadeRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());
            when(progressoAtividadeRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());

            QuestionarioResponse resposta = service.buscarQuestionario(ATIVIDADE_ID, USERNAME);

            assertNull(resposta.meuResultado());
            assertNull(resposta.meuProgresso());
            assertEquals(2, resposta.perguntas().size());
        }

        @Test
        @DisplayName("professor vê até rascunho, sem checar matrícula nem consultar resultado/progresso")
        void professorVeRascunhoSemRegrasDeAluno() {
            Usuario professor = aluno(1L, "PROFESSOR");
            Atividade rascunho = atividade(ATIVIDADE_ID, StatusAtividade.PENDENTE, 2);

            when(usuarioRepository.findByUsuario("prof@teste.com")).thenReturn(Optional.of(professor));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(rascunho));

            QuestionarioResponse resposta = service.buscarQuestionario(ATIVIDADE_ID, "prof@teste.com");

            assertEquals(StatusAtividade.PENDENTE, resposta.atividade().status());
            assertNull(resposta.meuResultado());
            assertNull(resposta.meuProgresso());
            verifyNoInteractions(matriculaService, resultadoAtividadeRepository, progressoAtividadeRepository);
        }
    }

    @Nested
    @DisplayName("listarParaAluno")
    class ListarParaAluno {

        @Test
        @DisplayName("401 quando o usuário do token não existe")
        void usuarioInexistente() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class, () -> service.listarParaAluno(USERNAME));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            verifyNoInteractions(atividadeRepository);
        }

        @Test
        @DisplayName("403 para quem não é aluno")
        void naoAluno() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno(1L, "PROFESSOR")));

            ApiException ex = assertThrows(ApiException.class, () -> service.listarParaAluno(USERNAME));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals("SOMENTE_ALUNO", ex.getCodigo());
            verifyNoInteractions(atividadeRepository);
        }

        @Test
        @DisplayName("sem atividades visíveis: lista vazia e nenhuma consulta extra (tipo 'aluno' em minúsculas também vale)")
        void semAtividadesVisiveis() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno(ALUNO_ID, "aluno")));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of());

            List<AtividadeAlunoResponse> resultado = service.listarParaAluno(USERNAME);

            assertTrue(resultado.isEmpty());
            verifyNoInteractions(resultadoAtividadeRepository, progressoAtividadeRepository);
        }

        @Test
        @DisplayName("exclui atividades em rascunho (PENDENTE) da busca de atividades visíveis")
        void rascunhoEhExcluidoDaBusca() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno(ALUNO_ID, "ALUNO")));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of());

            service.listarParaAluno(USERNAME);

            verify(atividadeRepository).findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE);
        }

        @Test
        @DisplayName("classifica cada atividade como CONCLUIDA, EM_ANDAMENTO ou PENDENTE e mantém a ordem")
        void classificaCadaAtividade() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade concluida = atividade(10L, StatusAtividade.EM_ANDAMENTO, 2);
            Atividade emAndamento = atividade(11L, StatusAtividade.EM_ANDAMENTO, 2);
            Atividade semInicio = atividade(12L, StatusAtividade.EM_ANDAMENTO, 2);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of(concluida, emAndamento, semInicio));
            when(resultadoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L, 11L, 12L)))
                    .thenReturn(List.of(resultado(concluida, aluno, 2, 10)));
            when(progressoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L, 11L, 12L)))
                    .thenReturn(List.of(progresso(emAndamento, aluno, 1)));

            List<AtividadeAlunoResponse> lista = service.listarParaAluno(USERNAME);

            assertEquals(3, lista.size());

            AtividadeAlunoResponse a = lista.get(0);
            assertEquals(10L, a.atividade().id());
            assertEquals(StatusAlunoAtividade.CONCLUIDA, a.status());
            assertEquals(2, a.acertos());
            assertEquals(10, a.pontuacao());
            assertEquals(100, a.percentualConclusao());

            AtividadeAlunoResponse b = lista.get(1);
            assertEquals(11L, b.atividade().id());
            assertEquals(StatusAlunoAtividade.EM_ANDAMENTO, b.status());
            assertNull(b.acertos());
            assertNull(b.pontuacao());
            assertEquals(50, b.percentualConclusao());

            AtividadeAlunoResponse c = lista.get(2);
            assertEquals(12L, c.atividade().id());
            assertEquals(StatusAlunoAtividade.PENDENTE, c.status());
            assertNull(c.acertos());
            assertNull(c.pontuacao());
            assertEquals(0, c.percentualConclusao());
        }

        @Test
        @DisplayName("se existe resultado E progresso para a mesma atividade, vale o resultado (CONCLUIDA)")
        void resultadoTemPrioridadeSobreProgresso() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(10L, StatusAtividade.EM_ANDAMENTO, 2);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of(atividade));
            when(resultadoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of(resultado(atividade, aluno, 1, 5)));
            when(progressoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of(progresso(atividade, aluno, 1)));

            List<AtividadeAlunoResponse> lista = service.listarParaAluno(USERNAME);

            assertEquals(1, lista.size());
            assertEquals(StatusAlunoAtividade.CONCLUIDA, lista.get(0).status());
            assertEquals(100, lista.get(0).percentualConclusao());
        }

        @Test
        @DisplayName("percentual é arredondado: 2 de 3 perguntas respondidas = 67%")
        void percentualArredondado() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade atividade = atividade(10L, StatusAtividade.EM_ANDAMENTO, 3);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of(atividade));
            when(resultadoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of());
            when(progressoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of(progresso(atividade, aluno, 2)));

            List<AtividadeAlunoResponse> lista = service.listarParaAluno(USERNAME);

            assertEquals(67, lista.get(0).percentualConclusao());
        }

        @Test
        @DisplayName("atividade sem perguntas e com progresso: 0% (sem divisão por zero)")
        void atividadeSemPerguntasNaoDividePorZero() {
            Usuario aluno = aluno(ALUNO_ID, "ALUNO");
            Atividade vazia = atividade(10L, StatusAtividade.EM_ANDAMENTO, 0);

            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
            when(atividadeRepository.findVisiveisParaAluno(ALUNO_ID, StatusAtividade.PENDENTE))
                    .thenReturn(List.of(vazia));
            when(resultadoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of());
            when(progressoAtividadeRepository.findAllByUsuario_IdAndAtividade_IdIn(ALUNO_ID, List.of(10L)))
                    .thenReturn(List.of(progresso(vazia, aluno, 0)));

            List<AtividadeAlunoResponse> lista = service.listarParaAluno(USERNAME);

            assertEquals(StatusAlunoAtividade.EM_ANDAMENTO, lista.get(0).status());
            assertEquals(0, lista.get(0).percentualConclusao());
        }
    }

    private static Usuario aluno(Long id, String tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTipoUsuario(tipo);
        u.setNomeUsuario("Usuario " + id);
        u.setUsuario("usuario" + id + "@teste.com");
        return u;
    }

    private static Turma turma(long id) {
        Turma t = new Turma();
        t.setId(id);
        t.setNome("Turma " + id);
        return t;
    }

    private static Turma turmaSomenteComId(long id) {
        Turma t = new Turma();
        t.setId(id);
        return t;
    }

    private static Atividade atividade(Long id, StatusAtividade status, int qtdPerguntas) {
        Atividade a = new Atividade();
        a.setId(id);
        a.setTitulo("Atividade " + id);
        a.setDescricao("Descricao da atividade " + id);
        a.setPontuacao(10);
        a.setStatus(status);
        a.setTurma(turma(TURMA_ID));

        for (int i = 1; i <= qtdPerguntas; i++) {
            Pergunta p = new Pergunta();
            p.setId(id * 100 + i);
            p.setEnunciado("Pergunta " + i);
            p.setAtividade(a);

            Alternativa alt = new Alternativa();
            alt.setId(p.getId() * 10 + 1);
            alt.setTexto("Alternativa A");
            alt.setCorreta(true);
            alt.setPergunta(p);
            p.getAlternativas().add(alt);

            a.getPerguntas().add(p);
        }
        return a;
    }

    private static ResultadoAtividade resultado(Atividade atividade, Usuario aluno, int acertos, int pontuacao) {
        ResultadoAtividade r = new ResultadoAtividade();
        r.setAtividade(atividade);
        r.setUsuario(aluno);
        r.setAcertos(acertos);
        r.setPontuacao(pontuacao);
        return r;
    }

    private static ProgressoAtividade progresso(Atividade atividade, Usuario aluno, int respondidas) {
        ProgressoAtividade p = new ProgressoAtividade();
        p.setAtividade(atividade);
        p.setUsuario(aluno);

        for (int i = 0; i < respondidas; i++) {
            Pergunta pergunta = atividade.getPerguntas().get(i);
            RespostaAtividade r = new RespostaAtividade();
            r.setProgresso(p);
            r.setPergunta(pergunta);
            r.setAlternativa(pergunta.getAlternativas().get(0));
            p.getRespostas().add(r);
        }
        return p;
    }
}
