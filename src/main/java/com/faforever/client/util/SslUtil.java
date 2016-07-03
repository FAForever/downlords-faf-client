package com.faforever.client.util;

import com.google.api.client.util.SecurityUtils;
import com.google.api.client.util.SslUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;

import static com.github.nocatch.NoCatch.noCatch;
import static org.bridj.Platform.getResourceAsStream;

public final class SslUtil {

  private SslUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static void loadTruststores() {
    noCatch(() -> {
      KeyStore trustStore = SecurityUtils.getJavaKeyStore();
      trustStore.load(null, null);

      try (InputStream inputStream = getResourceAsStream("/cacerts/lets-encrypt-x3-cross-signed.pem")) {
        SecurityUtils.loadKeyStoreFromCertificates(
            trustStore,
            SecurityUtils.getX509CertificateFactory(),
            inputStream);

        SSLContext sslContext = SslUtils.getTlsSslContext();
        SslUtils.initSslContext(sslContext, trustStore, SslUtils.getPkixTrustManagerFactory());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      }
    });
  }
}
