package com.faforever.client.fa.relay.gpg;

import java.util.List;

public sealed interface GPGMessage {

  String command();

  List<Object> arguments();

  record GameState(com.faforever.client.fa.relay.gpg.GameState state) implements GPGMessage {
    @Override
    public String command() {
      return "GameState";
    }

    @Override
    public List<Object> arguments() {
      return List.of(state().stateName());
    }
  }

  record GameEnded() implements GPGMessage {
    @Override
    public String command() {
      return "GameEnded";
    }

    @Override
    public List<Object> arguments() {
      return List.of();
    }
  }

  record GameFull() implements GPGMessage {
    @Override
    public String command() {
      return "GameFull";
    }

    @Override
    public List<Object> arguments() {
      return List.of();
    }
  }

  record CreateLobby(LobbyInitMode mode, int port, String playerName, int playerId) implements GPGMessage {
    @Override
    public String command() {
      return "CreateLobby";
    }

    @Override
    public List<Object> arguments() {
      return List.of(mode().getId(), port, playerName, playerId);
    }
  }

  record JoinGame(String remotePlayerLogin, int remotePlayerId) implements GPGMessage {
    @Override
    public String command() {
      return "JoinGame";
    }

    @Override
    public List<Object> arguments() {
      return List.of("127.0.0.1:0", remotePlayerLogin, remotePlayerId);
    }
  }

  record HostGame(String mapName) implements GPGMessage {
    @Override
    public String command() {
      return "HostGame";
    }

    @Override
    public List<Object> arguments() {
      return List.of(mapName);
    }
  }

  record ConnectToPeer(String remotePlayerLogin, int remotePlayerId) implements GPGMessage {
    @Override
    public String command() {
      return "ConnectToPeer";
    }

    @Override
    public List<Object> arguments() {
      return List.of("127.0.0.1:0", remotePlayerLogin, remotePlayerId);
    }
  }

  record DisconnectFromPeer(int remotePlayerId) implements GPGMessage {
    @Override
    public String command() {
      return "DisconnectFromPeer";
    }

    @Override
    public List<Object> arguments() {
      return List.of(remotePlayerId);
    }
  }

  record Generic(String command, List<Object> arguments) implements GPGMessage {
    public Generic {
      arguments = List.copyOf(arguments);
    }
  }

}
