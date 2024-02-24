package com.faforever.client.test;

import com.faforever.commons.api.elide.ElideEndpointBuilder;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
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

  public static <T extends ElideEntity> ArgumentMatcher<ElideNavigator<T>> hasDtoClass(
      Class<? extends ElideEntity> clazz) {
    return builder -> builder != null && builder.getDtoClass().equals(clazz);
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> filterPresent() {
    return new ElideParamMatcher(containsString("filter="));
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasFilter(Condition<?> condition) {
    return new ElideParamMatcher(containsString(String.format("filter=%s", condition.query(new RSQLVisitor()))));
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasSort(String sortProperty, boolean ascending) {
    return new ElideParamListMatcher<>(contains((ascending ? "" : "-") + sortProperty), SORT);
  }

  public static ArgumentMatcher<?> hasIncludes(List<String> includes) {
    return new ElideParamListMatcher<>(containsInAnyOrder(includes.stream()
        .map(Matchers::equalTo)
        .collect(Collectors.toList())), INCLUDE);
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasPageSize(int pageSize) {
    return new ElideParamMatcher(containsString(String.format("page[size]=%d", pageSize)));
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasPageNumber(int pageNumber) {
    return new ElideParamMatcher(containsString(String.format("page[number]=%d", pageNumber)));
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasPageTotals() {
    return new ElideParamMatcher(containsString("page[totals]"));
  }

  public static ArgumentMatcher<ElideNavigatorOnId<?>> hasId(int id) {
    return new ElideIdMatcher(matchesPattern(String.format("^/data/\\w+/%d.*", id)));
  }

  public static ArgumentMatcher<ElideNavigatorOnCollection<?>> hasRelationship(String relationship) {
    return new ElideRelationshipMatcher(matchesPattern(String.format("^/data/\\w+/\\d+/%s.*", relationship)));
  }

  private record ElideParamMatcher(Matcher<String> matcher) implements ArgumentMatcher<ElideNavigatorOnCollection<?>> {
    @Override
    public boolean matches(ElideNavigatorOnCollection<?> argument) {
      return argument != null && matcher.matches(argument.build());
    }
  }

  private record ElideIdMatcher(Matcher<String> matcher) implements ArgumentMatcher<ElideNavigatorOnId<?>> {
    @Override
    public boolean matches(ElideNavigatorOnId<?> argument) {
      return argument != null && matcher.matches(argument.build());
    }
  }


  private record ElideRelationshipMatcher(
      Matcher<String> matcher) implements ArgumentMatcher<ElideNavigatorOnCollection<?>> {
    @Override
    public boolean matches(ElideNavigatorOnCollection<?> argument) {
      return argument != null && matcher.matches(argument.build());
    }
  }

  private record ElideParamListMatcher<T extends ElideEndpointBuilder<?>>(Matcher<Iterable<? extends String>> matcher,
                                                                          String paramPrefix) implements ArgumentMatcher<T> {
    private static final String PARAM_DELIMITER = "&";
    private static final String PROPERTY_DELIMITER = ",";

    @Override
    public boolean matches(T argument) {
      if (argument == null) {
        return false;
      }

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
