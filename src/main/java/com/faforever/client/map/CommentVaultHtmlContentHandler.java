package com.faforever.client.map;


import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//FIXME nested list is ugly, let's do a commentInfoBean, but where do we store the list while keeping flat beans??
public class CommentVaultHtmlContentHandler extends HtmlContentHandler<List<Map<String,String>>> {

  private enum MapProperty {
    AUTHOR,
    DATE,
    COMMENT
  }

  private List<Map<String,String>> result;
  private MapProperty currentProperty;
  private String currentValue;
  private Map<String,String> currentComment;

  @Override
  protected List<Map<String,String>> getResult() {
    return result;
  }

  @Override
  public void startDocument() throws SAXException {
    result = new ArrayList<>();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("div") && "feedback".equals(atts.getValue("class"))) {
      currentComment = new HashMap<>();
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

    currentProperty = null;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("td".equals(localName)) {
      if (!currentComment.isEmpty()) {
        // End of cell means end of map
        result.add(currentComment);
        currentComment = null;
      }
      return;
    }

    if (currentProperty == null || currentValue == null || currentValue.trim().isEmpty()) {
      return;
    }

    switch (currentProperty) {
      case AUTHOR:
        //FIXME FUCK REGEX, FIX THIS SHIT PLEASE
        currentComment.put("Author",currentValue.replaceAll("\\bby\\b",""));
        return;
      case DATE:
        currentComment.put("Date",currentValue);
        return;
      case COMMENT:
        currentComment.put("Comment",currentValue);
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
