package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.ClipboardUtil;

public class CopyUsernameMenuItem extends AbstractMenuItem<String> {

  @Override
  protected void onClicked(String object) {
    ClipboardUtil.copyToClipboard(object);
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.copyUsername");
  }
}
