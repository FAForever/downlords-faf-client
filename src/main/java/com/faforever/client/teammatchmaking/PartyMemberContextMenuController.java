package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.fx.OnlinePlayerContextMenuController;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class PartyMemberContextMenuController extends OnlinePlayerContextMenuController {

  public PartyMemberContextMenuController(AvatarService avatarService,
                                          EventBus eventBus,
                                          I18n i18n,
                                          JoinGameHelper joinGameHelper,
                                          ModeratorService moderatorService,
                                          NotificationService notificationService,
                                          PlayerService playerService,
                                          ReplayService replayService,
                                          UiService uiService) {
    super(avatarService, eventBus, i18n, joinGameHelper, moderatorService, notificationService, playerService, replayService, uiService);
  }
}
