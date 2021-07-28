package de.fraunhofer.iosb.svs.sae.security;

import de.fraunhofer.iosb.svs.sae.MainEngineService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ReloadableX509TrustManager implements X509TrustManager {
	private static final Logger log = LoggerFactory.getLogger(ReloadableX509TrustManager.class);

	private X509TrustManager trustManager;

	private String customTrustStorePath;

	private String customTrustStorePassword;

	private static ReloadableX509TrustManager instance = null;

	private ReloadableX509TrustManager(String customTrustStorePath, String customTrustStorePassword) {
		this.customTrustStorePath = customTrustStorePath;
		this.customTrustStorePassword = customTrustStorePassword;
		try {
			reloadTrustManager();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static ReloadableX509TrustManager getInstance() {
		return instance;
	}

	public static void init(String customTrustStorePath, String customTrustStorePassword) {
		try {
			instance = new ReloadableX509TrustManager(customTrustStorePath, customTrustStorePassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		trustManager.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		X509Certificate[] issuers = trustManager.getAcceptedIssuers();
		return issuers;
	}

	private void deleteCertificateFromCustomTrustStore(String alias)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		
		log.info("Deleting cert from: {}", customTrustStorePath);
		
		File customTrustStore = new File(customTrustStorePath);
		
		if (customTrustStore.exists()) {
			KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream in = new FileInputStream(customTrustStorePath);
			ts.load(in, customTrustStorePassword.toCharArray());
			ts.deleteEntry(alias);
			in.close();
		}
	}

	private void saveCertificateToCustomTrustStore(Certificate cert, String alias)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		log.info("Saving cert to: {}", customTrustStorePath);
		
		handleTrustStoreExistance();
		char[] password = customTrustStorePassword.toCharArray();

		KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream in = new FileInputStream(customTrustStorePath);
		ts.load(in, customTrustStorePassword.toCharArray());
		ts.setCertificateEntry(alias, cert);

		FileOutputStream fos = new FileOutputStream(customTrustStorePath);
		ts.store(fos, password);
		fos.close();
	}

	private void handleTrustStoreExistance()
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
		File customTrustStore = new File(customTrustStorePath);

		char[] password = customTrustStorePassword.toCharArray();

		if (!customTrustStore.exists()) {
			customTrustStore.createNewFile();
			ts.load(null, null);
			
			FileOutputStream fos = new FileOutputStream(customTrustStorePath);
			ts.store(fos, password);
			fos.close();
			log.debug("Trust store created");
		}
	}

	private void reloadTrustManager()
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		log.info("Reloading trust manager");
		
		handleTrustStoreExistance();
		
		KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream custom_in = new FileInputStream(customTrustStorePath);
		ts.load(custom_in, customTrustStorePassword.toCharArray());
		custom_in.close();

		// initialize a new TMF with the ts we just loaded
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);

		// acquire X509 trust manager from factory
		TrustManager tms[] = tmf.getTrustManagers();
		for (int i = 0; i < tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				trustManager = (X509TrustManager) tms[i];
				return;
			}
		}
		throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
	}

	public void addServerCertAndReload(String certData, String certAlias) throws Exception {
		byte[] certBytes = Base64.getDecoder().decode(certData);
		CertificateFactory fac = CertificateFactory.getInstance("X509");
		InputStream is = new ByteArrayInputStream(certBytes);
		X509Certificate cert = (X509Certificate) fac.generateCertificate(is);
		saveCertificateToCustomTrustStore(cert, certAlias);
		is.close();
		reloadTrustManager();
	}

	public void deleteServerCertAndReload(String certAlias) throws Exception {
		deleteCertificateFromCustomTrustStore(certAlias);
		reloadTrustManager();
	}
}
