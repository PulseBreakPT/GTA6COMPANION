#!/usr/bin/env bash
set -euo pipefail
# Run from repo root. Keep the private key OUTSIDE the repository.
test "$#" = 1 || { echo 'Usage: scripts/setup_editorial_key.sh /absolute/private/key.pem'; exit 1; }
key_path="$1"
test ! -e "$key_path" || { echo 'Refusing to overwrite an existing key'; exit 1; }
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$key_path"
openssl pkey -in "$key_path" -pubout -outform DER | base64 | tr -d '\n' > app/src/main/assets/editorial_public_key.txt
printf '\nPublic trust key installed. Securely back up the private key outside Git. Rebuild the app once before enabling the signed feed.\n'
