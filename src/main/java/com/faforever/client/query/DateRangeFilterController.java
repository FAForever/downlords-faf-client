package com.faforever.client.query;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.InstantProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuButton;
import lombok.Data;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class DateRangeFilterController implements FilterNodeController {

  private final I18n i18n;
  private final TimeService timeService;

  public MenuButton menu;
  public DatePicker afterDate;
  public DatePicker beforeDate;
  private String propertyName;
  private int initialYearsBefore;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(menu);
    beforeDate.setValue(null);
    afterDate.setValue(null);
  }

  public Optional<List<Condition>> getCondition() {
    List<Condition> conditions = new ArrayList<>();
    if (afterDate.getValue() != null) {
      QBuilder qBuilder = new QBuilder<>();
      InstantProperty property = qBuilder.instant(propertyName);
      conditions.add(property.after(afterDate.getValue().atStartOfDay().toInstant(ZoneOffset.UTC), false));
    }
    if (beforeDate.getValue() != null) {
      QBuilder qBuilder = new QBuilder<>();
      InstantProperty property = qBuilder.instant(propertyName);
      conditions.add(property.before(beforeDate.getValue().atStartOfDay().toInstant(ZoneOffset.UTC), false));
    }
    if (!conditions.isEmpty()) {
      if (!menu.getStyleClass().contains("query-filter-selected")) {
        menu.getStyleClass().add("query-filter-selected");
      }
      return Optional.of(conditions);
    } else {
      menu.getStyleClass().removeIf(styleClass -> styleClass.equals("query-filter-selected"));
      return Optional.empty();
    }
  }

  public void addQueryListener(InvalidationListener queryListener) {
    afterDate.valueProperty().addListener(queryListener);
    beforeDate.valueProperty().addListener(queryListener);
  }

  public void clear() {
    beforeDate.setValue(null);
    if (initialYearsBefore > 0) {
      afterDate.setValue(LocalDate.now().minus(initialYearsBefore, ChronoUnit.YEARS));
    } else {
      afterDate.setValue(null);
    }
  }

  public void setTitle(String title) {
    menu.textProperty().unbind();
    menu.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("query.dateRangeFilter", title, timeService.asDate(afterDate.getValue()), timeService.asDate(beforeDate.getValue())), afterDate.valueProperty(), beforeDate.valueProperty()));
  }

  public void setBeforeDate(LocalDate date) {
    beforeDate.setValue(date);
  }

  public void setAfterDate(LocalDate date) {
    afterDate.setValue(date);
  }

  public void setInitialYearsBefore(int initialYearsBefore) {
    this.initialYearsBefore = initialYearsBefore;
    afterDate.setValue(LocalDate.now().minus(initialYearsBefore, ChronoUnit.YEARS));
  }

  @Override
  public Node getRoot() {
    return menu;
  }

}
