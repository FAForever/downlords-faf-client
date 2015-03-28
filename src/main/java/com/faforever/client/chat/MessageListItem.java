package com.faforever.client.chat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;

public class MessageListItem extends HBox {

  private static final DateTimeFormatter SHORT_TIME_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

  @FXML
  private Label timeLabel;

  @FXML
  private ImageView avatarImageView;

  @FXML
  private Label messageLabel;

  @FXML
  private Label senderLabel;

  public MessageListItem(Instant time, Image avatar, String sender, String message) {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/message_list_item.fxml"));
    fxmlLoader.setController(this);
    fxmlLoader.setRoot(this);

    try {
      fxmlLoader.load();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }

    TemporalAccessor localDateTime = ZonedDateTime.ofInstant(time, TimeZone.getDefault().toZoneId());
    timeLabel.setText(SHORT_TIME_FORMAT.format(localDateTime));
    avatarImageView.setImage(avatar);
    senderLabel.setText(sender);
    messageLabel.setText(message);
  }
}
