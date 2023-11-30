package com.faforever.client.vault.review;

import com.faforever.client.fx.NodeController;
import com.faforever.client.theme.UiService;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class StarsController extends NodeController<Pane> {
  private static final PseudoClass SELECTABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("selectable");

  private final UiService uiService;

  private final FloatProperty value = new SimpleFloatProperty();

  public Pane starsRoot;
  private List<StarController> starControllers;

  @Override
  protected void onInitialize() {
    starControllers = IntStream.range(0, 5).mapToObj(index -> {
      StarController starController = uiService.loadFxml("theme/vault/review/star.fxml");
      starController.fillProperty()
          .bind(value.subtract(index).map(val -> Math.min(1f, val.floatValue())).map(val -> Math.max(0f, val)));
      starsRoot.getChildren().add(starController.getRoot());
      return starController;
    }).toList();
  }

  @Override
  public Pane getRoot() {
    return starsRoot;
  }

  private void onStarSelected(StarController starController) {
    value.set(starControllers.indexOf(starController) + 1);
  }

  public float getValue() {
    return value.get();
  }

  public void setValue(float value) {
    this.value.set(value);
  }

  public FloatProperty valueProperty() {
    return value;
  }

  public void setSelectable(boolean selectable) {
    starsRoot.pseudoClassStateChanged(SELECTABLE_PSEUDO_CLASS, selectable);
    if (selectable) {
      starControllers.forEach(starController -> starController.setClickListener(this::onStarSelected));
    } else {
      starControllers.forEach(starController -> starController.setClickListener(null));
    }
  }
}
