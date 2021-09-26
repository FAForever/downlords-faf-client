package com.faforever.client.test;

import com.faforever.commons.api.elide.ElideEndpointBuilder;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import lombok.Value;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;

public final class ElideMatchers {

  public static final String INCLUDE = "include=";
  public static final String SORT = "sort=";

  public static ArgumentMatcher<ElideNavigator<?>> hasDtoClass(Class<? extends ElideEntity> clazz) {
    return builder -> builder.getDtoClass().equals(clazz);
  }

  public static ElideParamMatcher filterPresent() {
    return new ElideParamMatcher(containsString("filter="));
  }

  public static ElideParamMatcher hasFilter(Condition<?> condition) {
    return new ElideParamMatcher(containsString(String.format("filter=%s", condition.query(new RSQLVisitor()))));
  }

  public static ElideParamListMatcher<ElideNavigatorOnCollection<?>> hasSort(String sortProperty, boolean ascending) {
    return new ElideParamListMatcher<>(contains((ascending ? "" : "-") + sortProperty), SORT);
  }

  public static ElideParamListMatcher<?> hasIncludes(List<String> includes) {
    return new ElideParamListMatcher<>(containsInAnyOrder(includes.stream().map(Matchers::equalTo).collect(Collectors.toList())), INCLUDE);
  }

  public static ElideParamMatcher hasPageSize(int pageSize) {
    return new ElideParamMatcher(containsString(String.format("page[size]=%d", pageSize)));
  }

  public static ElideParamMatcher hasPageNumber(int pageNumber) {
    return new ElideParamMatcher(containsString(String.format("page[number]=%d", pageNumber)));
  }

  public static ElideParamMatcher hasPageTotals() {
    return new ElideParamMatcher(containsString("page[totals]"));
  }

  public static ElideIdMatcher hasId(int id) {
    return new ElideIdMatcher(matchesPattern(String.format("^/data/\\w+/%d.*", id)));
  }

  public static ElideRelationshipMatcher hasRelationship(String relationship) {
    return new ElideRelationshipMatcher(matchesPattern(String.format("^/data/\\w+/\\d+/%s.*", relationship)));
  }

  @Value
  public static class ElideParamMatcher implements ArgumentMatcher<ElideNavigatorOnCollection<?>> {
    Matcher<String> matcher;

    @Override
    public boolean matches(ElideNavigatorOnCollection<?> argument) {
      return matcher.matches(argument.build());
    }
  }

  @Value
  public static class ElideIdMatcher implements ArgumentMatcher<ElideNavigatorOnId<?>> {
    Matcher<String> matcher;

    @Override
    public boolean matches(ElideNavigatorOnId<?> argument) {
      return matcher.matches(argument.build());
    }
  }

  @Value
  public static class ElideRelationshipMatcher implements ArgumentMatcher<ElideNavigatorOnCollection<?>> {
    Matcher<String> matcher;

    @Override
    public boolean matches(ElideNavigatorOnCollection<?> argument) {
      return matcher.matches(argument.build());
    }
  }

  @Value
  private static class ElideParamListMatcher<T extends ElideEndpointBuilder<?>> implements ArgumentMatcher<T> {
    private static final String PARAM_DELIMITER = "&";
    private static final String PROPERTY_DELIMITER = ",";
    Matcher<Iterable<? extends String>> matcher;
    String paramPrefix;

    @Override
    public boolean matches(T argument) {
      String endpoint = argument.build();
      assertThat(endpoint, containsString(paramPrefix));
      int startIndex = endpoint.indexOf(paramPrefix) + paramPrefix.length();
      int endIndex = endpoint.indexOf(PARAM_DELIMITER, startIndex);
      if (endIndex == -1) {
        endIndex = endpoint.length();
      }
      List<String> properties = Arrays.asList(endpoint.substring(startIndex, endIndex).split(PROPERTY_DELIMITER));
      return matcher.matches(properties);
    }
  }

}
