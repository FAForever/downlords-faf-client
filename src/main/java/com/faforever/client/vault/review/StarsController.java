package com.faforever.client.vault.review;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StarsController implements Controller<Pane> {
  private static final PseudoClass SELECTABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("selectable");
  private final FloatProperty value;
  public StarController star1Controller;
  public StarController star2Controller;
  public StarController star3Controller;
  public StarController star4Controller;
  public StarController star5Controller;
  public Pane starsRoot;
  private List<StarController> starControllers;

  public StarsController() {
    value = new SimpleFloatProperty();
  }

  public void initialize() {
    starControllers = Arrays.asList(star1Controller, star2Controller, star3Controller, star4Controller, star5Controller);
    value.addListener((observable, oldValue, newValue) -> {
      int value = newValue.intValue();
      star1Controller.setFill(value > 0 ? 1 : 0);
      star2Controller.setFill(value > 1 ? 1 : 0);
      star3Controller.setFill(value > 2 ? 1 : 0);
      star4Controller.setFill(value > 3 ? 1 : 0);
      star5Controller.setFill(value > 4 ? 1 : 0);
    });
  }

  @Override
  public Pane getRoot() {
    return starsRoot;
  }

  private void onStarSelected(StarController starController) {
    JavaFxUtil.assertApplicationThread();
    int maxActiveStarIndex = starControllers.indexOf(starController);
    for (int starIndex = 0; starIndex < starControllers.size(); starIndex++) {
      starControllers.get(starIndex).setFill(starIndex <= maxActiveStarIndex ? 1 : 0);
    }
    value.set(countActivatedStars());
  }

  private int countActivatedStars() {
    return (int) starControllers.stream().filter(starController -> starController.getFill() >= 1f).count();
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
