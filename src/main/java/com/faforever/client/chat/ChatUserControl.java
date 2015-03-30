package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class ChatUserControl extends HBox {

  @Autowired
  FxmlLoader fxmlLoader;

  @FXML
  ImageView countryImageView;

  @FXML
  ImageView avatarImageView;

  @FXML
  ImageView statusImageView;

  @FXML
  Label usernameLabel;

  private ChatUser chatUser;

  public ChatUserControl(ChatUser chatUser) {
    this.chatUser = chatUser;
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("user_entry.fxml", this);
//    countryImageView.setImage(new Image(chatUser.getCountry()));
//    avatarImageView.setImage(new Image(chatUser.getAvatar()));
    usernameLabel.setText(chatUser.getNick());
  }
}
