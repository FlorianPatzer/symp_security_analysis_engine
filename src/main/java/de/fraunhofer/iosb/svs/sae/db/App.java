package de.fraunhofer.iosb.svs.sae.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

@Entity
public class App {
	@Id
	@Column(name = "app_key", unique = true)
	private String key;
	private Boolean active;

	private Boolean pending;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String certificate;
	@Column(name = "report_callback_uri")
	private String reportCallbackURI;
	private String token;
	
	@ManyToMany(mappedBy = "subscribers", fetch = FetchType.EAGER)
    private Set<Analysis> analyses;

	public App() {
		this.setKey(null);
		this.setActive(false);
		this.setPending(null);
		this.setCertificate(null);
		this.setReportCallbackURI(null);
		this.setToken(null);
	}

	public App(String key, Boolean active, Boolean pending, String reportCallbackURI, String certificate, String token) {
		this.setKey(key);
		this.setActive(active);
		this.setPending(pending);
		this.setCertificate(certificate);
		this.setReportCallbackURI(reportCallbackURI);
		this.setToken(token);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Boolean isActive() {
		return active;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getReportCallbackURI() {
		return reportCallbackURI;
	}

	public void setReportCallbackURI(String reportCallbackURI) {
		this.reportCallbackURI = reportCallbackURI;
	}

	public Boolean isPending() {
		return pending;
	}

	public void setPending(Boolean pending) {
		this.pending = pending;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
}
