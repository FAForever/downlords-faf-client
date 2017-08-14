package com.faforever.client.clan;

import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
public class ClanServiceImpl implements ClanService {

  private final FafService fafService;

  @Inject
  public ClanServiceImpl(FafService fafService) {
    this.fafService = fafService;
  }

  @Override
  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return fafService.getClanByTag(tag);
  }
}


