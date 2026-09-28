package com.umc.menteup.service;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RankingService {
    private final ResultadoAtividadeRepository resultadoRepository;

    public RankingService(
            ResultadoAtividadeRepository resultadoRepository
    ) {
        this.resultadoRepository = resultadoRepository;
    }

    public List<RankingResponse> buscarRanking() {
        return resultadoRepository.buscarRanking();
    }
}
