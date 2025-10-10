package com.faforever.client.builders;

import com.faforever.commons.lobby.VetoData;


public class VetoDataBuilder {
  public static VetoDataBuilder create() {
    return new VetoDataBuilder();
  }

  private int mapPoolMapVersionId;
  private int vetoTokensApplied;
  private int matchmakerQueueMapPoolId;

  public VetoDataBuilder defaultValues() {
    mapPoolMapVersionId(1);
    vetoTokensApplied(1);
    matchmakerQueueMapPoolId(1);
    return this;
  }

  public VetoDataBuilder mapPoolMapVersionId(int mapPoolMapVersionId) {
    this.mapPoolMapVersionId = mapPoolMapVersionId;
    return this;
  }

  public VetoDataBuilder vetoTokensApplied(int vetoTokensApplied) {
    this.vetoTokensApplied = vetoTokensApplied;
    return this;
  }

  public VetoDataBuilder matchmakerQueueMapPoolId(int matchmakerQueueMapPoolId) {
    this.matchmakerQueueMapPoolId = matchmakerQueueMapPoolId;
    return this;
  }

  public VetoData get() {
    return new VetoData(mapPoolMapVersionId, vetoTokensApplied, matchmakerQueueMapPoolId);
  }

}
