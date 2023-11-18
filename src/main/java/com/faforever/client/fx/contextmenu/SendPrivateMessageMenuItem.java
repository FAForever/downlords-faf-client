package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
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
  private final PlayerService playerService;
  private final ChatService chatService;

  @Override
  protected void onClicked() {
    Assert.isTrue(!StringUtils.isBlank(object), "No username has been set");
    chatService.onInitiatePrivateChat(object);
  }

  @Override
  protected String getStyleIcon() {
    return "bubble-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return !StringUtils.isBlank(object)
        && !playerService.getCurrentPlayer().getUsername().equals(object)
        && chatService.userExistsInAnyChannel(object);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.privateMessage");
  }
}
