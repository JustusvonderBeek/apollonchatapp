package com.example.apollonchat.networking.certificate

import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object TrustAllCertsManager : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        // Accept all
    }

    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        // Accept all
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(null, null)
        val cert = getMyCertificate() // replace with your own method for getting the certificate
        trustStore.setCertificateEntry("my_cert_alias", cert)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val trustManagers = tmf.trustManagers
        for (tm in trustManagers) {
            if (tm is X509TrustManager) {
                tm.checkServerTrusted(chain, authType)
                return
            }
        }
        throw CertificateException("Failed to verify server certificate")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}