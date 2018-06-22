package de.koudingspawn.vault.vault.impl.pki;

import org.springframework.util.CollectionUtils;

import java.util.List;

public class VaultResponseData {
    private String certificate;
    private String issuing_ca;
    private List<String> ca_chain;
    private String private_key;
    private String private_key_type;
    private String serial_number;

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getIssuing_ca() {
        return issuing_ca;
    }

    public void setIssuing_ca(String issuing_ca) {
        this.issuing_ca = issuing_ca;
    }

    public List<String> getCa_chain() {
        return ca_chain;
    }

    public void setCa_chain(List<String> ca_chain) {
        this.ca_chain = ca_chain;
    }

    public String getPrivate_key() {
        return private_key;
    }

    public void setPrivate_key(String private_key) {
        this.private_key = private_key;
    }

    public String getPrivate_key_type() {
        return private_key_type;
    }

    public void setPrivate_key_type(String private_key_type) {
        this.private_key_type = private_key_type;
    }

    public String getSerial_number() {
        return serial_number;
    }

    public void setSerial_number(String serial_number) {
        this.serial_number = serial_number;
    }

    public String getChainedCertificate() {
        StringBuilder sb = new StringBuilder(certificate);

        if (!CollectionUtils.isEmpty(ca_chain)) {
            ca_chain.forEach(cert -> sb.append("\n").append(cert));
        }

        return sb.toString();
    }
}
