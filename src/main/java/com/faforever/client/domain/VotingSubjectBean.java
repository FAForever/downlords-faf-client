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

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class VotingSubjectBean extends AbstractEntityBean<VotingSubjectBean> {
  StringProperty subjectKey = new SimpleStringProperty();
  StringProperty subject = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();
  StringProperty description = new SimpleStringProperty();
  StringProperty topicUrl = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> beginOfVoteTime = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> endOfVoteTime = new SimpleObjectProperty<>();
  IntegerProperty numberOfVotes = new SimpleIntegerProperty();
  IntegerProperty minGamesToVote = new SimpleIntegerProperty();
  BooleanProperty revealWinner = new SimpleBooleanProperty();
  ObjectProperty<VotingSubjectBean> votingSubject = new SimpleObjectProperty<>();
  ObservableList<VotingChoiceBean> winners = FXCollections.observableArrayList();
  ObservableList<VotingQuestionBean> votingQuestions = FXCollections.observableArrayList();

  public String getSubjectKey() {
    return subjectKey.get();
  }

  public StringProperty subjectKeyProperty() {
    return subjectKey;
  }

  public void setSubjectKey(String subjectKey) {
    this.subjectKey.set(subjectKey);
  }

  public String getSubject() {
    return subject.get();
  }

  public StringProperty subjectProperty() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject.set(subject);
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

  public int getNumberOfVotes() {
    return numberOfVotes.get();
  }

  public IntegerProperty numberOfVotesProperty() {
    return numberOfVotes;
  }

  public void setNumberOfVotes(int numberOfVotes) {
    this.numberOfVotes.set(numberOfVotes);
  }

  public int getMinGamesToVote() {
    return minGamesToVote.get();
  }

  public IntegerProperty minGamesToVoteProperty() {
    return minGamesToVote;
  }

  public void setMinGamesToVote(int minGamesToVote) {
    this.minGamesToVote.set(minGamesToVote);
  }

  public boolean getRevealWinner() {
    return revealWinner.get();
  }

  public BooleanProperty revealWinnerProperty() {
    return revealWinner;
  }

  public void setRevealWinner(boolean revealWinner) {
    this.revealWinner.set(revealWinner);
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

  public ObservableList<VotingQuestionBean> getVotingQuestions() {
    return votingQuestions;
  }

  public void setVotingQuestions(List<VotingQuestionBean> votingQuestions) {
    if (votingQuestions == null) {
      votingQuestions = List.of();
    }
    this.votingQuestions.setAll(votingQuestions);
  }

  public String getTopicUrl() {
    return topicUrl.get();
  }

  public StringProperty topicUrlProperty() {
    return topicUrl;
  }

  public void setTopicUrl(String topicUrl) {
    this.topicUrl.set(topicUrl);
  }

  public OffsetDateTime getBeginOfVoteTime() {
    return beginOfVoteTime.get();
  }

  public ObjectProperty<OffsetDateTime> beginOfVoteTimeProperty() {
    return beginOfVoteTime;
  }

  public void setBeginOfVoteTime(OffsetDateTime beginOfVoteTime) {
    this.beginOfVoteTime.set(beginOfVoteTime);
  }

  public OffsetDateTime getEndOfVoteTime() {
    return endOfVoteTime.get();
  }

  public ObjectProperty<OffsetDateTime> endOfVoteTimeProperty() {
    return endOfVoteTime;
  }

  public void setEndOfVoteTime(OffsetDateTime endOfVoteTime) {
    this.endOfVoteTime.set(endOfVoteTime);
  }

  public boolean isRevealWinner() {
    return revealWinner.get();
  }
}
