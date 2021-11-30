package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.fx.Controller;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EmoticonController implements Controller<AnchorPane> {

  public AnchorPane root;
  public ImageView emoticonImageView;

  private final Decoder decoder = Base64.getDecoder();

  @Override
  public void initialize() {
  }

  public void setEmoticon(Emoticon emoticon, Consumer<String> onAction) {
    String base64Content = emoticon.getBase64SvgContent();
    emoticonImageView.setImage(new Image(IOUtils.toInputStream(new String(decoder.decode(base64Content)))));
    emoticonImageView.setOnMouseClicked(event -> onAction.accept(emoticon.getShortcode()));
  }

  @Override
  public AnchorPane getRoot() {
    return root;
  }
}
