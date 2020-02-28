package org.openrndr.extra.glslify

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.net.URL
import javax.net.ssl.HttpsURLConnection


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
    val moduleUrl = "$BASE_URL/$module"

    val url = URL(moduleUrl)

    val con = url.openConnection() as HttpsURLConnection
    con.setRequestProperty("Accept", "application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8")

    val json = String(con.inputStream.readBytes())
    val gson = GsonBuilder().create()

    val npmResponse = gson.fromJson(json, NPMResponse::class.java)

    if (npmResponse.error != null) {
        return null
    }

    return npmResponse.distTags?.let {
        val latest = it["latest"]

        npmResponse.versions?.get(latest)?.dist?.tarball
    }
}