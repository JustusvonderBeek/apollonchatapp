package com.cloudsheeptechnologies.apollonchat.networking.certificate

import io.ktor.network.tls.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class ApollonNetworkConfigCreator {

    companion object {

        val root_ca_alias = "apollon_ca"

        fun createTlsConfig(rootCaResource : InputStream) : TLSConfig {
            val cf = CertificateFactory.getInstance("X509")
            val caInput = BufferedInputStream(rootCaResource)
            val certificateAuthority = caInput.use {
                cf.generateCertificate(caInput)
            }

            // Creating a key store and inserting custom certificate
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry(root_ca_alias, certificateAuthority)

            // Adding the default certificates to the key store
            val defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            defaultTmf.init(null as KeyStore?)
            defaultTmf.trustManagers.filterIsInstance<X509TrustManager>().flatMap {
                it.acceptedIssuers.toList()
            }.forEach {
                keyStore.setCertificateEntry(it.subjectDN.name, it)
            }

            // Final trust manager that uses this key store
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            val tm = tmf.trustManagers.first()
            val tlsConfig = TLSConfigBuilder().apply {
                trustManager = tm
                cipherSuites = CIOCipherSuites.SupportedSuites
            }.build()

            return tlsConfig
        }
    }

}