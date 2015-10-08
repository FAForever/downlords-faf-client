package com.faforever.client.main;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.HostService;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.gravatar.GravatarService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.neovisionaries.i18n.CountryCode;
import javafx.beans.binding.ObjectBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

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
  Node userMenuRoot;

  @Resource
  PlayerService playerService;
  @Resource
  CountryFlagService countryFlagService;
  @Resource
  UserService userService;
  @Resource
  GravatarService gravatarService;
  @Resource
  HostService hostService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  SceneFactory sceneFactory;

  public Node getRoot() {
    return userMenuRoot;
  }

  @PostConstruct
  void postConstruct() {
    playerService.currentPlayerProperty().addListener((observable, oldValue, newValue) -> {
      countryLabel.textProperty().bind(countryLabelBinding(newValue));
      usernameLabel.textProperty().bind(newValue.usernameProperty());
      countryImageView.imageProperty().bind(countryImageBinding(newValue));
    });

    userImageView.imageProperty().bind(new ObjectBinding<Image>() {
      @Override
      protected Image computeValue() {
        return gravatarService.getGravatar(userService.emailProperty().get());
      }

      {
        bind(userService.emailProperty());
      }
    });
  }

  @NotNull
  private ObjectBinding<String> countryLabelBinding(final PlayerInfoBean newValue) {
    return new ObjectBinding<String>() {
      {
        bind(newValue.countryProperty());
      }

      @Override
      protected String computeValue() {
        CountryCode countryCode = CountryCode.getByCode(newValue.getCountry());
        if (countryCode != null) {
          // Country code is unknown to CountryCode, like A1 or A2 (from GeoIP)
          return countryCode.getName();
        }
        return newValue.getCountry();
      }
    };
  }

  @NotNull
  private ObjectBinding<Image> countryImageBinding(final PlayerInfoBean newValue) {
    return new ObjectBinding<Image>() {
      {
        bind(newValue.countryProperty());
      }

      @Override
      protected Image computeValue() {
        return countryFlagService.loadCountryFlag(newValue.getCountry());
      }
    };
  }

  @FXML
  void onLogOutButtonClicked() {

  }

  @FXML
  void onUserImageClicked() {
    hostService.showDocument(gravatarService.getProfileUrl(userService.getEmail()));
  }

  public void onShowProfileButtonClicked() {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerService.getCurrentPlayer());

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(((Popup) userMenuRoot.getScene().getWindow()).getOwnerWindow());

    sceneFactory.createScene(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }
}
