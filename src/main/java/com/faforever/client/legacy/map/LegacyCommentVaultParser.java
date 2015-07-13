package com.faforever.client.legacy.map;


import com.faforever.client.legacy.htmlparser.HtmlParser;
import com.faforever.client.map.CommentVaultHtmlContentHandler;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class LegacyCommentVaultParser implements CommentVaultParser {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  HtmlParser htmlParser;

  @Override
  public List<Map<String,String>> parseCommentVault(int id) throws IOException {
    CommentVaultHtmlContentHandler commentVaultHtmlContentHandler = new CommentVaultHtmlContentHandler();

    URL url = new URL(String.format(environment.getProperty("vault.mapCommentUrl"), id));

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    logger.debug("Fetching comments from {}", url);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
      JsonReader jsonReader = new JsonReader(reader);
      jsonReader.beginObject();

      //FIXME pretty sure this is useless code for the comment parser
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        if (!"comments".equals(key)) {
          jsonReader.skipValue();
          continue;
        }

        String comments = jsonReader.nextString();

        return htmlParser.parse(comments,commentVaultHtmlContentHandler);
      }

      jsonReader.endObject();
    }

    throw new IllegalStateException("Map vault could not be read from " + url);
  }
}
