package com.faforever.client.voting;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class VotingQuestion {

  private final StringProperty id;
  private final ObjectProperty<OffsetDateTime> createTime;
  private final ObjectProperty<OffsetDateTime> updateTime;
  private final IntegerProperty numberOfAnswers;
  private final StringProperty question;
  private final StringProperty description;
  private final StringProperty questionKey;
  private final StringProperty descriptionKey;
  private final IntegerProperty maxAnswers;
  private final ObjectProperty<VotingSubject> votingSubject;
  private final ObservableList<VotingChoice> votingChoices;

  public VotingQuestion() {
    this.id = new SimpleStringProperty();
    this.createTime = new SimpleObjectProperty<>();
    this.updateTime = new SimpleObjectProperty<>();
    this.numberOfAnswers = new SimpleIntegerProperty();
    this.question = new SimpleStringProperty();
    this.description = new SimpleStringProperty();
    this.questionKey = new SimpleStringProperty();
    this.descriptionKey = new SimpleStringProperty();
    this.maxAnswers = new SimpleIntegerProperty();
    this.votingSubject = new SimpleObjectProperty<>();
    this.votingChoices = FXCollections.observableArrayList();
  }

  public static VotingQuestion fromDto(com.faforever.client.api.dto.VotingQuestion dto, @Nullable VotingSubject parent) {
    VotingQuestion votingQuestion = new VotingQuestion();
    votingQuestion.setId(dto.getId());
    votingQuestion.setCreateTime(dto.getCreateTime());
    votingQuestion.setUpdateTime(dto.getUpdateTime());
    votingQuestion.setNumberOfAnswers(dto.getNumberOfAnswers());
    votingQuestion.setQuestion(dto.getQuestion());
    votingQuestion.setQuestionKey(dto.getQuestionKey());
    votingQuestion.setDescription(dto.getDescription());
    votingQuestion.setDescriptionKey(dto.getDescriptionKey());
    votingQuestion.setMaxAnswers(dto.getMaxAnswers());
    votingQuestion.setVotingSubject(parent == null ? VotingSubject.fromDto(dto.getVotingSubject()) : parent);
    List<VotingChoice> choices = dto.getVotingChoices().stream().map(votingChoice -> VotingChoice.fromDto(votingChoice, votingQuestion)).collect(Collectors.toList());
    votingQuestion.votingChoices.setAll(choices);
    return votingQuestion;
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

  public int getNumberOfAnswers() {
    return numberOfAnswers.get();
  }

  public void setNumberOfAnswers(int numberOfAnswers) {
    this.numberOfAnswers.set(numberOfAnswers);
  }

  public IntegerProperty numberOfAnswersProperty() {
    return numberOfAnswers;
  }

  public String getQuestion() {
    return question.get();
  }

  public void setQuestion(String question) {
    this.question.set(question);
  }

  public StringProperty questionProperty() {
    return question;
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

  public String getQuestionKey() {
    return questionKey.get();
  }

  public void setQuestionKey(String questionKey) {
    this.questionKey.set(questionKey);
  }

  public StringProperty questionKeyProperty() {
    return questionKey;
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

  public int getMaxAnswers() {
    return maxAnswers.get();
  }

  public void setMaxAnswers(int maxAnswers) {
    this.maxAnswers.set(maxAnswers);
  }

  public IntegerProperty maxAnswersProperty() {
    return maxAnswers;
  }

  public VotingSubject getVotingSubject() {
    return votingSubject.get();
  }

  public void setVotingSubject(VotingSubject votingSubject) {
    this.votingSubject.set(votingSubject);
  }

  public ObjectProperty<VotingSubject> votingSubjectProperty() {
    return votingSubject;
  }

  public ObservableList<VotingChoice> getVotingChoices() {
    return votingChoices;
  }
}
