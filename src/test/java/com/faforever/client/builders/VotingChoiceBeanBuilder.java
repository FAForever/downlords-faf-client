package com.faforever.client.builders;

import com.faforever.client.domain.VotingChoiceBean;

import java.time.OffsetDateTime;


public class VotingChoiceBeanBuilder {
  public static VotingChoiceBeanBuilder create() {
    return new VotingChoiceBeanBuilder();
  }

  private final VotingChoiceBean votingChoiceBean = new VotingChoiceBean();

  public VotingChoiceBeanBuilder defaultValues() {
    return this;
  }

  public VotingChoiceBeanBuilder choiceTextKey(String choiceTextKey) {
    votingChoiceBean.setChoiceTextKey(choiceTextKey);
    return this;
  }

  public VotingChoiceBeanBuilder choiceText(String choiceText) {
    votingChoiceBean.setChoiceText(choiceText);
    return this;
  }

  public VotingChoiceBeanBuilder descriptionKey(String descriptionKey) {
    votingChoiceBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public VotingChoiceBeanBuilder description(String description) {
    votingChoiceBean.setDescription(description);
    return this;
  }

  public VotingChoiceBeanBuilder numberOfAnswers(int numberOfAnswers) {
    votingChoiceBean.setNumberOfAnswers(numberOfAnswers);
    return this;
  }

  public VotingChoiceBeanBuilder ordinal(int ordinal) {
    votingChoiceBean.setOrdinal(ordinal);
    return this;
  }

  public VotingChoiceBeanBuilder id(Integer id) {
    votingChoiceBean.setId(id);
    return this;
  }

  public VotingChoiceBeanBuilder createTime(OffsetDateTime createTime) {
    votingChoiceBean.setCreateTime(createTime);
    return this;
  }

  public VotingChoiceBeanBuilder updateTime(OffsetDateTime updateTime) {
    votingChoiceBean.setUpdateTime(updateTime);
    return this;
  }

  public VotingChoiceBean get() {
    return votingChoiceBean;
  }

}

