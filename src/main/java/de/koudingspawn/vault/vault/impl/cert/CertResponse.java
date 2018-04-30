package de.koudingspawn.vault.vault.impl.cert;

import de.koudingspawn.vault.vault.impl.pki.PKIResponse;

public class CertResponse {

    private PKIResponse data;

    public PKIResponse getData() {
        return data;
    }

    public void setData(PKIResponse data) {
        this.data = data;
    }
}
