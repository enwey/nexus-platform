package com.nexus.platform.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

object BackendHttpClientFactory {
    fun create(
        connectTimeoutSeconds: Long = 15,
        readTimeoutSeconds: Long = 15,
        writeTimeoutSeconds: Long = 15,
        configure: OkHttpClient.Builder.() -> Unit = {}
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .apply {
                if (BackendConfig.shouldPinCertificate) {
                    addInterceptor(BackendCertificatePinningInterceptor)
                }
                configure()
            }
            .build()
    }
}

private object BackendCertificatePinningInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val requestUrl = chain.request().url
        if (!BackendConfig.shouldPinCertificate || requestUrl.isHttps.not() || requestUrl.host != BackendConfig.backendHost) {
            return response
        }
        val certificate = response.handshake?.peerCertificates?.firstOrNull() as? X509Certificate
            ?: throw IOException("Missing peer certificate for pinned backend host")
        val actual = sha256Hex(certificate.encoded)
        if (!actual.equals(BackendConfig.backendCertSha256, ignoreCase = true)) {
            response.close()
            throw IOException("Backend certificate pin mismatch")
        }
        return response
    }

    private fun sha256Hex(input: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
