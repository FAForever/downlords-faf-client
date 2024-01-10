package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.NodeController;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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

  private final EmoticonService emoticonService;

  public AnchorPane root;
  public ImageView emoticonImageView;

  private final Tooltip shortCodesTooltip = new Tooltip();
  private final ObjectProperty<Consumer<Emoticon>> onEmoticonClicked = new SimpleObjectProperty<>();
  private final ObjectProperty<Emoticon> emoticon = new SimpleObjectProperty<>();
  private final IntegerProperty emoticonSize = new SimpleIntegerProperty(36);
  private final ObjectProperty<Insets> emoticonPadding = new SimpleObjectProperty<>(new Insets(5, 5, 5, 5));

  @Override
  protected void onInitialize() {
    root.onMouseClickedProperty()
        .bind(onEmoticonClicked.flatMap(onEmoticonClicked -> emoticon.map(
            emoticon -> (EventHandler<MouseEvent>) event -> onEmoticonClicked.accept(emoticon))).when(showing));
    emoticonImageView.imageProperty()
                     .bind(emoticon.map(Emoticon::shortcodes)
                                   .map(List::getFirst)
                                   .map(emoticonService::getImageByShortcode)
                                   .when(showing));
    emoticonImageView.fitHeightProperty().bind(emoticonSize);
    emoticonImageView.fitWidthProperty().bind(emoticonSize);
    root.paddingProperty().bind(emoticonPadding);

    shortCodesTooltip.textProperty()
                     .bind(emoticon.map(Emoticon::shortcodes)
                                   .map(shortCodes -> String.join("\t", shortCodes))
                                   .when(showing));

    shortCodesTooltip.setFont(new Font(14d));
    shortCodesTooltip.setShowDuration(Duration.seconds(10));
    shortCodesTooltip.setShowDelay(Duration.ZERO);
    shortCodesTooltip.setHideDelay(Duration.ZERO);
    Tooltip.install(root, shortCodesTooltip);
  }

  public void setEmoticon(String shortcode) {
    setEmoticon(emoticonService.getEmoticonByShortcode(shortcode));
  }

  public void setEmoticon(Emoticon emoticon) {
    this.emoticon.set(emoticon);
  }

  public Emoticon getEmoticon() {
    return emoticon.get();
  }

  public ObjectProperty<Emoticon> emoticonProperty() {
    return emoticon;
  }

  public void setOnEmoticonClicked(Consumer<Emoticon> onEmoticonClicked) {
    this.onEmoticonClicked.set(onEmoticonClicked);
  }

  public Consumer<Emoticon> getOnEmoticonClicked() {
    return onEmoticonClicked.get();
  }

  public ObjectProperty<Consumer<Emoticon>> onEmoticonClickedProperty() {
    return onEmoticonClicked;
  }

  public int getEmoticonSize() {
    return emoticonSize.get();
  }

  public IntegerProperty emoticonSizeProperty() {
    return emoticonSize;
  }

  public void setEmoticonSize(int emoticonSize) {
    this.emoticonSize.set(emoticonSize);
  }

  public Insets getEmoticonPadding() {
    return emoticonPadding.get();
  }

  public ObjectProperty<Insets> emoticonPaddingProperty() {
    return emoticonPadding;
  }

  public void setEmoticonPadding(Insets emoticonPadding) {
    this.emoticonPadding.set(emoticonPadding);
  }

  @Override
  public AnchorPane getRoot() {
    return root;
  }
}
