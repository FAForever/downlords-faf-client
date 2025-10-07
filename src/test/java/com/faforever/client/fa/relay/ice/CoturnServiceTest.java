package com.faforever.client.fa.relay.ice;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.IceServerResponse;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
    when(fafApiAccessor.getApiObject("/ice/server", IceServerResponse.class)).thenReturn(Mono.empty());
    instance.getActiveCoturns();
    verify(fafApiAccessor).getApiObject("/ice/server", IceServerResponse.class);
  }

  @Test
  public void TestGetIceSessionNoActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(
        Mono.just(new IceSession("someSessionId", false, List.of(otherServer))));

    StepVerifier.create(instance.getIceSession(123))
                .assertNext(session -> assertThat(session.servers(), contains(otherServer)))
                .verifyComplete();

    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }

  @Test
  public void testGetIceSessionNonePreferredSelected() {
    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(
        Mono.just(new IceSession("someSessionId", false, List.of(otherServer))));

    StepVerifier.create(instance.getIceSession(123))
                .assertNext(session -> assertThat(session.servers(), contains(otherServer)))
                .verifyComplete();

    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }

  @Test
  public void testGetIceSessionActiveSelected() {
    forgedAlliancePrefs.getPreferredCoturnIds().add("1");

    CoturnServer otherServer = new CoturnServer();
    otherServer.setId("0");
    CoturnServer selectedServer = new CoturnServer();
    selectedServer.setId("1");
    when(fafApiAccessor.getApiObject(any(), any())).thenReturn(
        Mono.just(new IceSession("someSessionId", false, List.of(otherServer, selectedServer))));

    StepVerifier.create(instance.getIceSession(123))
                .assertNext(session -> assertThat(session.servers(), contains(selectedServer)))
                .verifyComplete();

    verify(fafApiAccessor).getApiObject(endsWith("123"), eq(IceSession.class));
  }
}
