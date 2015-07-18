package com.faforever.client.map;


import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import com.faforever.client.legacy.map.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.DateFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentVaultHtmlContentHandler extends HtmlContentHandler<List<Comment>> {

  private enum MapProperty {
    AUTHOR,
    DATE,
    COMMENT
  }

  private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\w{2} (\\w+), (\\d{4})");

  private List<Comment> result;
  private MapProperty currentProperty;
  private String currentValue;
  private Comment currentComment;

  @Override
  protected List<Comment> getResult() {
    return result;
  }

  @Override
  public void startDocument() throws SAXException {
    result = new ArrayList<>();
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("div") && "feedback".equals(atts.getValue("class"))) {
      currentComment = new Comment();
      result.add(currentComment);
    }
    if (localName.equals("span") && "feedback_author".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.AUTHOR;
      return;
    }

    if (localName.equals("span") && "feedback_date".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.DATE;
      return;
    }

    if (localName.equals("div") && "feedback_body".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.COMMENT;
      return;
    }
    if (localName.equals("b")){
      return;
    }
    currentProperty = null;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {

    if (currentProperty == null || currentValue == null || currentValue.trim().isEmpty()) {
      return;
    }
    logger.debug(currentValue);
    switch (currentProperty) {
      case AUTHOR:
        currentComment.setAuthor(currentValue);
        return;
      case DATE:
        Matcher matcher = DATE_PATTERN.matcher(currentValue);
        if(matcher.find()){
          String day = matcher.group(1);
          String month = matcher.group(2);
          String year = matcher.group(3);
          String date =  String.format("%s %02d %s", month,Integer.parseInt(day), year);
          LocalDate dateTime = LocalDate.parse(date, DateTimeFormatter.ofPattern("MMMM dd yyyy", Locale.US));
          currentComment.setDate(dateTime);
        }

        return;
      case COMMENT:
        currentComment.setText(currentValue);
        return;
      default:
        throw new IllegalStateException("Unhandled property: " + currentProperty);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    currentValue = new String(ch, start, length);
  }
}
