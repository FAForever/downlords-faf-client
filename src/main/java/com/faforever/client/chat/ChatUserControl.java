package com.faforever.client.chat;

import com.faforever.client.fx.FxmlLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.PopupWindow;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public class ChatUserControl extends HBox {

  private static final String CLAN_TAG_FORMAT = "[%s]";

  @Resource
  ApplicationContext applicationContext;

  @Resource
  FxmlLoader fxmlLoader;

  @Resource
  AvatarService avatarService;

  @Resource
  CountryFlagService countryFlagService;

  @Resource
  ChatController chatController;

  @FXML
  ImageView countryImageView;

  @FXML
  ImageView avatarImageView;

  @FXML
  Label usernameLabel;

  @FXML
  Label clanLabel;

  private PlayerInfoBean playerInfoBean;

  @FXML
  void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserContextMenuController contextMenuController = applicationContext.getBean(ChatUserContextMenuController.class);
    contextMenuController.setPlayerInfoBean(playerInfoBean);
    contextMenuController.getContextMenu().show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @FXML
  void onUsernameClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      chatController.openPrivateMessageTabForUser(playerInfoBean.getUsername());
    }
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("chat_user_control.fxml", this);
  }

  public PlayerInfoBean getPlayerInfoBean() {
    return playerInfoBean;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();

    usernameLabel.setText(playerInfoBean.getUsername());
  }

  private void configureCountryImageView() {
    playerInfoBean.countryProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setCountry(newValue));
    });
    setCountry(playerInfoBean.getCountry());

    Tooltip countryTooltip = new Tooltip(playerInfoBean.getCountry());
    countryTooltip.textProperty().bind(playerInfoBean.countryProperty());

    Tooltip.install(countryImageView, countryTooltip);
  }

  private void configureAvatarImageView() {
    playerInfoBean.avatarUrlProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setAvatarUrl(newValue));
    });
    setAvatarUrl(playerInfoBean.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(playerInfoBean.getAvatarTooltip());
    avatarTooltip.textProperty().bind(playerInfoBean.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureClanLabel() {
    setClanTag(playerInfoBean.getClan());
    playerInfoBean.clanProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setClanTag(newValue));
    });
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

  private void setClanTag(String newValue) {
    if (StringUtils.isEmpty(newValue)) {
      clanLabel.setVisible(false);
    } else {
      clanLabel.setText(String.format(CLAN_TAG_FORMAT, newValue));
      clanLabel.setVisible(true);
    }
  }
}
