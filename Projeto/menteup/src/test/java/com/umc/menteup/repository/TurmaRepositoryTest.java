package com.umc.menteup.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.util.TestBuilder;

/**
 * Teste de persistência do TurmaRepository com banco H2 real (em memória).
 * Cada teste roda em uma transação que é desfeita ao final, então nenhum teste
 * depende dos dados de outro.
 */
@DataJpaTest
class TurmaRepositoryTest {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("salva a turma com alunos e recupera os mesmos dados do banco")
    void deveSalvarERecuperarTurmaComAlunos() {
        // Arrange
        Usuario aluno = persistirAluno("aluno.salvar@teste.com");
        Turma turma = novaTurma("Matemática 1A");
        turma.getAlunos().add(aluno);

        // Act
        Turma salva = turmaRepository.save(turma);
        entityManager.flush();
        entityManager.clear(); // força a leitura do banco, e não do cache da sessão
        Turma recuperada = turmaRepository.findById(salva.getId()).orElseThrow();

        // Assert
        assertNotNull(salva.getId());
        assertEquals("Matemática 1A", recuperada.getNome());
        assertEquals(1, recuperada.getAlunos().size());
        assertEquals("aluno.salvar@teste.com", recuperada.getAlunos().get(0).getUsuario());
    }

    @Test
    @DisplayName("contarMatricula devolve 1 quando o aluno está matriculado na turma")
    void deveContarUmaMatriculaQuandoAlunoEstaNaTurma() {
        // Arrange
        Usuario aluno = persistirAluno("aluno.matriculado@teste.com");
        Turma turma = novaTurma("Português 2B");
        turma.getAlunos().add(aluno);
        entityManager.persistAndFlush(turma);

        // Act
        long matriculas = turmaRepository.contarMatricula(turma.getId(), aluno.getId());

        // Assert
        assertEquals(1, matriculas);
    }

    @Test
    @DisplayName("contarMatricula devolve 0 quando o aluno não pertence à turma")
    void deveContarZeroMatriculasQuandoAlunoNaoEstaNaTurma() {
        // Arrange
        Usuario alunoDeFora = persistirAluno("aluno.fora@teste.com");
        Turma turmaVazia = entityManager.persistAndFlush(novaTurma("História 3C"));

        // Act
        long matriculas = turmaRepository.contarMatricula(turmaVazia.getId(), alunoDeFora.getId());

        // Assert
        assertEquals(0, matriculas);
    }

    @Test
    @DisplayName("busca por nome ignora maiúsculas e minúsculas")
    void deveBuscarTurmaPorNomeIgnorandoMaiusculas() {
        // Arrange
        entityManager.persist(novaTurma("Ciências 1A"));
        entityManager.persistAndFlush(novaTurma("Geografia 1A"));

        // Act
        List<Turma> encontradas = turmaRepository.findAllByNomeContainingIgnoreCase("CIÊNCIAS");

        // Assert
        assertEquals(1, encontradas.size());
        assertTrue(encontradas.get(0).getNome().startsWith("Ciências"));
    }

    private Usuario persistirAluno(String email) {
        return entityManager.persistAndFlush(
                TestBuilder.criarUsuario(null, "Aluno Teste", email, "12345678"));
    }

    private static Turma novaTurma(String nome) {
        Turma turma = new Turma();
        turma.setNome(nome);
        return turma;
    }
}
