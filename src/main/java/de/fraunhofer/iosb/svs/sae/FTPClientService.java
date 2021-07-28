package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.exceptions.FTPConnectionException;
import de.fraunhofer.iosb.svs.sae.exceptions.UnsatisfiableException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public class FTPClientService implements MountpointHandler {
    private static final Logger log = LoggerFactory.getLogger(FTPClientService.class);

    private final String ontologiesMountpoint;
    private final String host;
    private final int port;
    private final String path;

    public FTPClientService(@Value("${mountpoint.ontologies}") String ontologiesMountpoint) {
        // use ontologies mountpoint local value if exists, as the ftp client needs this
        // one to connect
        this.ontologiesMountpoint = ontologiesMountpoint;

        try {
            URI uri = new URI(getOntologiesMountpoint());
            if (!uri.getScheme().equals("ftp")) {
                throw new UnsatisfiableException("FTP Client needs an uri with ftp scheme");
            }
            host = uri.getHost();
            log.debug("Set FTP host '{}'", host);
            if (uri.getPort() == -1) {
                log.debug("Port not specified. Using default port 21");
                port = 21;
            } else {
                port = uri.getPort();
            }
            log.debug("Set FTP port '{}'", port);
            path = uri.getPath();
            log.debug("Set FTP path '{}'", uri.getPath());
        } catch (URISyntaxException e) {
            log.warn("Mountpoint ontologies '{}' not a valid uri. Reason: '{}'", e.getInput(), e.getReason(), e);
            throw new UnsatisfiableException("FTP Client has no valid uri", e);
        }
    }

    @Override
    public String getOntologiesMountpoint() {
        return this.ontologiesMountpoint;
    }

    @Override
    public String getUriFor(String filename) {
        return ontologiesMountpoint + "/" + filename;
    }

    @Override
    public String getFilenameForOwl(String filename) {
        return filename + OWL_SUFFIX;
    }

    public String uploadFile(String fileName, InputStream inputStream) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host, port);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReply())) {
                throw new FTPConnectionException("FTP server refused connection");
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.login("anonymous", "");
            ftpClient.changeWorkingDirectory(path);
            ftpClient.storeFile(fileName, inputStream);
            ftpClient.logout();
            return "";
        } catch (IOException ex) {
            log.error("Problem with FTP server", ex);
            throw new FTPConnectionException("Caught IO exception", ex);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while closing", ex);
            }
        }
    }

    public void uploadFiles(Map<String, DataBuffer> uploads) {
        FTPClient ftpClient = new FTPClient();
        try {
            log.debug("Connect to '{}:{}'", host, port);
            ftpClient.connect(host, port);
            log.debug("Enter local passive mode");
            ftpClient.enterLocalPassiveMode();
            log.debug("Login anonymous");
            ftpClient.login("anonymous", "");
            ftpClient.changeWorkingDirectory(path);
            for (Map.Entry<String, DataBuffer> upload : uploads.entrySet()) {
                ftpClient.storeFile(upload.getKey(), upload.getValue().asInputStream());
                log.debug("Stored file '{}' to '{}'", upload.getKey(), getUriFor(upload.getKey()));
            }
            ftpClient.logout();
        } catch (IOException ex) {
            log.error("Problem with FTP server", ex);
            throw new FTPConnectionException("Caught IO exception", ex);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while closing", ex);
            }
        }
    }

}
