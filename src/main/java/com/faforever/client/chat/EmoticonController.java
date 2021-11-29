package com.faforever.client.chat;

import com.faforever.client.chat.emojis.Emoticon;
import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EmoticonController implements Controller<AnchorPane> {

  private final UiService uiService;

  public AnchorPane root;
  public ImageView emoticonImageView;

  @Override
  public void initialize() {
  }

  public void setEmoticon(Emoticon emoticon, Consumer<String> onAction) {
    emoticonImageView.setImage(uiService.getThemeImage(emoticon.getSvgFilePath()));
    emoticonImageView.setOnMouseClicked(event -> onAction.accept(emoticon.getShortcode()));
  }

  @Override
  public AnchorPane getRoot() {
    return root;
  }
}
