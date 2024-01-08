package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.NodeController;
import com.faforever.client.theme.UiService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class EmoticonsWindowController extends NodeController<VBox> {

  private final EmoticonService emoticonService;
  private final UiService uiService;

  public VBox root;

  private final ObjectProperty<Consumer<Emoticon>> onEmoticonClicked = new SimpleObjectProperty<>();

  @Override
  protected void onInitialize() {
    List<Node> nodes = new ArrayList<>();
    emoticonService.getEmoticonsGroups().forEach(group -> {
      EmoticonsGroupController controller = uiService.loadFxml("theme/chat/emoticons/emoticons_group.fxml");
      controller.setEmoticonsGroup(group);
      controller.onEmoticonClickedProperty().bind(onEmoticonClicked);
      nodes.add(controller.getRoot());
    });
    root.getChildren().addAll(nodes);
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

  @Override
  public VBox getRoot() {
    return root;
  }
}
