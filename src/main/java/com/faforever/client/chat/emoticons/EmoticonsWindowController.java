package com.faforever.client.chat.emoticons;

import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class EmoticonsWindowController implements Controller<VBox> {

  private final EmoticonService emoticonService;
  private final UiService uiService;

  public VBox root;

  private TextInputControl textInputControl;

  @Override
  public void initialize() {
    List<Node> nodes = new ArrayList<>();
    emoticonService.getEmoticonsGroups().forEach(group -> {
      EmoticonsGroupController controller = uiService.loadFxml("theme/chat/emoticons/emoticons_group.fxml");
      controller.setGroup(group, shortcode -> {
        textInputControl.appendText(" ".concat(shortcode).concat(" "));
        textInputControl.requestFocus();
        textInputControl.selectEnd();
      });
      nodes.add(controller.getRoot());
    });
    root.getChildren().addAll(nodes);
  }

  public void associateWith(TextInputControl textInputControl) {
    this.textInputControl = textInputControl;
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
