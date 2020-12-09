package com.faforever.client.query;

import com.faforever.client.fx.Controller;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Constructs an {@code AND} or {@code OR} query.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public interface FilterNodeController extends Controller<Node> {

  Optional<List<Condition>> getCondition();

  void addQueryListener(InvalidationListener queryListener);

  void setTitle(String title);

  void clear();
}
