package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SendPrivateMessageMenuItem extends AbstractMenuItem<String> {

  private final I18n i18n;
  private final EventBus eventBus;
  private final PlayerService playerService;

  @Override
  protected void onClicked() {
    Assert.isTrue(!StringUtils.isBlank(object), "No username has been set");
    eventBus.post(new InitiatePrivateChatEvent(object));
  }

  @Override
  protected boolean isItemVisible() {
    return !StringUtils.isBlank(object) && !playerService.getCurrentPlayer().getUsername().equals(object);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.privateMessage");
  }
}
