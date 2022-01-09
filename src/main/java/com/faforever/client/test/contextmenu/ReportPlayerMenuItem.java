package com.faforever.client.test.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;

import static com.faforever.client.player.SocialStatus.SELF;

public class ReportPlayerMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    ReportDialogController reportDialogController = getBean(UiService.class).loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getParentPopup().getOwnerWindow());
    reportDialogController.show();
  }

  @Override
  protected boolean getVisible() {
    return getObject().getSocialStatus() != SELF;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.report");
  }
}
