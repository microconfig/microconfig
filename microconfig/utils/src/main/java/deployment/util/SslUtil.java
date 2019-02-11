package deployment.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier;
import static javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory;

public class SslUtil {
    public static void disableSslVerification() {
        setDefaultHostnameVerifier((name, session) -> true);

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllManagers(), new java.security.SecureRandom());
            setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustManager[] trustAllManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
    }
}