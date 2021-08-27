package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import org.apache.jena.ontology.Individual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.svs.sae.workflowmanager.ImageInfo;

public class ProcessingModule extends Implementation {

    private static final Logger log = LoggerFactory.getLogger(ProcessingModule.class);

    private String imageName;
    private String imageExternalSource;
    private String imageTag;
    private String moduleType;

    public ProcessingModule(Individual individual) {
        super(individual);
        //fill imageName
        imageExternalSource = getMustExistLiteral(DataProperty.IMAGE_EXTERNAL_SOURCE_URI).getString();
        imageName = getMustExistLiteral(DataProperty.IMAGE_NAME_URI).getString();
        imageTag = getMustExistLiteral(DataProperty.IMAGE_TAG_URI).getString();
        //TODO moduleType
    }

    public ImageInfo getImageInfo() {
        return new ImageInfo(imageExternalSource, imageName, imageTag);
    }
}
