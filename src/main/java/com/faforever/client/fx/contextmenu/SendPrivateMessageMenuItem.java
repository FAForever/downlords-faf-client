package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.SELF;

public class SendPrivateMessageMenuItem extends AbstractMenuItem<String> {

  @Override
  protected void onClicked(String username) {
    Assert.isTrue(!StringUtils.isBlank(username), "No username has been set");
    getBean(EventBus.class).post(new InitiatePrivateChatEvent(username));
  }

  @Override
  protected boolean isItemVisible(String username) {
    return !StringUtils.isBlank(username) && !getBean(PlayerService.class).getCurrentPlayer().getUsername().equals(username);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.privateMessage");
  }
}
