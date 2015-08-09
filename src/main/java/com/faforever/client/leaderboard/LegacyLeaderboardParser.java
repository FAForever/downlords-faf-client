package com.faforever.client.leaderboard;

import com.faforever.client.util.JavaFxUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;

public class LegacyLeaderboardParser implements LeaderboardParser {

  @Autowired
  Environment environment;

  @Override
  public List<LeaderboardEntryBean> parseLadder() throws IOException {
    JavaFxUtil.assertBackgroundThread();

    String urlParameters = environment.getProperty("ladder.params");
    URL url = new URL(environment.getProperty("ladder.url"));

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setUseCaches(false);
    try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
      dataOutputStream.writeBytes(urlParameters);
    }

    try (InputStream inputStream = connection.getInputStream()) {
      // Wrap the result inside a <root> element since every well-formed XML needs a such. Otherwise it can't be parsed.
      SequenceInputStream sequenceInputStream = new SequenceInputStream(enumeration(asList(
          new ByteArrayInputStream("<root>".getBytes(StandardCharsets.US_ASCII)),
          new BufferedInputStream(inputStream),
          new ByteArrayInputStream("</root>".getBytes(StandardCharsets.US_ASCII))
      )));

      return parseData(new BufferedInputStream(sequenceInputStream));
    } catch (SAXException e) {
      throw new IOException("Error while parsing leaderboard", e);
    }
  }

  private static List<LeaderboardEntryBean> parseData(BufferedInputStream inputStream) throws IOException, SAXException {
    LeaderboardContentHandler leaderboardContentHandler = new LeaderboardContentHandler();

    XMLReader xmlReader = XMLReaderFactory.createXMLReader();
    xmlReader.setContentHandler(leaderboardContentHandler);
    xmlReader.parse(new InputSource(inputStream));

    return leaderboardContentHandler.getResult();
  }
}
