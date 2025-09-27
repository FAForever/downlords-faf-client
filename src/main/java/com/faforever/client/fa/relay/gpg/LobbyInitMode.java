package com.faforever.client.fa.relay.gpg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum LobbyInitMode {
    NORMAL( 0),
    AUTO(1); // Normal = normal lobby, Auto = skip lobby screen (e.g. ranked)

    private final int id;

    public static LobbyInitMode fromId(int id) {
        return Arrays.stream(LobbyInitMode.values()).filter(mode -> mode.id == id).findFirst().orElse(null);
    }
}
