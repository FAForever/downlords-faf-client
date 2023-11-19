package com.faforever.client.query;

import com.faforever.client.fx.NodeController;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;

import java.util.List;
import java.util.Optional;

/**
 * Constructs an {@code AND} or {@code OR} query.
 */
public abstract class FilterNodeController extends NodeController<Node> {

  public abstract Optional<List<Condition>> getCondition();

  public abstract void addQueryListener(InvalidationListener queryListener);

  public abstract void setTitle(String title);

  public abstract void clear();
}
