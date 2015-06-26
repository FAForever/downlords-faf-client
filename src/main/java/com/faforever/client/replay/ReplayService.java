package com.faforever.client.replay;

import java.io.IOException;
import java.util.Collection;

public interface ReplayService {

  Collection<ReplayInfoBean> getLocalReplays() throws IOException;

  Collection<ReplayInfoBean> getOnlineReplays();
}
