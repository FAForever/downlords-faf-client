package com.faforever.client.map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class MapBeanTest {

  @Test
  public void testCompareByVersions() {
    MapBean oldestMap = MapBeanBuilder.create().displayName("old map").folderName("map v3").version(null).get();
    MapBean outdatedMap = MapBeanBuilder.create().displayName("test map").folderName("testMap.v0001").version(1).get();
    MapBean newMap = MapBeanBuilder.create().displayName("test map").folderName("testMap.v0002").version(2).get();
    MapBean sameMap = MapBeanBuilder.create().displayName("test map").folderName("testMap.v0002").version(2).get();
    MapBean unknownVersionMap = MapBeanBuilder.create().displayName("test map").folderName("testMap.v0002").version(null).get();

    assertThat(newMap.compareByVersion(outdatedMap),  is(1));
    assertThat(outdatedMap.compareByVersion(newMap),  is(-1));
    assertThat(newMap.compareByVersion(sameMap),  is(0));

    Assert.assertThrows(CompareMapVersionException.class, () -> newMap.compareByVersion(oldestMap));
    Assert.assertThrows(CompareMapVersionException.class, () -> oldestMap.compareByVersion(newMap));
    Assert.assertThrows(CompareMapVersionException.class, () -> outdatedMap.compareByVersion(unknownVersionMap));
  }
}
