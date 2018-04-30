package de.koudingspawn.vault.vault.impl.dockercfg;


public class DockerCfgResponse {
    private PullSecret data;

    public PullSecret getData() {
        return data;
    }

    public void setData(PullSecret data) {
        this.data = data;
    }
}
