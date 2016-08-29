package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class EventServiceImpl implements EventService {

  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  PlayerService playerService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;

  @Override
  public CompletionStage<Map<String, PlayerEvent>> getPlayerEvents(String username) {
    Player playerForUsername = playerService.getPlayerForUsername(username);
    if (playerForUsername == null) {
      return CompletableFuture.completedFuture(Collections.emptyMap());
    }
    int playerId = playerForUsername.getId();

    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getPlayerEvents(playerId).stream()
        .collect(toMap(PlayerEvent::getEventId, identity())), threadPoolExecutor);
  }
}
