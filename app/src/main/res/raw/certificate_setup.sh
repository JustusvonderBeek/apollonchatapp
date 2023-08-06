#!/bin/bash

# TODO: Creating new key and self-signed root certificate

# Converting the existing key into a keystore to use with android
# File: certificate
# Keystore: Output
# Alias: Name of the certificate
keytool -importcert -file apollon.crt -keystore keystore.p12 -storetype PKCS12 -alias "anzuchat" -storepass 123456