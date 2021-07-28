package de.fraunhofer.iosb.svs.sae.security;

import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Scope("singleton")
public class RestTemplateWithReloadableTruststore{
	
	public X509TrustManager getDefaultTrustManager() throws Exception {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
		trustManagerFactory.init((KeyStore) null);
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		return trustManager;
	}

	private static SSLContext getSSLContext() throws Exception {
		TrustManager[] trustManagers = new TrustManager[] { ReloadableX509TrustManager.getInstance() };

		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}

	public static RestTemplate create() throws Exception {
		// Create SSL Context
		SSLContext sslContext = getSSLContext();
		HttpClient httpClient = HttpClientBuilder.create().setSSLContext(sslContext).build();
		ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return new RestTemplate(requestFactory);
	}

}
