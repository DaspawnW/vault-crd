#!/usr/bin/env bash

### setup kind cluster
kind create cluster --config $PWD/cluster.yaml
### it exposes at 8200 a port for vault


### install vault with a static token
kind get kubeconfig > ~/.kube/kind_config
export KUBECONFIG="$HOME/.kube/kind_config"

kubectl create namespace vault
kubectl apply -f vault.yaml --namespace vault

while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:8200/ui/)" != "200" ]]; do sleep 5; done
echo "Vault is up and running"

export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="root"
### end: install vault with a static token

### deploy vault-crd
kubectl apply -f ../../deploy/rbac.yaml
kubectl apply -f ../../deploy/admission-webhook.yaml
### end: deploy vault-crd

### configure vault
vault secrets enable -version=1 --path=keyvaluev1 kv

echo "Configure vault with default values"
vault write keyvaluev1/docker-hub url=registry.gitlab.com username=username password=VERYSECURE email=john.doe@test.com

vault secrets enable -path=testpki -description=testpki pki
vault secrets tune -max-lease-ttl=8760h testpki
vault write testpki/root/generate/internal \
      common_name=koudingspawn.de \
      ttl=8500h
vault write testpki/roles/testrole \
      allowed_domains=koudingspawn.de \
      allow_subdomains=true \
      max_ttl=200h

vault write -format=json testpki/issue/testrole common_name=vault.koudingspawn.de > data.json
vault write keyvaluev1/vault.koudingspawn.de @data.json
rm data.json

vault secrets enable -version=2 --path=keyvaluev2 kv
vault kv put keyvaluev2/example key=first-version value=first-version
vault kv put keyvaluev2/example key=second-version value=second-version
vault kv put keyvaluev2/example key=third-version value=third-version
vault kv put keyvaluev2/example key=fourth-version value=fourth-version

vault kv put keyvaluev2/database/root username=root password=really
vault write keyvaluev1/database/host host=localhost
### end: configure vault