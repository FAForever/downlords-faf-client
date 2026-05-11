package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
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
public class AddMutedUserMenuItem extends AbstractMenuItem<ChatChannelUser> {

  private final ChatPrefs chatPrefs;
  private final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no chat user has been set");
    chatPrefs.addMutedUser(object.getUsername());
  }

  @Override
  protected boolean isDisplayed() {
    return object != null
        && object.getCategory() != ChatUserCategory.SELF
        && !chatPrefs.isUserMuted(object.getUsername());
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.mute");
  }
}
