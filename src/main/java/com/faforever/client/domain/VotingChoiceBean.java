package com.faforever.client.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class VotingChoiceBean extends AbstractEntityBean<VotingChoiceBean> {
  StringProperty choiceTextKey = new SimpleStringProperty();
  StringProperty choiceText = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();
  StringProperty description = new SimpleStringProperty();
  IntegerProperty numberOfAnswers = new SimpleIntegerProperty();
  IntegerProperty ordinal = new SimpleIntegerProperty();

  public String getChoiceTextKey() {
    return choiceTextKey.get();
  }

  public StringProperty choiceTextKeyProperty() {
    return choiceTextKey;
  }

  public void setChoiceTextKey(String choiceTextKey) {
    this.choiceTextKey.set(choiceTextKey);
  }

  public String getChoiceText() {
    return choiceText.get();
  }

  public StringProperty choiceTextProperty() {
    return choiceText;
  }

  public void setChoiceText(String choiceText) {
    this.choiceText.set(choiceText);
  }

  public String getDescriptionKey() {
    return descriptionKey.get();
  }

  public StringProperty descriptionKeyProperty() {
    return descriptionKey;
  }

  public void setDescriptionKey(String descriptionKey) {
    this.descriptionKey.set(descriptionKey);
  }

  public String getDescription() {
    return description.get();
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public int getNumberOfAnswers() {
    return numberOfAnswers.get();
  }

  public IntegerProperty numberOfAnswersProperty() {
    return numberOfAnswers;
  }

  public void setNumberOfAnswers(int numberOfAnswers) {
    this.numberOfAnswers.set(numberOfAnswers);
  }

  public int getOrdinal() {
    return ordinal.get();
  }

  public IntegerProperty ordinalProperty() {
    return ordinal;
  }

  public void setOrdinal(int ordinal) {
    this.ordinal.set(ordinal);
  }
}
