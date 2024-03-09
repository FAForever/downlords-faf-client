package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.theme.UiService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
public class EmoticonsGroupController extends NodeController<VBox> {

  private final UiService uiService;
  private final PlatformService platformService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public VBox root;
  public Label groupLabel;
  public HBox attributionPane;
  public Hyperlink attributionHyperlink;
  public FlowPane emoticonsPane;

  private final ObjectProperty<EmoticonsGroup> emoticonsGroup = new SimpleObjectProperty<>();
  private final ObjectProperty<Consumer<Emoticon>> onEmoticonClicked = new SimpleObjectProperty<Consumer<Emoticon>>();

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(attributionPane);
    ObservableValue<String> attribution = emoticonsGroup.map(EmoticonsGroup::attributionUrl);
    attributionHyperlink.textProperty().bind(attribution.when(showing));
    attributionHyperlink.onActionProperty()
                        .bind(attribution.map(
                                             url -> (EventHandler<ActionEvent>) event -> platformService.showDocument(url))
                                         .when(showing));
    attributionPane.visibleProperty().bind(attributionHyperlink.textProperty().isNotEmpty().when(showing));
    groupLabel.textProperty().bind(emoticonsGroup.map(EmoticonsGroup::name));
    emoticonsGroup.subscribe(this::populateEmoticons);
  }

  public EmoticonsGroup getEmoticonsGroup() {
    return emoticonsGroup.get();
  }

  public ObjectProperty<EmoticonsGroup> emoticonsGroupProperty() {
    return emoticonsGroup;
  }

  public void setEmoticonsGroup(EmoticonsGroup emoticonsGroup) {
    this.emoticonsGroup.set(emoticonsGroup);
  }

  public Consumer<Emoticon> getOnEmoticonClicked() {
    return onEmoticonClicked.get();
  }

  public ObjectProperty<Consumer<Emoticon>> onEmoticonClickedProperty() {
    return onEmoticonClicked;
  }

  public void setOnEmoticonClicked(Consumer<Emoticon> onEmoticonClicked) {
    this.onEmoticonClicked.set(onEmoticonClicked);
  }

  private void populateEmoticons(EmoticonsGroup group) {
    if (group == null) {
      fxApplicationThreadExecutor.execute(() -> emoticonsPane.getChildren().clear());
    } else {
      List<AnchorPane> emoticonViewList = group.emoticons().stream().map(emoticon -> {
        EmoticonController controller = uiService.loadFxml("theme/chat/emoticons/emoticon.fxml");
        controller.setEmoticon(emoticon);
        controller.onEmoticonClickedProperty().bind(onEmoticonClicked);
        return controller.getRoot();
      }).toList();
      fxApplicationThreadExecutor.execute(() -> emoticonsPane.getChildren().setAll(emoticonViewList));
    }
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
