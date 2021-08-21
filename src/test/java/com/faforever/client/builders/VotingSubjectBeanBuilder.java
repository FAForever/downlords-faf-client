package com.faforever.client.builders;

import com.faforever.client.domain.VotingQuestionBean;
import com.faforever.client.domain.VotingSubjectBean;

import java.time.OffsetDateTime;
import java.util.List;


public class VotingSubjectBeanBuilder {
  public static VotingSubjectBeanBuilder create() {
    return new VotingSubjectBeanBuilder();
  }

  private final VotingSubjectBean votingSubjectBean = new VotingSubjectBean();

  public VotingSubjectBeanBuilder defaultValues() {
    return this;
  }

  public VotingSubjectBeanBuilder subjectKey(String subjectKey) {
    votingSubjectBean.setSubjectKey(subjectKey);
    return this;
  }

  public VotingSubjectBeanBuilder subject(String subject) {
    votingSubjectBean.setSubject(subject);
    return this;
  }

  public VotingSubjectBeanBuilder descriptionKey(String descriptionKey) {
    votingSubjectBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public VotingSubjectBeanBuilder description(String description) {
    votingSubjectBean.setDescription(description);
    return this;
  }

  public VotingSubjectBeanBuilder topicUrl(String topicUrl) {
    votingSubjectBean.setTopicUrl(topicUrl);
    return this;
  }

  public VotingSubjectBeanBuilder beginOfVoteTime(OffsetDateTime beginOfVoteTime) {
    votingSubjectBean.setBeginOfVoteTime(beginOfVoteTime);
    return this;
  }

  public VotingSubjectBeanBuilder endOfVoteTime(OffsetDateTime endOfVoteTime) {
    votingSubjectBean.setEndOfVoteTime(endOfVoteTime);
    return this;
  }

  public VotingSubjectBeanBuilder numberOfVotes(int numberOfVotes) {
    votingSubjectBean.setNumberOfVotes(numberOfVotes);
    return this;
  }

  public VotingSubjectBeanBuilder minGamesToVote(int minGamesToVote) {
    votingSubjectBean.setMinGamesToVote(minGamesToVote);
    return this;
  }

  public VotingSubjectBeanBuilder revealWinner(boolean revealWinner) {
    votingSubjectBean.setRevealWinner(revealWinner);
    return this;
  }

  public VotingSubjectBeanBuilder votingSubject(VotingSubjectBean votingSubject) {
    votingSubjectBean.setVotingSubject(votingSubject);
    return this;
  }

  public VotingSubjectBeanBuilder votingQuestions(List<VotingQuestionBean> votingQuestions) {
    votingSubjectBean.setVotingQuestions(votingQuestions);
    return this;
  }

  public VotingSubjectBeanBuilder id(Integer id) {
    votingSubjectBean.setId(id);
    return this;
  }

  public VotingSubjectBeanBuilder createTime(OffsetDateTime createTime) {
    votingSubjectBean.setCreateTime(createTime);
    return this;
  }

  public VotingSubjectBeanBuilder updateTime(OffsetDateTime updateTime) {
    votingSubjectBean.setUpdateTime(updateTime);
    return this;
  }

  public VotingSubjectBean get() {
    return votingSubjectBean;
  }

}

