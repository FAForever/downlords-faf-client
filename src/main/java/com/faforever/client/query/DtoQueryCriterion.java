package com.faforever.client.query;

import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.querybuilder.QueryCriterion;
import com.faforever.commons.api.elide.querybuilder.QueryOperator;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class DtoQueryCriterion<T> implements QueryCriterion<T> {
  private final static String I18N_PREFIX = "apiQuery.";

  private Class<? extends ElideEntity> rootClass;

  private String apiName;

  private Class<T> valueType;

  private Set<QueryOperator> supportedOperators;

  private List<T> proposals;

  private boolean allowsOnlyProposedValues;

  private boolean advancedFilter;

  private int order;

  public String getI18nKey() {
    return I18N_PREFIX + getId();
  }
}
