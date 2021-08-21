package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class VotingQuestionBean extends AbstractEntityBean<VotingQuestionBean> {
  StringProperty question = new SimpleStringProperty();
  StringProperty questionKey = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();
  StringProperty description = new SimpleStringProperty();
  IntegerProperty maxAnswers = new SimpleIntegerProperty();
  IntegerProperty ordinal = new SimpleIntegerProperty();
  BooleanProperty alternativeQuestion = new SimpleBooleanProperty();
  ObjectProperty<VotingSubjectBean> votingSubject = new SimpleObjectProperty<>();
  ObservableList<VotingChoiceBean> winners = FXCollections.observableArrayList();
  ObservableList<VotingChoiceBean> votingChoices = FXCollections.observableArrayList();

  public String getQuestion() {
    return question.get();
  }

  public StringProperty questionProperty() {
    return question;
  }

  public void setQuestion(String question) {
    this.question.set(question);
  }

  public String getQuestionKey() {
    return questionKey.get();
  }

  public StringProperty questionKeyProperty() {
    return questionKey;
  }

  public void setQuestionKey(String questionKey) {
    this.questionKey.set(questionKey);
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

  public int getMaxAnswers() {
    return maxAnswers.get();
  }

  public IntegerProperty maxAnswersProperty() {
    return maxAnswers;
  }

  public void setMaxAnswers(int maxAnswers) {
    this.maxAnswers.set(maxAnswers);
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

  public boolean getAlternativeQuestion() {
    return alternativeQuestion.get();
  }

  public BooleanProperty alternativeQuestionProperty() {
    return alternativeQuestion;
  }

  public void setAlternativeQuestion(boolean alternativeQuestion) {
    this.alternativeQuestion.set(alternativeQuestion);
  }

  public VotingSubjectBean getVotingSubject() {
    return votingSubject.get();
  }

  public ObjectProperty<VotingSubjectBean> votingSubjectProperty() {
    return votingSubject;
  }

  public void setVotingSubject(VotingSubjectBean votingSubject) {
    this.votingSubject.set(votingSubject);
  }

  public ObservableList<VotingChoiceBean> getWinners() {
    return winners;
  }

  public void setWinners(List<VotingChoiceBean> winners) {
    if (winners == null) {
      winners = List.of();
    }
    this.winners.setAll(winners);
  }

  public ObservableList<VotingChoiceBean> getVotingChoices() {
    return votingChoices;
  }

  public void setVotingChoices(List<VotingChoiceBean> votingChoices) {
    if (votingChoices == null) {
      votingChoices = List.of();
    }
    this.votingChoices.setAll(votingChoices);
  }
}
