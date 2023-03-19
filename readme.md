# What is Vault-CRD?

Vault-CRD is a custom resource definition for holding secrets that are stored in HashiCorp Vault up to date with
Kubernetes secrets.

The following Secret engines of Vault are supported:

* KV (Version 1)
* KV (Version 2)
* PKI

The following types of secrets can be managed by Vault-CRD:

* Docker Pull Secret (DockerCfg)
* Ingress Certificates
* JKS Key Stores

For more details please see: [https://vault.koudingspawn.de/how-does-vault-crd-work](https://vault.koudingspawn.de/how-does-vault-crd-work)

## Note

Due to Docker's decision to discontinue its Free Teams, I decided to host my Docker images on GHCR (GitHub Container
Registry) and public ECR (Elastic Container Registry) in the future. 