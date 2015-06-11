package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

public class MapVaultHtmlContentHandler extends HtmlContentHandler<List<MapInfoBean>> {

  private enum MapProperty {
    PLAYS,
    DOWNLOADS,
    NAME,
    DESCRIPTION,
    RATING
  }

  private MapProperty currentProperty;
  private List<MapInfoBean> result;
  private MapInfoBean currentBean;
  private String currentValue;

  @Override
  protected List<MapInfoBean> getResult() {
    return result;
  }

  @Override
  public void startDocument() throws SAXException {
    result = new ArrayList<>();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("td") && "map".equals(atts.getValue("class"))) {
      // Start of table cell is start of map
      currentBean = new MapInfoBean();
      return;
    }

    if (localName.equals("div") && "map_name".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.NAME;
      return;
    }

    if (localName.equals("div") && "map_desc".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.DESCRIPTION;
      return;
    }

    if (localName.equals("span") && "plays".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.PLAYS;
      return;
    }

    if (localName.equals("span") && "downloads".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.DOWNLOADS;
      return;
    }

    currentProperty = null;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("td".equals(localName)) {
      if (currentBean != null) {
        // End of cell means end of map
        result.add(currentBean);
        currentBean = null;
      }
      return;
    }

    if (currentProperty == null || currentValue == null || currentValue.trim().isEmpty()) {
      return;
    }

    switch (currentProperty) {
      case PLAYS:
        currentBean.setPlays(Integer.parseInt(currentValue));
        return;
      case DOWNLOADS:
        currentBean.setDownloads(Integer.parseInt(currentValue));
        return;
      case NAME:
        currentBean.setName(currentValue.replaceAll("[\\s\\n]+", "\\s"));
        return;
      case DESCRIPTION:
        currentBean.setDescription(currentValue.replaceAll("[\\s\\n]+", " "));
        return;
      case RATING:
        currentBean.setRating(Float.parseFloat(currentValue));
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
