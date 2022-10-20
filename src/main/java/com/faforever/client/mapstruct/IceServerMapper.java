package com.faforever.client.mapstruct;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.CoturnHostPort;
import com.faforever.commons.api.dto.CoturnServer;
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

  public abstract CoturnHostPort mapToHostPort(CoturnServer coturnServer);

  public Map<String, Object> map(CoturnServer coturnServer) {
    Map<String, Object> map = new HashMap<>();

    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    // Build hmac verification as described here:
    // https://github.com/coturn/coturn/blob/f67326fe3585eafd664720b43c77e142d9bed73c/README.turnserver#L710
    long timestamp = System.currentTimeMillis() / 1000 + TTL;
    String tokenName = String.format("%d:%d", timestamp, currentPlayer.getId());

    String token = Base64.getEncoder().encodeToString(new HmacUtils("HmacSHA1", coturnServer.getKey()).hmac(tokenName));

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

  public abstract List<Map<String, Object>> map(Collection<CoturnServer> coturnServers);

}
