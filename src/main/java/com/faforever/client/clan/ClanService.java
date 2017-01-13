package com.faforever.client.clan;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClanService {

  CompletableFuture<Optional<Clan>> getClanByTag(String tag);
}
