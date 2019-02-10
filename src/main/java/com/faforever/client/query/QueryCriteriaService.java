package com.faforever.client.query;

import com.faforever.commons.api.elide.querybuilder.ElideEntityScanner;
import com.faforever.commons.api.elide.querybuilder.QueryCriterion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueryCriteriaService implements InitializingBean {

  private final ElideEntityScanner elideEntityScanner;

  private Map<Class<?>, List<DtoQueryCriterion>> queryCriteriaByRootClass;

  public QueryCriteriaService() {
    this.elideEntityScanner = new ElideEntityScanner(DtoQueryCriterion.class);
  }

  @Override
  public void afterPropertiesSet() {
    queryCriteriaByRootClass = new HashMap<>();

    queryCriteriaByRootClass.put(com.faforever.commons.api.dto.Map.class,
        mapToDtoQueryCriteria(elideEntityScanner.scan(com.faforever.commons.api.dto.Map.class))
    );

    queryCriteriaByRootClass.put(com.faforever.commons.api.dto.Mod.class,
        mapToDtoQueryCriteria(elideEntityScanner.scan(com.faforever.commons.api.dto.Map.class))
    );
    queryCriteriaByRootClass.put(com.faforever.commons.api.dto.Game.class,
        mapToDtoQueryCriteria(elideEntityScanner.scan(com.faforever.commons.api.dto.Game.class))
    );
  }

  private List<DtoQueryCriterion> mapToDtoQueryCriteria(List<QueryCriterion> collection) {
    // We are guaranteed to have pure collection of DtoQueryCriterion as we gave it in ElideEntityScanner constructor
    return collection.stream()
        .map(o -> (DtoQueryCriterion) o)
        .collect(Collectors.toList());
  }

  public List<DtoQueryCriterion> getCriteria(Class<?> dtoClass) {
    return queryCriteriaByRootClass.getOrDefault(dtoClass, Collections.emptyList());
  }
}
