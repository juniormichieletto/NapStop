package com.example

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.util.Locale

data class SearchResult(
    val name: String,
    val geoPoint: GeoPoint
)

object LocationSearchHelper {
    private val client = OkHttpClient()

    suspend fun searchLocation(context: Context, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        
        // 1. Try system Geocoder first
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5)
                if (!addresses.isNullOrEmpty()) {
                    for (address in addresses) {
                        val nameStr = buildString {
                            val line = address.getAddressLine(0)
                            if (line != null) {
                                append(line)
                            } else {
                                if (address.featureName != null) append(address.featureName)
                                if (address.locality != null) {
                                    if (isNotEmpty()) append(", ")
                                    append(address.locality)
                                }
                                if (address.countryName != null) {
                                    if (isNotEmpty()) append(", ")
                                    append(address.countryName)
                                }
                            }
                        }.ifEmpty { "Location (${address.latitude}, ${address.longitude})" }
                        
                        results.add(SearchResult(nameStr, GeoPoint(address.latitude, address.longitude)))
                    }
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fall back to Nominatim on exceptions
            }
        }
        
        // 2. Fall back to OpenStreetMap Nominatim API
        try {
            val url = "https://nominatim.openstreetmap.org/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&limit=5"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CommuteWakeApp/1.0 (${context.packageName})")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val displayName = obj.optString("display_name", "Unknown Location")
                            val lat = obj.optDouble("lat", 0.0)
                            val lon = obj.optDouble("lon", 0.0)
                            results.add(SearchResult(displayName, GeoPoint(lat, lon)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext results
    }
}
