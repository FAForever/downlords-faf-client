package com.faforever.client.clan;

import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
public class ClanService {

  private final FafService fafService;


  public ClanService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return fafService.getClanByTag(tag);
  }
}


