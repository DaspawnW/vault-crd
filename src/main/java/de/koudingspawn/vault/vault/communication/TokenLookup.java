package de.koudingspawn.vault.vault.communication;

public class TokenLookup {

    private String request_id;
    private boolean renewable;
    private TokenLookupData data;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public void setRenewable(boolean renewable) {
        this.renewable = renewable;
    }

    public TokenLookupData getData() {
        return data;
    }

    public void setData(TokenLookupData data) {
        this.data = data;
    }
}
