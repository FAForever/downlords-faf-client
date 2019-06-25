package com.faforever.client.teammatchmaking;

import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final PlayerService playerService;

  public TeamMatchmakingService(FafServerAccessor fafServerAccessor, PlayerService playerService) {
    this.fafServerAccessor = fafServerAccessor;
    this.playerService = playerService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  public void invitePlayer(String player) {
    //TODO
  }
}
