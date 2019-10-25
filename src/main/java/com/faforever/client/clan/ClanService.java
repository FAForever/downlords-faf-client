package com.faforever.client.clan;

import com.faforever.client.remote.FafService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
@RequiredArgsConstructor
public class ClanService {

  private final FafService fafService;

  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return fafService.getClanByTag(tag);
  }
}


