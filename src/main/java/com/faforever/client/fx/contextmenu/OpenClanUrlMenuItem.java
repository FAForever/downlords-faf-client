package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.ClanBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class OpenClanUrlMenuItem extends AbstractMenuItem<ClanBean> {

  private final I18n i18n;
  private final PlatformService platformService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no clan has been set");
    platformService.showDocument(object.getWebsiteUrl());
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && StringUtils.isNotBlank(object.getWebsiteUrl());
  }

  @Override
  protected String getItemText() {
    return i18n.get("clan.visitPage");
  }
}
