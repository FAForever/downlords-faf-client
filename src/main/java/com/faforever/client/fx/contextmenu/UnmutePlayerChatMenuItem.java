package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.server.PlayerInfo;
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
public class UnmutePlayerChatMenuItem extends AbstractMenuItem<PlayerInfo> {

  private final ChatPrefs chatPrefs;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    chatPrefs.unmuteUser(object.getUsername());
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && chatPrefs.isUserMuted(object.getUsername());
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.unmutePlayerChat");
  }
}
