package com.faforever.client.query;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.commons.api.elide.querybuilder.QueryOperator;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.google.common.collect.ImmutableMap;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

/**
 * Controller for building a specification in the sense of Domain Driven Design, e.g. {@code login == "Someone"} or
 * {@code rating > 500}. The specification can then be converted into a condition to be used in a {@link QBuilder}.
 * Before it can be used, the type of the object to query has to be defined first by using {@link #setRootType(Class)}.
 * This controller consists of a property ChoiceBox, an operator ChoiceBox and a value field. The items available in the
 * operator ChoiceBox depend on the selected property and its respective type in the target class.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SpecificationController implements Controller<Node> {

  private static final Map<QueryOperator, String> operatorToI18nKey = ImmutableMap.<QueryOperator, String>builder()
      .put(QueryOperator.LIKE, "query.contains")
      .put(QueryOperator.EQUALS, "query.equals")
      .put(QueryOperator.UNEQUALS, "query.notEquals")
      .put(QueryOperator.GREATER_THAN, "query.greaterThan")
      .put(QueryOperator.GREATER_EQUALS_THAN, "greaterThanEquals.equals")
      .put(QueryOperator.LESSER_THAN, "query.lessThan")
      .put(QueryOperator.LESSER_EQUALS_THAN, "query.lessThanEquals")
      .put(QueryOperator.IN, "query.in")
      .put(QueryOperator.NOT_IN, "query.notIn")
      .put(QueryOperator.IS_NULL, "query.isNull")
      .put(QueryOperator.NOT_IS_NULL, "query.notIsNull")
      .build();

  private final I18n i18n;
  private final QueryCriteriaService queryCriteriaService;
  private final FilteredList<QueryOperator> comparisonOperators;
  public ComboBox<DtoQueryCriterion> propertyField;
  public ComboBox<QueryOperator> operationField;
  public ComboBox valueField;
  public HBox specificationRoot;
  public DatePicker datePicker;
  private Class<?> rootType;
  private List<DtoQueryCriterion> queryCriteria;

  public SpecificationController(I18n i18n, QueryCriteriaService queryCriteriaService) {
    this.i18n = i18n;
    this.queryCriteriaService = queryCriteriaService;
    comparisonOperators = new FilteredList<>(FXCollections.observableArrayList(operatorToI18nKey.keySet()),
        queryOperator -> false);
  }

  public void initialize() {
    datePicker.setValue(LocalDate.now());
    datePicker.managedProperty().bind(datePicker.visibleProperty());
    datePicker.setVisible(false);

    valueField.managedProperty().bind(valueField.visibleProperty());
    // JFXComboBox throws an exception if the field is bound bidirectionally but editable (or so...)
    valueField.editableProperty().bind(valueField.visibleProperty());

    operationField.setItems(comparisonOperators);

    propertyField.setConverter(new StringConverter<>() {
      @Override
      public String toString(DtoQueryCriterion queryCriterion) {
        if (queryCriterion == null) {
          return "";
        }

        return queryCriterion.getI18nKey();
        //return i18n.get(queryCriterion.getI18nKey());
      }

      @Override
      public DtoQueryCriterion fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    operationField.setConverter(new StringConverter<>() {
      @Override
      public String toString(QueryOperator object) {
        return operatorToI18nKey.get(object);
        //return i18n.get(operatorToI18nKey.get(object));
      }

      @Override
      public QueryOperator fromString(String string) {
        throw new UnsupportedOperationException("Not supported");
      }
    });

    propertyField.valueProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue == null) {
            comparisonOperators.setPredicate(queryOperator -> false);
          } else {
            comparisonOperators.setPredicate(queryOperator -> newValue
                .getSupportedOperators()
                .contains(queryOperator));

            populateValueField(newValue);
          }

        }
    );
  }

  /**
   * If there are predefined values for a property (like an enum or boolean), the value field is populated with the
   * possible values. If its an instant, a date picker will be displayed and the value field will be hidden. The
   * selected date will then be populated to the hidden value field in its proper format.
   */
  private void populateValueField(DtoQueryCriterion criterion) {
    valueField.setVisible(true);
    valueField.getItems().clear();
    valueField.valueProperty().unbind();
    valueField.setValue(null);
    datePicker.setVisible(false);

    Class<?> propertyClass = criterion.getValueType();

    if (ClassUtils.isAssignable(Boolean.class, propertyClass)) {
      valueField.getItems().setAll(Boolean.TRUE, Boolean.FALSE);
      valueField.getSelectionModel().select(0);
    } else if (ClassUtils.isAssignable(Enum.class, propertyClass)) {
      valueField.getItems().setAll(propertyClass.getEnumConstants());
      valueField.getSelectionModel().select(0);
    } else if (ClassUtils.isAssignable(Temporal.class, propertyClass)) {
      datePicker.setVisible(true);
      valueField.setVisible(false);
      valueField.valueProperty()
          .bind(Bindings.createStringBinding(() -> datePicker.getValue().atStartOfDay(ZoneId.systemDefault())
              .format(DateTimeFormatter.ISO_INSTANT), datePicker.valueProperty()));
    }

    if (criterion.getProposals().size() > 0) {
      valueField.getItems().clear();
      valueField.getItems().setAll(criterion.getProposals());
    }
  }

  @Override
  public Node getRoot() {
    return specificationRoot;
  }

  /**
   * Sets the type this controller can build queries for (including types used in relationships).
   */
  public void setRootType(Class<?> rootType) {
    this.rootType = rootType;

    queryCriteria = queryCriteriaService.getCriteria(rootType);

    propertyField.setItems(FXCollections.observableList(queryCriteria));
    propertyField.getSelectionModel().select(0);
    operationField.getSelectionModel().select(0);
  }

}
