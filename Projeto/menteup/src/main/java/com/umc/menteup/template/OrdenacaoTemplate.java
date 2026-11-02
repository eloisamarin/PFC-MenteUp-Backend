package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

///O aluno marca as respostas em determinada ordem e só acerta se a sequência inteira estiver correta
@Component
public class OrdenacaoTemplate extends SequenciaTemplate {

    @Override
    public TipoAtividade tipo() {
        return TipoAtividade.ORDENACAO;
    }

    ///Deve ter ao menos 2 itens e as ordens corretas
    @Override
    public void validarCriacao(PerguntaRequest pergunta) {
        List<AlternativaRequest> itens = pergunta.alternativas();

        if (itens.size() < 2) {
            throw invalida("A pergunta \"" + pergunta.enunciado() + "\" precisa de pelo menos 2 itens para ordenar.");
        }

        if (itens.stream().map(AlternativaRequest::ordem).anyMatch(Objects::isNull)) {
            throw invalida("Todos os itens de \"" + pergunta.enunciado() + "\" precisam informar a ordem.");
        }

        Set<Integer> ordens = itens.stream().map(AlternativaRequest::ordem).collect(Collectors.toSet());
        Set<Integer> esperadas = IntStream.rangeClosed(1, itens.size()).boxed().collect(Collectors.toSet());

        if (ordens.size() != itens.size() || !ordens.equals(esperadas)) {
            throw invalida("A ordem dos itens de \"" + pergunta.enunciado()
                    + "\" deve ir de 1 a " + itens.size() + ", sem repetir.");
        }
    }

    ///Todos os itens devem ser posicionados
    @Override
    protected int tamanhoEsperado(Pergunta pergunta) {
        return pergunta.getAlternativas().size();
    }

    ///Correção da ordem
    @Override
    protected boolean sequenciaCorreta(Pergunta pergunta, List<Alternativa> sequencia) {
        for (int i = 0; i < sequencia.size(); i++) {
            if (!Objects.equals(sequencia.get(i).getOrdem(), i + 1)) {
                return false;
            }
        }
        return true;
    }

    private static ApiException invalida(String mensagem) {
        return ApiException.requisicaoInvalida("ORDENACAO_INVALIDA", mensagem);
    }
}
