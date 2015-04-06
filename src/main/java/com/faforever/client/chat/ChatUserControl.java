package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.legacy.message.Avatar;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

public class ChatUserControl extends HBox {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String CLAN_TAG_FORMAT = "[%s]";

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  AvatarService avatarService;

  @Autowired
  CountryFlagService countryFlagService;

  @FXML
  ImageView countryImageView;

  @FXML
  private ImageView avatarImageView;

  @FXML
  ImageView statusImageView;

  @FXML
  Label usernameLabel;

  @FXML
  Label clanLabel;

  private ChatUser chatUser;

  public ChatUserControl(ChatUser chatUser) {
    this.chatUser = chatUser;
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("chat_user_control.fxml", this);
    usernameLabel.setText(chatUser.getLogin());
  }

  public ChatUser getChatUser() {
    return chatUser;
  }

  public void setAvatar(Avatar avatar) {
    if (avatar == null) {
      return;
    }

    Image image = avatarService.loadAvatar(avatar);
    avatarImageView.setImage(image);
  }

  public void setClan(String clan) {
    if (StringUtils.isEmpty(clan)) {
      clanLabel.setText("");
      return;
    }

    clanLabel.setText(String.format(CLAN_TAG_FORMAT, clan));
  }

  public void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      return;
    }

    countryImageView.setImage(countryFlagService.loadCountryFlag(country));
  }
}
