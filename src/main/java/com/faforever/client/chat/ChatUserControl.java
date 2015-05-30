package com.faforever.client.chat;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

public class ChatUserControl extends HBox {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String CLAN_TAG_FORMAT = "[%s]";

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  AvatarService avatarService;

  @Autowired
  CountryFlagService countryFlagService;

  @Autowired
  I18n i18n;

  @Autowired
  UserInfoWindowFactory userInfoWindowFactory;

  @Autowired
  SceneFactory sceneFactory;

  @FXML
  ImageView countryImageView;

  @FXML
  ImageView avatarImageView;

  @FXML
  Label usernameLabel;

  @FXML
  Label clanLabel;

  private final PlayerInfoBean playerInfoBean;
  private final OnChatUserControlDoubleClickListener onChatUserControlDoubleClickListener;
  private Stage userInfoWindow;

  public ChatUserControl(PlayerInfoBean playerInfoBean, OnChatUserControlDoubleClickListener onChatUserControlDoubleClickListener) {
    this.playerInfoBean = playerInfoBean;
    this.onChatUserControlDoubleClickListener = onChatUserControlDoubleClickListener;
  }

  @FXML
  void initialize() {
    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();
  }

  @FXML
  void onContextMenuRequested(ContextMenuEvent event) {
    ContextMenu contextMenu = fxmlLoader.loadAndGetRoot("chat_user_context_menu.fxml", this);
    contextMenu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @FXML
  void onUsernameClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2) {
      onChatUserControlDoubleClickListener.onChatUserControlDoubleClicked(this);
    }
  }

  @FXML
  void onUserInfo(ActionEvent actionEvent) {
    if (userInfoWindow != null) {
      return;
    }

    UserInfoWindowController controller = userInfoWindowFactory.create(playerInfoBean);

    userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(getScene().getWindow());

    sceneFactory.createScene(userInfoWindow, this, false, CLOSE);

    userInfoWindow.setOnHiding(event -> {
      userInfoWindow = null;
    });

    userInfoWindow.show();
  }

  private void configureClanLabel() {
    playerInfoBean.clanProperty().addListener((observable, oldValue, newValue) -> {
      if (StringUtils.isEmpty(newValue)) {
        clanLabel.setVisible(false);
      } else {
        clanLabel.setText(String.format(CLAN_TAG_FORMAT, newValue));
        clanLabel.setVisible(true);
      }
    });
  }

  private void configureAvatarImageView() {
    playerInfoBean.avatarUrlProperty().addListener((observable, oldValue, newValue) -> {
      setAvatarUrl(newValue);
    });
    setAvatarUrl(playerInfoBean.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(playerInfoBean.getAvatarTooltip());
    avatarTooltip.textProperty().bind(playerInfoBean.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureCountryImageView() {
    playerInfoBean.countryProperty().addListener((observable, oldValue, newValue) -> {
      setCountry(newValue);
    });
    setCountry(playerInfoBean.getCountry());

    Tooltip countryTooltip = new Tooltip(playerInfoBean.getCountry());
    countryTooltip.textProperty().bind(playerInfoBean.countryProperty());

    Tooltip.install(countryImageView, countryTooltip);
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryImageView.setImage(countryFlagService.loadCountryFlag(country));
      countryImageView.setVisible(true);
    }
  }

  private void setAvatarUrl(String avatarUrl) {
    if (StringUtils.isEmpty(avatarUrl)) {
      avatarImageView.setVisible(false);
    } else {
      avatarImageView.setImage(avatarService.loadAvatar(avatarUrl));
      avatarImageView.setVisible(true);
    }
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("chat_user_control.fxml", this);
    usernameLabel.setText(playerInfoBean.getUsername());
  }

  public PlayerInfoBean getPlayerInfoBean() {
    return playerInfoBean;
  }
}
