package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import org.springframework.util.Assert;

import static com.faforever.client.player.SocialStatus.SELF;

public class ReportPlayerMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    ReportDialogController reportDialogController = getBean(UiService.class).loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getParentPopup().getOwnerWindow());
    reportDialogController.show();
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null && player.getSocialStatus() != SELF;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.report");
  }
}
