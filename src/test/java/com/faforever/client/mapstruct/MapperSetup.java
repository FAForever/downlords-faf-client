package com.faforever.client.mapstruct;

import org.mapstruct.factory.Mappers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MapperSetup {
  private static Map<Class<?>, Object> INJECTED_MAPPERS = new HashMap<>();

  public static <T> void injectMappers(T mapper) throws IllegalAccessException {
    Class<T> mapperClass = (Class<T>) mapper.getClass();
    INJECTED_MAPPERS.put(mapperClass, mapper);
    for (Field field : mapperClass.getDeclaredFields()) {
      Class<?> internalMapperType = field.getType();
      if (INJECTED_MAPPERS.containsKey(internalMapperType)) {
        injectMapper(mapper, field, INJECTED_MAPPERS.get(internalMapperType));
        continue;
      }

      Object internalMapper = Mappers.getMapper(internalMapperType);
      if (internalMapper == null) {
        continue;
      }

      injectMapper(mapper, field, internalMapper);

      INJECTED_MAPPERS.put(internalMapperType, internalMapper);
      injectMappers(internalMapper);
    }
  }

  private static <T> void injectMapper(T mapper, Field field, Object internalMapper) throws IllegalAccessException {
    field.setAccessible(true);
    field.set(mapper, internalMapper);
    field.setAccessible(false);
  }
}
