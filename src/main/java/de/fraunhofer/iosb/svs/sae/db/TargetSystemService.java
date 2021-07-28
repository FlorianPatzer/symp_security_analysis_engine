package de.fraunhofer.iosb.svs.sae.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.fraunhofer.iosb.svs.sae.restclient.sme.SmeClientService;

@Service
public class TargetSystemService {

    private static final Logger log = LoggerFactory.getLogger(TargetSystemService.class);

    private final SmeClientService smeClientService;

    @Autowired
    public TargetSystemService(SmeClientService smeClientService) {
        this.smeClientService = smeClientService;
    }
}
