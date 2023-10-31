package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.IceSession;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.CoturnServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoturnServiceTest extends ServiceTest {
  @InjectMocks
  private CoturnService instance;

  @Mock
  private FafApiAccessor fafApiAccessor;

  @Spy
  private IceServerMapper iceServerMapper = Mappers.getMapper(IceServerMapper.class);
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(iceServerMapper);
  }

  @Test
  public void testGetActiveCoturns() {
    when(fafApiAccessor.getApiObjects("/ice/server", IceServer.class)).thenReturn(Flux.empty());
    instance.getActiveCoturns();
    verify(fafApiAccessor).getApiObjects("/ice/server", IceServer.class);
  }

  @Test
  public void TestGetSelectedCoturnsNoActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(Mono.just(new IceSession("someSessionId", List.of(otherServer))));

    List<CoturnServer> servers = instance.getSelectedCoturns(123).join();

    assertEquals(1, servers.size());
    assertEquals("0", servers.get(0).getId());
    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }

  @Test
  public void testGetSelectedCoturnsNoneSelected() {
    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(Mono.just(new IceSession("someSessionId", List.of(otherServer))));

    List<CoturnServer> servers = instance.getSelectedCoturns(123).join();

    assertEquals(1, servers.size());
    assertEquals("0", servers.get(0).getId());
    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }

  @Test
  public void testGetSelectedCoturnsActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    CoturnServer selectedServer = new CoturnServer();
    otherServer.setId("1");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(Mono.just(new IceSession("someSessionId", List.of(otherServer, selectedServer))));

    List<CoturnServer> servers = instance.getSelectedCoturns(123).join();

    assertEquals(1, servers.size());
    assertEquals("1", servers.get(0).getId());
    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }
}
