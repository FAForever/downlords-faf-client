package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.CoturnServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    instance.getActiveCoturns();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(CoturnServer.class)));
  }

  @Test
  public void TestGetSelectedCoturnsNoActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(otherServer));

    List<CoturnServer> servers = instance.getSelectedCoturns().join();

    assertEquals(1, servers.size());
    assertEquals("0", servers.get(0).getId());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(CoturnServer.class)));
  }

  @Test
  public void testGetSelectedCoturnsNoneSelected() {
    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(otherServer));

    List<CoturnServer> servers = instance.getSelectedCoturns().join();

    assertEquals(1, servers.size());
    assertEquals("0", servers.get(0).getId());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(CoturnServer.class)));
  }

  @Test
  public void testGetSelectedCoturnsActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    CoturnServer selectedServer = new CoturnServer();
    otherServer.setId("1");
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(otherServer, selectedServer));

    List<CoturnServer> servers = instance.getSelectedCoturns().join();

    assertEquals(1, servers.size());
    assertEquals("1", servers.get(0).getId());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasDtoClass(CoturnServer.class)));
  }
}
