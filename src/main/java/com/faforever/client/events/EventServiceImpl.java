package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.PlayerEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


@Lazy
@Service
public class EventServiceImpl implements EventService {

  private final FafApiAccessor fafApiAccessor;
  private final ThreadPoolExecutor threadPoolExecutor;

  @Inject
  public EventServiceImpl(FafApiAccessor fafApiAccessor, ThreadPoolExecutor threadPoolExecutor) {
    this.fafApiAccessor = fafApiAccessor;
    this.threadPoolExecutor = threadPoolExecutor;
  }

  @Override
  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getPlayerEvents(playerId).stream()
        .collect(toMap(playerEvent -> playerEvent.getEvent().getId(), identity())), threadPoolExecutor);
  }
}
