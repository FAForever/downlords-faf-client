package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerEvent;
import com.faforever.client.player.PlayerService;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class EventServiceImpl implements EventService {

  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  PlayerService playerService;
  @Resource
  ExecutorService executorService;

  @Override
  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(String username) {
    int playerId = playerService.getPlayerForUsername(username).getId();

    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getPlayerEvents(playerId).stream()
        .collect(Collectors.toMap(PlayerEvent::getEventId, identity())), executorService);
  }
}
