package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SendPrivateMessageClanLeaderMenuItem extends AbstractMenuItem<ClanBean> {

  private final I18n i18n;
  private final PlayerService playerService;
  private final EventBus eventBus;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no clan has been set");
    eventBus.post(new InitiatePrivateChatEvent(object.getLeader().getUsername()));
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && !playerService.getCurrentPlayer().getId().equals(object.getLeader().getId())
        && playerService.isOnline(object.getLeader().getId());
  }

  @Override
  protected String getItemText() {
    return i18n.get("clan.messageLeader");
  }
}
