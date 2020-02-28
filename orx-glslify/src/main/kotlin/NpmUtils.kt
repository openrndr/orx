package org.openrndr.extra.glslify

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import khttp.responses.Response


internal data class NPMPackageDist(
        val shasum: String,
        val tarball: String
)

internal data class NPMPackageVersion(
        val name: String,
        val version: String,
        val dist: NPMPackageDist
)

internal data class NPMResponse(
        val name: String?,
        val error: String?,
        @SerializedName("dist-tags")
        val distTags: MutableMap<String, String>?,
        val versions: Map<String, NPMPackageVersion>?
)

internal fun getPackageUrl(module: String): String? {
    val url = "$BASE_URL/$module"

    val response : Response = khttp.get(
            url = url,
            headers = mapOf(
                    "Accept" to "application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8"
            )
    )

    val gson = GsonBuilder().create()

    val npmResponse = gson.fromJson(
            response.text, NPMResponse::class.java
    )

    if (npmResponse.error != null) {
        return null
    }

    return npmResponse.distTags?.let {
        val latest = it["latest"]

        npmResponse.versions?.get(latest)?.dist?.tarball
    }
}