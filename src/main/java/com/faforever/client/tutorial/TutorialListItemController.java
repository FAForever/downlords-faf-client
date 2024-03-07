package com.faforever.client.tutorial;

import com.faforever.client.domain.api.Tutorial;
import com.faforever.client.fx.NodeController;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.google.common.base.Strings;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TutorialListItemController extends NodeController<Node> {

  public static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
  public GridPane root;
  public ImageView imageView;
  public Label titleLabel;
  public Label ordinalLabel;
  private Tutorial tutorial;
  private final MapService mapService;

  public TutorialListItemController(MapService mapService) {
    this.mapService = mapService;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public Tutorial getTutorial() {
    return tutorial;
  }

  public void setTutorial(final Tutorial tutorial) {
    this.tutorial = tutorial;
    imageView.setImage(getImage(tutorial));
    titleLabel.setText(tutorial.title());
    ordinalLabel.setText(String.valueOf(tutorial.ordinal()));
  }

  private Image getImage(Tutorial tutorial) {
    if (!Strings.isNullOrEmpty(tutorial.imageUrl())) {
      return new Image(tutorial.imageUrl());
    }
    return tutorial.mapVersion() != null ? mapService.loadPreview(tutorial.mapVersion(), PreviewSize.SMALL) : null;
  }
}
