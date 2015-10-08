package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.util.RatingUtil;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerInfoTooltipController {

  @FXML
  Pane playerInfoTooltipRoot;

  @FXML
  Label tooltipLabel;

  @Autowired
  CountryFlagService countryFlagService;

  @Autowired
  I18n i18n;

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    ((ImageView) tooltipLabel.getGraphic()).setImage(countryFlagService.loadCountryFlag(playerInfoBean.getCountry()));
    tooltipLabel.setText(
        i18n.get("userInfoTooltipFormat", playerInfoBean.getUsername(), RatingUtil.getGlobalRating(playerInfoBean))
    );
  }

  public Node getRoot() {
    return playerInfoTooltipRoot;
  }
}
