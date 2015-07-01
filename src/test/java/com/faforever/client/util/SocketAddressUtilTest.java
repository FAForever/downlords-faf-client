package com.faforever.client.util;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.faforever.client.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class SocketAddressUtilTest {

  @Test
  public void testUtilityClass() {
    assertThat(SocketAddressUtil.class, isUtilityClass());
  }

  @Test
  public void testToString() throws Exception {
    InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("123.132.121.131"), 54321);

    String string = SocketAddressUtil.toString(address);

    assertEquals("123.132.121.131:54321", string);
  }
}
