package de.fraunhofer.iosb.svs.sae.workflowmanager;

public class ImageInfo {
    private String imageName;
    private String imageExternalSource;
    private String imageTag;

    public ImageInfo(String imageExternalSource, String imageName, String imageTag) {
        this.imageExternalSource = imageExternalSource;
        this.imageName = imageName;
        this.imageTag = imageTag;
    }
    
    public String getExternalSource() {
        return imageExternalSource;
    }
    
    public String getName() {
        return imageName;
    }
    
    public String getTag() {
        return imageTag;
    }
}
