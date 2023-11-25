package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.NodeController;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EmoticonController extends NodeController<AnchorPane> {

  private final Font shortcodesFont = new Font(14d);

  public AnchorPane root;
  public ImageView emoticonImageView;

  public void setEmoticon(Emoticon emoticon, Consumer<String> onAction) {
    emoticonImageView.setImage(emoticon.getImage());
    root.setOnMouseClicked(event -> onAction.accept(emoticon.getShortcodes().get(0)));

    displayShortcodesOnHover(emoticon.getShortcodes());
  }

  private void displayShortcodesOnHover(List<String> shortcodes) {
    Tooltip tooltip = new Tooltip();
    tooltip.setText(String.join("\t",shortcodes));
    tooltip.setFont(shortcodesFont);
    tooltip.setShowDuration(Duration.seconds(10));
    tooltip.setShowDelay(Duration.ZERO);
    tooltip.setHideDelay(Duration.ZERO);
    Tooltip.install(root, tooltip);
  }

  @Override
  public AnchorPane getRoot() {
    return root;
  }
}
