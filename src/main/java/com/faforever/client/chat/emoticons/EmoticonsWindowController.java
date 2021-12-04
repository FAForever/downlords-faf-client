package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
public class EmoticonsWindowController implements Controller<VBox> {

  private final EmoticonService emoticonService;
  private final UiService uiService;

  public VBox root;

  @Setter
  private TextInputControl textInputControl;

  @Override
  public void initialize() {
    List<Node> nodes = new ArrayList<>();
    emoticonService.getEmoticonsGroups().forEach(group -> {
      EmoticonsGroupController controller = uiService.loadFxml("theme/chat/emoticons/emoticons_group.fxml");
      controller.setGroup(group, onEmoticonClicked());
      nodes.add(controller.getRoot());
    });
    root.getChildren().addAll(nodes);
  }

  @VisibleForTesting
  protected Consumer<String> onEmoticonClicked() {
    return shortcode -> {
      textInputControl.appendText(" " + shortcode + " ");
      textInputControl.requestFocus();
      textInputControl.selectEnd();
    };
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
