package com.faforever.client.builders;

import com.faforever.client.domain.VotingChoiceBean;
import com.faforever.client.domain.VotingQuestionBean;
import com.faforever.client.domain.VotingSubjectBean;

import java.time.OffsetDateTime;
import java.util.List;


public class VotingQuestionBeanBuilder {
  public static VotingQuestionBeanBuilder create() {
    return new VotingQuestionBeanBuilder();
  }

  private final VotingQuestionBean votingQuestionBean = new VotingQuestionBean();

  public VotingQuestionBeanBuilder defaultValues() {
    return this;
  }

  public VotingQuestionBeanBuilder question(String question) {
    votingQuestionBean.setQuestion(question);
    return this;
  }

  public VotingQuestionBeanBuilder questionKey(String questionKey) {
    votingQuestionBean.setQuestionKey(questionKey);
    return this;
  }

  public VotingQuestionBeanBuilder descriptionKey(String descriptionKey) {
    votingQuestionBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public VotingQuestionBeanBuilder description(String description) {
    votingQuestionBean.setDescription(description);
    return this;
  }

  public VotingQuestionBeanBuilder maxAnswers(int maxAnswers) {
    votingQuestionBean.setMaxAnswers(maxAnswers);
    return this;
  }

  public VotingQuestionBeanBuilder ordinal(int ordinal) {
    votingQuestionBean.setOrdinal(ordinal);
    return this;
  }

  public VotingQuestionBeanBuilder alternativeQuestion(boolean alternativeQuestion) {
    votingQuestionBean.setAlternativeQuestion(alternativeQuestion);
    return this;
  }

  public VotingQuestionBeanBuilder votingSubject(VotingSubjectBean votingSubject) {
    votingQuestionBean.setVotingSubject(votingSubject);
    return this;
  }

  public VotingQuestionBeanBuilder winners(List<VotingChoiceBean> winners) {
    votingQuestionBean.setWinners(winners);
    return this;
  }

  public VotingQuestionBeanBuilder votingChoices(List<VotingChoiceBean> votingChoices) {
    votingQuestionBean.setVotingChoices(votingChoices);
    return this;
  }

  public VotingQuestionBeanBuilder id(Integer id) {
    votingQuestionBean.setId(id);
    return this;
  }

  public VotingQuestionBeanBuilder createTime(OffsetDateTime createTime) {
    votingQuestionBean.setCreateTime(createTime);
    return this;
  }

  public VotingQuestionBeanBuilder updateTime(OffsetDateTime updateTime) {
    votingQuestionBean.setUpdateTime(updateTime);
    return this;
  }

  public VotingQuestionBean get() {
    return votingQuestionBean;
  }

}

