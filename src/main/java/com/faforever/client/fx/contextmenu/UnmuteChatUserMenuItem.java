package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UnmuteChatUserMenuItem extends AbstractMenuItem<ChatChannelUser> {

  private final ChatPrefs chatPrefs;
  private final ChatService chatService;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no chat user has been set");
    chatPrefs.unmuteUser(object.getUsername());
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.unmute");
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && !isCurrentUser() && chatPrefs.isUserMuted(object.getUsername());
  }

  private boolean isCurrentUser() {
    String currentUsername = chatService.getCurrentUsername();
    return currentUsername != null && currentUsername.equalsIgnoreCase(object.getUsername());
  }
}
