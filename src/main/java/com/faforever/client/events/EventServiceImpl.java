package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;
import static java.util.function.Function.identity;

public class EventServiceImpl implements EventService {

  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  PlayerService playerService;
  @Resource
  TaskService taskService;

  @Override
  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(String username) {
    int playerId = playerService.getPlayerForUsername(username).getId();

    return taskService.submitTask(new AbstractPrioritizedTask<Map<String, PlayerEvent>>(HIGH) {
      @Override
      protected Map<String, PlayerEvent> call() throws Exception {
        return fafApiAccessor.getPlayerEvents(playerId).stream()
            .collect(Collectors
                .toMap(PlayerEvent::getEventId, identity()));
      }
    });
  }
}
