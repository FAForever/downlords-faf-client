package com.faforever.client.voting;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class VotingSubject {
  private final StringProperty id;
  private final ObjectProperty<OffsetDateTime> createTime;
  private final ObjectProperty<OffsetDateTime> updateTime;
  private final StringProperty subjectKey;
  private final StringProperty subject;
  private final IntegerProperty numberOfVotes;
  private final StringProperty topicUrl;
  private final ObjectProperty<OffsetDateTime> beginOfVoteTime;
  private final ObjectProperty<OffsetDateTime> endOfVoteTime;
  private final IntegerProperty minGamesToVote;
  private final StringProperty descriptionKey;
  private final StringProperty description;
  private final ObservableList<VotingQuestion> votingQuestions;

  public VotingSubject() {
    this.id = new SimpleStringProperty();
    this.createTime = new SimpleObjectProperty<>();
    this.updateTime = new SimpleObjectProperty<>();
    this.subjectKey = new SimpleStringProperty();
    this.subject = new SimpleStringProperty();
    this.numberOfVotes = new SimpleIntegerProperty();
    this.topicUrl = new SimpleStringProperty();
    this.beginOfVoteTime = new SimpleObjectProperty<>();
    this.endOfVoteTime = new SimpleObjectProperty<>();
    this.minGamesToVote = new SimpleIntegerProperty();
    this.descriptionKey = new SimpleStringProperty();
    this.description = new SimpleStringProperty();
    this.votingQuestions = FXCollections.observableArrayList();
  }

  public static VotingSubject fromDto(com.faforever.client.api.dto.VotingSubject dto) {
    VotingSubject votingSubject = new VotingSubject();
    votingSubject.setId(dto.getId());
    votingSubject.setCreateTime(dto.getCreateTime());
    votingSubject.setUpdateTime(dto.getUpdateTime());
    votingSubject.setSubjectKey(dto.getSubjectKey());
    votingSubject.setSubject(dto.getSubject());
    votingSubject.setNumberOfVotes(dto.getNumberOfVotes());
    votingSubject.setTopicUrl(dto.getTopicUrl());
    votingSubject.setBeginOfVoteTime(dto.getBeginOfVoteTime());
    votingSubject.setEndOfVoteTime(dto.getEndOfVoteTime());
    votingSubject.setMinGamesToVote(dto.getMinGamesToVote());
    votingSubject.setDescriptionKey(dto.getDescriptionKey());
    votingSubject.setDescription(dto.getDescription());
    List<VotingQuestion> questions = dto.getVotingQuestions().stream().map(votingQuestion -> VotingQuestion.fromDto(votingQuestion, votingSubject)).collect(Collectors.toList());
    votingSubject.votingQuestions.setAll(questions);
    return votingSubject;
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

  public String getSubjectKey() {
    return subjectKey.get();
  }

  public void setSubjectKey(String subjectKey) {
    this.subjectKey.set(subjectKey);
  }

  public StringProperty subjectKeyProperty() {
    return subjectKey;
  }

  public String getSubject() {
    return subject.get();
  }

  public void setSubject(String subject) {
    this.subject.set(subject);
  }

  public StringProperty subjectProperty() {
    return subject;
  }

  public int getNumberOfVotes() {
    return numberOfVotes.get();
  }

  public void setNumberOfVotes(int numberOfVotes) {
    this.numberOfVotes.set(numberOfVotes);
  }

  public IntegerProperty numberOfVotesProperty() {
    return numberOfVotes;
  }

  public String getTopicUrl() {
    return topicUrl.get();
  }

  public void setTopicUrl(String topicUrl) {
    this.topicUrl.set(topicUrl);
  }

  public StringProperty topicUrlProperty() {
    return topicUrl;
  }

  public OffsetDateTime getBeginOfVoteTime() {
    return beginOfVoteTime.get();
  }

  public void setBeginOfVoteTime(OffsetDateTime beginOfVoteTime) {
    this.beginOfVoteTime.set(beginOfVoteTime);
  }

  public ObjectProperty<OffsetDateTime> beginOfVoteTimeProperty() {
    return beginOfVoteTime;
  }

  public OffsetDateTime getEndOfVoteTime() {
    return endOfVoteTime.get();
  }

  public void setEndOfVoteTime(OffsetDateTime endOfVoteTime) {
    this.endOfVoteTime.set(endOfVoteTime);
  }

  public ObjectProperty<OffsetDateTime> endOfVoteTimeProperty() {
    return endOfVoteTime;
  }

  public int getMinGamesToVote() {
    return minGamesToVote.get();
  }

  public void setMinGamesToVote(int minGamesToVote) {
    this.minGamesToVote.set(minGamesToVote);
  }

  public IntegerProperty minGamesToVoteProperty() {
    return minGamesToVote;
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

  public ObservableList<VotingQuestion> getVotingQuestions() {
    return votingQuestions;
  }
}
