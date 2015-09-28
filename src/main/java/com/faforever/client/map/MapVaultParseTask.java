package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class MapVaultParseTask extends AbstractPrioritizedTask<List<MapInfoBean>> {

  @Autowired
  I18n i18n;

  @Autowired
  MapVaultParser mapVaultParser;

  private Integer page;
  private Integer maxEntries;

  public MapVaultParseTask() {
    super(Priority.MEDIUM);
  }

  @Override
  protected List<MapInfoBean> call() throws Exception {
    Assert.checkNullIllegalState(page, "page has not been set");
    Assert.checkNullIllegalState(maxEntries, "maxEntries has not been set");

    updateTitle(i18n.get("readMapVaultTask.title"));

    return mapVaultParser.parseMapVault(page, maxEntries);
  }

  public void setPage(int page) {
    this.page = page;
  }

  public void setMaxEntries(int maxEntries) {
    this.maxEntries = maxEntries;
  }
}
