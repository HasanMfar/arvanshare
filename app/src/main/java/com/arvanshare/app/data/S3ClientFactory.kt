package com.arvanshare.app.data

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions

object S3ClientFactory {
    /**
     * Builds an S3 client pointed at the user's ArvanCloud bucket.
     * Arvan is S3-compatible; path-style addressing matches its endpoints
     * (e.g. https://s3.ir-thr-at1.arvanstorage.ir).
     *
     * The signer region override is REQUIRED: the AWS SDK derives a "clientRegion"
     * from any non-standard endpoint host and then tries to resolve that region's
     * standard S3 service endpoint (RegionUtils.getRegion(...).getServiceEndpoint("s3")).
     * For Arvan endpoints (and IPs like 10.0.2.2) that lookup is empty, so the SDK
     * builds `new URI("https://" + "")` and throws
     * URISyntaxException: Expected authority at index 8. Pinning the override to a
     * real region keeps the custom endpoint while giving the signer a valid region.
     */
    fun create(s: Settings): AmazonS3Client {
        val client = AmazonS3Client(BasicAWSCredentials(s.accessKey, s.secretKey))
        client.setEndpoint(s.endpoint.trim())
        client.setSignerRegionOverride("us-east-1")
        client.setS3ClientOptions(
            S3ClientOptions.builder()
                .setPathStyleAccess(true)
                .disableChunkedEncoding()
                .build()
        )
        return client
    }
}
