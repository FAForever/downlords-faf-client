package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.api.Clan;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SendPrivateMessageClanLeaderMenuItem extends AbstractMenuItem<PlayerInfo> {

  private final I18n i18n;
  private final ClanService clanService;
  private final ChatService chatService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");

    clanService.getClanByTag(object.getClan()).map(Clan::leader).map(PlayerInfo::getUsername)
               .subscribe(chatService::joinPrivateChat);
  }

  @Override
  protected String getStyleIcon() {
    return "bubble-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return object != null;
  }

  @Override
  protected String getItemText() {
    return i18n.get("clan.messageLeader");
  }
}
