package com.faforever.client.clan;

import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLButtonElement;
import sun.plugin.javascript.navig.Document;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.swing.text.html.HTMLDocument;
import java.awt.Button;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Created by Alex on 18.12.2016.
 */



public class ClanController {

  public WebView clanRoot;

  @Value("${clanWebsite.url}")
  private String clanWebsiteUrl;
  @Value("${clanWebsite.clans.url}")
  private String clanWebsiteClansUrl;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Resource
  PreferencesService preferencesService;

  LoginPrefs login;
  @PostConstruct
  public void init()
  {
    login= preferencesService.getPreferences().getLogin();
  }
  public void setUpIfNecessary() {
    if (Strings.isNullOrEmpty(clanRoot.getEngine().getLocation())) {


        clanRoot.getEngine().load(clanWebsiteUrl);
        clanRoot.getEngine().setJavaScriptEnabled(true);

        clanRoot.getEngine().documentProperty().addListener(new ChangeListener<org.w3c.dom.Document>() {
          @Override
          public void changed(ObservableValue<? extends org.w3c.dom.Document> observable, org.w3c.dom.Document oldValue, org.w3c.dom.Document newValue) {
            onSiteLoaded();
          }
        });



    }
  }

  public void onSiteLoaded()
  {
    if(login.getAutoLoginForClan()) {
      try {
        org.w3c.dom.Document site = clanRoot.getEngine().getDocument();
        org.w3c.dom.Element username = site.getElementById("login_form_username_input");
        if(username==null)throw new Exception("usernameField not found. Is this the main Page?");
        username.setAttribute("value", login.getUsername());
        NodeList elemtenList = site.getElementsByTagName("input");
        org.w3c.dom.Element passwordElement = (org.w3c.dom.Element) elemtenList.item(1);


        passwordElement.setAttribute("value",login.getDecodedPassword() );
        HTMLButtonElement button = (HTMLButtonElement) site.getElementsByTagName("button").item(0);
        HashCode.fromString(login.getPassword());
        button.getForm().submit();




      } catch (Exception e) {
        logger.warn(e.toString()+" consider this might be triggered also if another page then the front page is loaded");
      }
    }
  }

  public Node getRoot() {
    return clanRoot;
  }
}


