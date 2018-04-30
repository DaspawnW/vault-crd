package de.koudingspawn.vault.vault;

public interface TypedSecretGeneratorFactory {

    TypedSecretGenerator get(String name);

}
