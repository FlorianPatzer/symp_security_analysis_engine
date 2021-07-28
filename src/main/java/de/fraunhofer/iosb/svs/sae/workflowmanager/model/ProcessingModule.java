package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import org.apache.jena.ontology.Individual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingModule extends Implementation {

    private static final Logger log = LoggerFactory.getLogger(ProcessingModule.class);

    private String imageName;
    private String moduleType;

    public ProcessingModule(Individual individual) {
        super(individual);
        //fill imageName
        imageName = getMustExistLiteral(DataProperty.IMAGE_NAME_URI).getString();
    }

    public String getImageName() {
        return imageName;
    }
}
