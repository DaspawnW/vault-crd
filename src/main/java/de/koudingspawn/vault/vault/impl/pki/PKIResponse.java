package de.koudingspawn.vault.vault.impl.pki;

public class PKIResponse {
    private String lease_id;
    private boolean renewable;
    private VaultResponseData data;

    public String getLease_id() {
        return lease_id;
    }

    public void setLease_id(String lease_id) {
        this.lease_id = lease_id;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public void setRenewable(boolean renewable) {
        this.renewable = renewable;
    }

    public VaultResponseData getData() {
        return data;
    }

    public void setData(VaultResponseData data) {
        this.data = data;
    }
}
