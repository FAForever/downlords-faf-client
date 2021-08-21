package com.faforever.client.tournament;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.TournamentBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.TournamentMapper;
import com.faforever.commons.api.dto.Tournament;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentService {
  private final FafApiAccessor fafApiAccessor;
  private final TournamentMapper tournamentMapper;

  public CompletableFuture<List<TournamentBean>> getAllTournaments() {
    return fafApiAccessor.getMany(Tournament.class, "/challonge/v1/tournaments.json", 100, Map.of())
        .map(dto -> tournamentMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }
}
