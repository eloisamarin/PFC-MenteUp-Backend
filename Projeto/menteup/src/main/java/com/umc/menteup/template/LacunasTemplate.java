package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

///O aluno deve preencher todas as lacunas com as opções corretas, ignorando as palavras incorretas embaralhadas.
@Component
public class LacunasTemplate extends SequenciaTemplate {

    private static final Pattern LACUNA = Pattern.compile("\\{\\{(\\d{1,3})}}");

    @Override
    public TipoAtividade tipo() {
        return TipoAtividade.LACUNAS;
    }

    ///N lacunas e 1 resposta correta para cada
    @Override
    public void validarCriacao(PerguntaRequest pergunta) {
        List<Integer> lacunas = lacunasDoTexto(pergunta.enunciado());

        if (lacunas.isEmpty()) {
            throw invalida("O texto deve ter pelo menos uma lacuna, por exemplo: \"A capital do Brasil é {{1}}.\"");
        }

        Set<Integer> esperadas = IntStream.rangeClosed(1, lacunas.size()).boxed().collect(Collectors.toSet());
        if (!new HashSet<>(lacunas).equals(esperadas)) {
            throw invalida("As lacunas devem ser numeradas de {{1}} a {{" + lacunas.size() + "}}, sem repetir.");
        }

        List<AlternativaRequest> corretas = pergunta.alternativas().stream()
                .filter(a -> Boolean.TRUE.equals(a.correta()))
                .toList();
        Set<Integer> ordensCorretas = corretas.stream()
                .map(AlternativaRequest::ordem)
                .collect(Collectors.toSet());

        if (corretas.size() != lacunas.size() || !ordensCorretas.equals(esperadas)) {
            throw invalida("Cada lacuna deve ter exatamente uma palavra correta, com ordem igual ao número da lacuna.");
        }

        boolean distratorComOrdem = pergunta.alternativas().stream()
                .anyMatch(a -> !Boolean.TRUE.equals(a.correta()) && a.ordem() != null);
        if (distratorComOrdem) {
            throw invalida("Palavras extras (distratores) não devem ter ordem.");
        }
    }

    ///Apenas 1 palavra por lacuna
    @Override
    protected int tamanhoEsperado(Pergunta pergunta) {
        return (int) pergunta.getAlternativas().stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorreta()))
                .count();
    }

    /// Correção pelo texto, assim uma palavra repetida está correta em todas as lacunas que ela é uma resposta correta
    @Override
    protected boolean sequenciaCorreta(Pergunta pergunta, List<Alternativa> sequencia) {
        Map<Integer, String> respostaPorLacuna = pergunta.getAlternativas().stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorreta()))
                .collect(Collectors.toMap(Alternativa::getOrdem, a -> normalizar(a.getTexto())));

        for (int i = 0; i < sequencia.size(); i++) {
            if (!normalizar(sequencia.get(i).getTexto()).equals(respostaPorLacuna.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    ///verifica o número das lacunas na ordem em que aparecem no texto
    private static List<Integer> lacunasDoTexto(String texto) {
        List<Integer> lacunas = new ArrayList<>();
        Matcher matcher = LACUNA.matcher(texto == null ? "" : texto);
        while (matcher.find()) {
            lacunas.add(Integer.parseInt(matcher.group(1)));
        }
        return lacunas;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    private static ApiException invalida(String mensagem) {
        return ApiException.requisicaoInvalida("LACUNAS_INVALIDA", mensagem);
    }
}

