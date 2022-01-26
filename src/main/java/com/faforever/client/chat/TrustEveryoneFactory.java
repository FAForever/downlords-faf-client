package com.faforever.client.chat;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class TrustEveryoneFactory extends TrustManagerFactory {

  public static final TrustManager[] FULL_TRUST_MANAGERS = {new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }};

  public static final HostnameVerifier ALL_HOSTS_VALID = (hostname, session) -> true;

  public TrustEveryoneFactory() {
    super(new TrustManagerFactorySpi() {
      @Override
      protected void engineInit(KeyStore ks) {

      }

      @Override
      protected void engineInit(ManagerFactoryParameters spec) {

      }

      @Override
      protected TrustManager[] engineGetTrustManagers() {
        return FULL_TRUST_MANAGERS;
      }
    }, null, null);
  }
}
