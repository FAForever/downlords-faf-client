package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.ClipboardUtil;
import org.apache.commons.lang3.StringUtils;

public class CopyUsernameMenuItem extends AbstractMenuItem<String> {

  @Override
  protected void onClicked(String username) {
    ClipboardUtil.copyToClipboard(username);
  }

  @Override
  protected boolean isItemVisible(String username) {
    return !StringUtils.isBlank(username);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.copyUsername");
  }
}
