package com.faforever.client.vault.review;

import com.faforever.client.fx.NodeController;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StarController extends NodeController<Pane> {

  public StackPane starRoot;
  public Node starBackground;
  public Node starFill;

  private final FloatProperty fill = new SimpleFloatProperty();
  private final Rectangle fillClip = new Rectangle();

  private Consumer<StarController> clickedListener;

  @Override
  public Pane getRoot() {
    return starRoot;
  }

  @Override
  protected void onInitialize() {
    fillClip.widthProperty()
        .bind(fill.multiply(DoubleExpression.doubleExpression(starRoot.layoutBoundsProperty().map(Bounds::getWidth))));
    fillClip.heightProperty().bind(starRoot.heightProperty());
    starFill.setClip(fillClip);
  }

  public void onMouseClicked() {
    if (clickedListener != null) {
      clickedListener.accept(this);
    }
  }

  public float getFill() {
    return fill.get();
  }

  public void setFill(float ratio) {
    fill.set(ratio);
  }

  public FloatProperty fillProperty() {
    return fill;
  }

  public void setClickListener(Consumer<StarController> clickListener) {
    this.clickedListener = clickListener;
  }
}
