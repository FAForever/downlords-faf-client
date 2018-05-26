package com.faforever.client.tournament;

import com.faforever.client.remote.FafService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
@Slf4j
public class TournamentService {
  private final FafService fafService;

  public TournamentService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<List<TournamentBean>> getAllTournaments() {
    return fafService.getAllTournaments();
  }
}
