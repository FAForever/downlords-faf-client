package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerEvent;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;

import javax.annotation.Resource;
import java.util.Collections;
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
    PlayerInfoBean playerForUsername = playerService.getPlayerForUsername(username);
    if (playerForUsername == null) {
      return CompletableFuture.completedFuture(Collections.emptyMap());
    }
    int playerId = playerForUsername.getId();

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
