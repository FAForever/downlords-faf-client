package com.faforever.client.legacy.map;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CommentVaultParser {

  List<Map<String,String>> parseCommentVault(int id) throws IOException;
}
