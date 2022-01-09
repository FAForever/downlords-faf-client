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
    String username = getObject();
    Assert.isTrue(!StringUtils.isBlank(username), "No username has been set");
    eventBus.post(new InitiatePrivateChatEvent(username));
  }

  @Override
  protected boolean isItemVisible() {
    String username = getObject();
    return !StringUtils.isBlank(username) && !playerService.getCurrentPlayer().getUsername().equals(username);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.privateMessage");
  }
}
