package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.mapstruct.MapperConfiguration;
import com.faforever.client.player.PlayerService;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.IceServer;
import org.apache.commons.codec.digest.HmacUtils;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(config = MapperConfiguration.class)
public abstract class IceServerMapper {
  int TTL = 86400;

  @Autowired
  private PlayerService playerService;

  public Map<String, Object> map(IceServer iceServer) {
    Map<String, Object> map = new HashMap<>();
    List<String> urls = new ArrayList<>();
    if (iceServer.getUrl() != null && !"null".equals(iceServer.getUrl())) {
      urls.add(iceServer.getUrl());
    }
    if (iceServer.getUrls() != null) {
      urls.addAll(iceServer.getUrls());
    }

    map.put("urls", urls);
    map.put("credential", iceServer.getCredential());
    map.put("credentialType", "token");
    map.put("username", iceServer.getUsername());
    return map;
  }

  public abstract List<Map<String, Object>> mapIceServers(Collection<IceServer> iceServers);

  public Map<String, Object> map(CoturnServer coturnServer) {
    Map<String, Object> map = new HashMap<>();

    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    long timestamp = System.currentTimeMillis() / 1000 + TTL;
    String tokenName = String.format("%d:%d", timestamp, currentPlayer.getId());

    String token = Base64.getEncoder().encodeToString(new HmacUtils("sha1", coturnServer.getKey()).hmac(tokenName));

    String host = coturnServer.getHost();
    if (coturnServer.getPort() != null) {
      host += ":" + coturnServer.getPort();
    }

    List<String> urls = new ArrayList<>();
    urls.add(String.format("turn:%s?transport=tcp", host));
    urls.add(String.format("turn:%s?transport=udp", host));
    urls.add(String.format("turn:%s", host));

    map.put("urls", urls);
    map.put("credential", token);
    map.put("credentialType", "token");
    map.put("username", tokenName);
    return map;
  }

  public abstract List<Map<String, Object>> mapCoturnServers(Collection<CoturnServer> coturnServers);

}
