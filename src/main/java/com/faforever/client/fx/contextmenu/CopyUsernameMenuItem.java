package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import com.faforever.client.util.ClipboardUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Primary
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CopyUsernameMenuItem extends AbstractMenuItem<String> {

  protected final I18n i18n;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no username has been set");
    ClipboardUtil.copyToClipboard(object);
  }

  @Override
  protected boolean isItemVisible() {
    return !StringUtils.isBlank(object);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.copyUsername");
  }
}
