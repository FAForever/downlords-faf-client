package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

public class MapVaultHtmlContentHandler extends HtmlContentHandler<List<MapInfoBean>> {

  private List<MapInfoBean> result;
  private MapInfoBean currentBean;

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
      currentBean = new MapInfoBean();
      return;
    }

    // FIXME continue;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    super.endElement(uri, localName, qName);
  }
}
