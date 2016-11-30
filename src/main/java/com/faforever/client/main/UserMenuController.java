package com.faforever.client.main;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.neovisionaries.i18n.CountryCode;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static com.faforever.client.util.IdenticonUtil.createIdenticon;

public class UserMenuController {

  @FXML
  ImageView userImageView;
  @FXML
  Label usernameLabel;
  @FXML
  Label countryLabel;
  @FXML
  ImageView countryImageView;
  @FXML
  Pane userMenuRoot;

  @Resource
  PlayerService playerService;
  @Resource
  CountryFlagService countryFlagService;
  @Resource
  UserService userService;
  @Resource
  PlatformService platformService;
  @Resource
  ApplicationContext applicationContext;

  public Pane getRoot() {
    return userMenuRoot;
  }

  @PostConstruct
  void postConstruct() {
    playerService.currentPlayerProperty().addListener((observable, oldValue, newValue) -> {
      usernameLabel.textProperty().bind(newValue.usernameProperty());
      countryImageView.imageProperty().bind(Bindings.createObjectBinding(() -> countryFlagService.loadCountryFlag(newValue.getCountry()), newValue.countryProperty()));
      userImageView.imageProperty().bind(Bindings.createObjectBinding(() -> createIdenticon(newValue.getId()), newValue.idProperty()));
      countryLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
        CountryCode countryCode = CountryCode.getByCode(newValue.getCountry());
        if (countryCode != null) {
          // Country code is unknown to CountryCode, like A1 or A2 (from GeoIP)
          return countryCode.getName();
        }
        return newValue.getCountry();
      }, newValue.countryProperty()));
    });
  }

  @FXML
  void onLogOutButtonClicked() {
    userService.logOut();
    userMenuRoot.getScene().getWindow().hide();
  }

  public void onShowProfileButtonClicked() {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayer(playerService.getCurrentPlayer());

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(((Popup) userMenuRoot.getScene().getWindow()).getOwnerWindow());

    WindowController windowController = applicationContext.getBean(WindowController.class);
    windowController.configure(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }
}
