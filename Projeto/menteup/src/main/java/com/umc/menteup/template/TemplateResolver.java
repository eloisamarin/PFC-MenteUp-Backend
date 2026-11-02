package com.umc.menteup.template;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TemplateResolver {

    private final Map<TipoAtividade, TemplateAtividade> templates = new EnumMap<>(TipoAtividade.class);

    public TemplateResolver(List<TemplateAtividade> implementacoes) {
        for (TemplateAtividade template : implementacoes) {
            if (templates.put(template.tipo(), template) != null) {
                throw new IllegalStateException("Mais de um template registrado para o tipo " + template.tipo());
            }
        }
    }

    public TemplateAtividade resolver(Atividade atividade) {
        return resolver(atividade.getTipo());
    }

    public TemplateAtividade resolver(TipoAtividade tipo) {
        TemplateAtividade template = templates.get(TipoAtividade.ouPadrao(tipo));

        if (template == null) {
            throw ApiException.requisicaoInvalida(
                    "TIPO_ATIVIDADE_NAO_SUPORTADO",
                    "O tipo de atividade " + tipo + " ainda não está disponível."
            );
        }
        return template;
    }
}
