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

    /*public String getTargetSystemOntologyPath(String targetSystemId) {
        return getTargetSystemOntologyPath(Long.valueOf(targetSystemId));
    }

    public String getTargetSystemOntologyPath(Long targetSystemId) {
        TargetSystem targetSystem = targetSystemRepository.findById(targetSystemId).orElseThrow(() -> {
            log.warn("No targetSystem with id " + targetSystemId + " found.");
            return new ResourceNotFoundException("targetSystem", targetSystemId);
        });
        // already set to ready --> TODO what if it changes
        if (targetSystem.isReady()) {
            return targetSystem.getOntologyPath();
        } else {
            //make call or what
            throw new TargetSystemNotReadyException(targetSystemId);
        }

        Optional<TargetSystem> targetSystemOptional = targetSystemRepository.findById(targetSystemId);

        if (targetSystemOptional.isPresent()){
            // do we have ontologypath? we should, or else error for now
        } else {
            // we do not have the target system, so we pull it from the spc
        }
    }
    */
}
