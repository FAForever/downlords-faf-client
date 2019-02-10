package com.faforever.client.query;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.ProgrammingError;
import com.faforever.commons.api.elide.querybuilder.QueryBuilder;
import com.faforever.commons.api.elide.querybuilder.QueryElement;
import com.faforever.commons.api.elide.querybuilder.QueryOperator;
import com.faforever.commons.api.elide.querybuilder.QueryOperator.ArgumentAmount;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.Collections;

import static com.faforever.client.query.LogicalNodeController.LogicalOperator.AND;
import static com.faforever.client.query.LogicalNodeController.LogicalOperator.OR;

/**
 * Constructs an {@code AND} or {@code OR} query.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LogicalNodeController implements Controller<Node> {

  private final I18n i18n;
  public ComboBox<LogicalOperator> logicalOperatorField;
  public SpecificationController specificationController;
  public Pane logicalNodeRoot;
  public Button removeCriteriaButton;

  @Setter
  private Runnable removeCriteriaButtonListener;

  public LogicalNodeController(I18n i18n) {
    this.i18n = i18n;
  }

  public void initialize() {
    logicalOperatorField.setItems(FXCollections.observableArrayList(AND, OR));
    logicalOperatorField.getSelectionModel().select(0);
    logicalOperatorField.setConverter(new StringConverter<>() {
      @Override
      public String toString(LogicalOperator object) {
        if (object == null) {
          return "";
        }
        return i18n.get(object.i18nKey);
      }

      @Override
      public LogicalOperator fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });
  }

  @Override
  public Node getRoot() {
    return logicalNodeRoot;
  }

  public QueryBuilder appendTo(QueryBuilder queryBuilder) {
    LogicalOperator operator = logicalOperatorField.getValue();

    if (operator == null) {
      return queryBuilder;
    }

    switch (operator) {
      case AND:
        return queryBuilder.and(buildQueryElement());
      case OR:
        return queryBuilder.or(buildQueryElement());
      default:
        throw new ProgrammingError("Uncovered operator: " + operator);
    }
  }

  public QueryElement buildQueryElement() {
    return () -> {
      DtoQueryCriterion criterion = specificationController
          .propertyField.getSelectionModel().getSelectedItem();

      QueryOperator operator = specificationController.operationField
          .getSelectionModel().getSelectedItem();

      Object value = specificationController.valueField.getValue();

      if (criterion == null || operator == null || value == null) {
        return "";
      }

      String valueText = value.toString();
      Object typedValue = valueText;

      Class<?> propertyClass = criterion.getValueType();
      if (ClassUtils.isAssignable(Boolean.class, propertyClass)) {
        typedValue = Boolean.parseBoolean(valueText);
      } else if (ClassUtils.isAssignable(Enum.class, propertyClass)) {
        try {
          typedValue = Enum.valueOf((Class<? extends Enum>) propertyClass, valueText);
        } catch (IllegalArgumentException | NullPointerException e) {
          typedValue = null;
        }
      } else if (ClassUtils.isAssignable(Temporal.class, propertyClass)) {
        try {
          typedValue = LocalDateTime.parse(valueText, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        } catch (Exception e) {
          typedValue = null;
        }
      }

      if (operator.getArgumentAmount() != ArgumentAmount.NONE && typedValue == null) {
        return "";
      }

      return criterion.createRsql(operator, Collections.singletonList(typedValue));
    };
  }

  public void setType(Class<?> type) {
    specificationController.setRootType(type);
  }

  public void onRemoveCriteriaButtonClicked() {
    removeCriteriaButtonListener.run();
  }

  public enum LogicalOperator {
    AND("query.and"),
    OR("query.or");
    private final String i18nKey;

    LogicalOperator(String i18nKey) {
      this.i18nKey = i18nKey;
    }
  }
}
