package com.faforever.client.vault.review;

import com.faforever.client.fx.Controller;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StarController implements Controller<Pane> {

  public StackPane starRoot;
  public Node starBackground;
  public Node starFill;

  private final FloatProperty fill;
  private Consumer<StarController> clickedListener;

  public StarController() {
    fill = new SimpleFloatProperty(0);
  }

  @Override
  public Pane getRoot() {
    return starRoot;
  }

  public void initialize() {
    starRoot.widthProperty().addListener((observable, oldValue, newValue) -> {
      Rectangle fillClip = new Rectangle();
      fillClip.widthProperty().bind(fill.multiply(starRoot.getLayoutBounds().getWidth()));
      fillClip.heightProperty().bind(starRoot.heightProperty());

      starFill.setClip(fillClip);
    });
  }

  public void onMouseClicked() {
    Optional.ofNullable(clickedListener).ifPresent(clickedListener -> clickedListener.accept(this));
  }

  public float getFill() {
    return fill.get();
  }

  public void setFill(float ratio) {
    fill.set(ratio);
  }

  public ReadOnlyFloatProperty fillProperty() {
    return fill;
  }

  public void setClickListener(Consumer<StarController> clickListener) {
    this.clickedListener = clickListener;
  }
}
