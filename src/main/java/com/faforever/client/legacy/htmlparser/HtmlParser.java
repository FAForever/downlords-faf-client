package com.faforever.client.legacy.htmlparser;

import com.faforever.client.util.JavaFxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.tidy.Tidy;
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
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;

/**
 * Some stuff is not provided by the FAF Server but in a web view. This class tries allowing to parse such "HTML APIs"
 * as clean as possible (which as you can guess isn't that easy).
 */
public class HtmlParser {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Parses the document at the given URL. The document is always put into &lt;root&gt;&lt;/root&gt; because the SAX
   * parser would not be able to parse HTML without a single root element. The specified HtmlContentHandler should
   * expect this.
   */
  public <RESULT_TYPE> RESULT_TYPE parse(String urlString, String params, HtmlContentHandler<RESULT_TYPE> htmlContentHandler) throws IOException {
    JavaFxUtil.assertBackgroundThread();

    URL url = new URL(urlString);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setUseCaches(false);
    try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
      dataOutputStream.writeBytes(params);
    }

    try (InputStream inputStream = connection.getInputStream()) {
      return parseData(inputStream, htmlContentHandler);
    }
  }

  /**
   * Parses the given HTML string. The HTML string is always put into &lt;root&gt;&lt;/root&gt; because the SAX parser
   * would not be able to parse HTML without a single root element. The specified HtmlContentHandler should expect
   * this.
   */
  public <RESULT_TYPE> RESULT_TYPE parse(String string, HtmlContentHandler<RESULT_TYPE> htmlContentHandler) throws IOException {
    logger.trace("Parsing maps from HTML string: {}", string);
    return parseData(new ByteArrayInputStream(string.getBytes(StandardCharsets.US_ASCII)), htmlContentHandler);
  }

  private static <T> T parseData(InputStream inputStream, HtmlContentHandler<T> htmlContentHandler) throws IOException {
    StringWriter out = new StringWriter();

    Tidy tidy = new Tidy();
    tidy.setXmlOut(true);
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    tidy.parse(inputStream, out);

    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setContentHandler(htmlContentHandler);
      xmlReader.parse(new InputSource(new StringReader(out.toString())));

      return htmlContentHandler.getResult();
    } catch (SAXException e) {
      throw new IOException("Error while parsing HTML", e);
    }
  }
}
