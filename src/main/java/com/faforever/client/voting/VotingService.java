package com.faforever.client.voting;

import com.faforever.client.remote.FafService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class VotingService {
  private final FafService fafService;

  public VotingService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<List<VotingSubject>> getOutStandingVotingSubjects() {
    return fafService.getOutStandingVotingSubjects();
  }
}
