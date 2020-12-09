package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import lombok.Data;
import org.controlsfx.control.CheckListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Constructs an {@code AND} or {@code OR} query.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class CategoryFilterController implements FilterNodeController {

  private final I18n i18n;

  public CheckListView<String> checkListView;
  public MenuButton menu;
  private String propertyName;
  private LinkedHashMap<String, String> itemMap;

  public void setItems(List<String> items) {
    itemMap = null;
    checkListView.getItems().setAll(items);
  }

  public void setItems(LinkedHashMap<String, String> items) {
    itemMap = items;
    checkListView.getItems().setAll(items.keySet());
  }


  public Optional<List<Condition>> getCondition() {
    QBuilder qBuilder = new QBuilder<>();
    StringProperty property = qBuilder.string(propertyName);
    ArrayList<String> values = new ArrayList<>(checkListView.getCheckModel().getCheckedItems());
    if (!values.isEmpty()) {
      if (!menu.getStyleClass().contains("query-filter-selected")) {
        menu.getStyleClass().add("query-filter-selected");
      }
      if (itemMap != null) {
        values = values.stream().map(s -> itemMap.get(s)).collect(Collectors.toCollection(ArrayList::new));
      }
      return Optional.of(Collections.singletonList(property.in(values.toArray())));
    } else {
      menu.getStyleClass().removeIf(styleClass -> styleClass.equals("query-filter-selected"));
      return Optional.empty();
    }
  }

  public void addQueryListener(InvalidationListener queryListener) {
    checkListView.getCheckModel().getCheckedItems().addListener(queryListener);
  }

  public void clear() {
    checkListView.getItems().forEach(item -> checkListView.getItemBooleanProperty(item).setValue(false));
  }

  public void setTitle(String title) {
    menu.textProperty().unbind();
    menu.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("query.categoryFilter", title, String.join(", ", checkListView.getCheckModel().getCheckedItems())), checkListView.getCheckModel().getCheckedItems()));
  }

  @Override
  public Node getRoot() {
    return menu;
  }

}
