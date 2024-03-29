package com.faforever.client.query;

import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
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

import java.util.Optional;

/**
 * Constructs an {@code AND} or {@code OR} query.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LogicalNodeController extends NodeController<Node> {

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

  @Override
  protected void onInitialize() {
    logicalOperatorField.setItems(FXCollections.observableArrayList(LogicalOperator.AND, LogicalOperator.OR));
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

  public Optional<Condition> appendTo(Condition condition) {
    LogicalOperator operator = logicalOperatorField.getValue();
    if (operator == null) {
      return Optional.empty();
    }
    switch (operator) {
      case AND:
        return specificationController.appendTo(condition.and());
      case OR:
        return specificationController.appendTo(condition.or());
      default:
        throw new ProgrammingError("Uncovered operator: " + operator);
    }
  }

  public void setType(Class<?> type) {
    specificationController.setRootType(type);
  }

  public void onRemoveCriteriaButtonClicked() {
    removeCriteriaButtonListener.run();
  }

  enum LogicalOperator {
    AND("query.and"),
    OR("query.or");
    private final String i18nKey;

    LogicalOperator(String i18nKey) {
      this.i18nKey = i18nKey;
    }
  }
}
