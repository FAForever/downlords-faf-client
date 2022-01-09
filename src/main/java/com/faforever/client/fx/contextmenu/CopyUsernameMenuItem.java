package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.ClipboardUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CopyUsernameMenuItem extends AbstractMenuItem<String> {

  private final I18n i18n;

  @Override
  protected void onClicked() {
    ClipboardUtil.copyToClipboard(getObject());
  }

  @Override
  protected boolean isItemVisible() {
    return !StringUtils.isBlank(getUnsafeObject());
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.copyUsername");
  }
}
