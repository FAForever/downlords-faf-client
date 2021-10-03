package com.faforever.client.teammatchmaking;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.PlayerContextMenuController;
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

import static com.faforever.client.player.SocialStatus.SELF;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class PartyMemberContextMenuController extends PlayerContextMenuController {

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

  @Override
  public void initialize() {
    super.initialize();
    sendPrivateMessageItem.setOnAction(event -> onSendPrivateMessageSelected());
  }

  @Override
  public void setPlayer(PlayerBean player) {
    super.setPlayer(player);
    sendPrivateMessageItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(super.player.getUsername()));
  }

}
