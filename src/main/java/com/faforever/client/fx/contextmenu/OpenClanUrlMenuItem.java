package com.faforever.client.fx.contextmenu;

import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.api.Clan;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class OpenClanUrlMenuItem extends AbstractMenuItem<PlayerInfo> {

  private final I18n i18n;
  private final PlatformService platformService;
  private final ClanService clanService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");

    clanService.getClanByTag(object.getClan()).map(Clan::websiteUrl)
               .map(URL::toExternalForm)
               .subscribe(platformService::showDocument);
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && StringUtils.isNotBlank(object.getClan());
  }

  @Override
  protected String getItemText() {
    return i18n.get("clan.visitPage");
  }
}
