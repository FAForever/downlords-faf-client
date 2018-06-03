package com.faforever.client.voting;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.OffsetDateTime;

public class VotingChoice {
  private final StringProperty id;
  private final ObjectProperty<OffsetDateTime> createTime;
  private final ObjectProperty<OffsetDateTime> updateTime;
  private final StringProperty choiceTextKey;
  private final StringProperty choiceText;
  private final StringProperty descriptionKey;
  private final StringProperty description;
  private final IntegerProperty numberOfAnswers;
  private final IntegerProperty ordinal;
  private final ObjectProperty<VotingQuestion> votingQuestion;

  public VotingChoice() {
    this.id = new SimpleStringProperty();
    this.createTime = new SimpleObjectProperty<>();
    this.updateTime = new SimpleObjectProperty<>();
    this.choiceTextKey = new SimpleStringProperty();
    this.choiceText = new SimpleStringProperty();
    this.descriptionKey = new SimpleStringProperty();
    this.description = new SimpleStringProperty();
    this.numberOfAnswers = new SimpleIntegerProperty();
    this.ordinal = new SimpleIntegerProperty();
    this.votingQuestion = new SimpleObjectProperty<>();
  }

  public static VotingChoice fromDto(com.faforever.client.api.dto.VotingChoice dto, VotingQuestion parent) {
    VotingChoice votingChoice = new VotingChoice();
    votingChoice.setId(dto.getId());
    votingChoice.setCreateTime(dto.getCreateTime());
    votingChoice.setUpdateTime(dto.getUpdateTime());
    votingChoice.setChoiceText(dto.getChoiceText());
    votingChoice.setChoiceTextKey(dto.getChoiceTextKey());
    votingChoice.setDescription(dto.getDescription());
    votingChoice.setDescriptionKey(dto.getDescriptionKey());
    votingChoice.setNumberOfAnswers(dto.getNumberOfAnswers());
    votingChoice.setOrdinal(dto.getOrdinal());
    votingChoice.setVotingQuestion(parent == null ? VotingQuestion.fromDto(dto.getVotingQuestion(), null) : parent);
    return votingChoice;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }

  public OffsetDateTime getCreateTime() {
    return createTime.get();
  }

  public void setCreateTime(OffsetDateTime createTime) {
    this.createTime.set(createTime);
  }

  public ObjectProperty<OffsetDateTime> createTimeProperty() {
    return createTime;
  }

  public OffsetDateTime getUpdateTime() {
    return updateTime.get();
  }

  public void setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime.set(updateTime);
  }

  public ObjectProperty<OffsetDateTime> updateTimeProperty() {
    return updateTime;
  }

  public String getChoiceTextKey() {
    return choiceTextKey.get();
  }

  public void setChoiceTextKey(String choiceTextKey) {
    this.choiceTextKey.set(choiceTextKey);
  }

  public StringProperty choiceTextKeyProperty() {
    return choiceTextKey;
  }

  public String getChoiceText() {
    return choiceText.get();
  }

  public void setChoiceText(String choiceText) {
    this.choiceText.set(choiceText);
  }

  public StringProperty choiceTextProperty() {
    return choiceText;
  }

  public String getDescriptionKey() {
    return descriptionKey.get();
  }

  public void setDescriptionKey(String descriptionKey) {
    this.descriptionKey.set(descriptionKey);
  }

  public StringProperty descriptionKeyProperty() {
    return descriptionKey;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public int getNumberOfAnswers() {
    return numberOfAnswers.get();
  }

  public void setNumberOfAnswers(int numberOfAnswers) {
    this.numberOfAnswers.set(numberOfAnswers);
  }

  public IntegerProperty numberOfAnswersProperty() {
    return numberOfAnswers;
  }

  public int getOrdinal() {
    return ordinal.get();
  }

  public void setOrdinal(int ordinal) {
    this.ordinal.set(ordinal);
  }

  public IntegerProperty ordinalProperty() {
    return ordinal;
  }

  public VotingQuestion getVotingQuestion() {
    return votingQuestion.get();
  }

  public void setVotingQuestion(VotingQuestion votingQuestion) {
    this.votingQuestion.set(votingQuestion);
  }

  public ObjectProperty<VotingQuestion> votingQuestionProperty() {
    return votingQuestion;
  }
}
