package com.faforever.client.mapstruct;

import org.mapstruct.factory.Mappers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MapperSetup {
  private static final Map<Class<?>, Object> INJECTED_MAPPERS = new HashMap<>();

  public static void injectMappers(Object object) throws IllegalAccessException {
    Class<?> objectClass = object.getClass();
    if (objectClass.getName().contains("MockitoMock")) {
      objectClass = objectClass.getSuperclass();
    }

    INJECTED_MAPPERS.put(objectClass, object);
    for (Field field : objectClass.getDeclaredFields()) {
      Class<?> internalMapperType = field.getType();
      if (INJECTED_MAPPERS.containsKey(internalMapperType)) {
        injectMapper(object, field, INJECTED_MAPPERS.get(internalMapperType));
        continue;
      }

      Object internalMapper = Mappers.getMapper(internalMapperType);
      if (internalMapper == null) {
        continue;
      }

      injectMapper(object, field, internalMapper);

      INJECTED_MAPPERS.put(internalMapperType, internalMapper);
      injectMappers(internalMapper);
    }
  }

  private static void injectMapper(Object mapper, Field field, Object internalMapper) throws IllegalAccessException {
    field.setAccessible(true);
    field.set(mapper, internalMapper);
    field.setAccessible(false);
  }
}
