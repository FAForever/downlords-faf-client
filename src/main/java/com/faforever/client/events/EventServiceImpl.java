package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


@Lazy
@Service
public class EventServiceImpl implements EventService {

  private final FafApiAccessor fafApiAccessor;
  private final PlayerService playerService;
  private final ThreadPoolExecutor threadPoolExecutor;

  @Inject
  public EventServiceImpl(FafApiAccessor fafApiAccessor, PlayerService playerService, ThreadPoolExecutor threadPoolExecutor) {
    this.fafApiAccessor = fafApiAccessor;
    this.playerService = playerService;
    this.threadPoolExecutor = threadPoolExecutor;
  }

  @Override
  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(String username) {
    Player playerForUsername = playerService.getPlayerForUsername(username);
    if (playerForUsername == null) {
      return CompletableFuture.completedFuture(Collections.emptyMap());
    }
    int playerId = playerForUsername.getId();

    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getPlayerEvents(playerId).stream()
        .collect(toMap(PlayerEvent::getEventId, identity())), threadPoolExecutor);
  }
}
