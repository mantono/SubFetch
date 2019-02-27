package com.mantono.subfetch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import org.apache.xmlrpc.client.AsyncCallback
import java.net.URL
import java.time.Duration
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl



class ApiClient() {
	private var token: String? = null
	private val client = OkHttpClient.Builder()
		.callTimeout(Duration.ofSeconds(20))
		.readTimeout(Duration.ofSeconds(20))
		.build()

	private val xmlClient: XmlRpcClient = run {
		val config = XmlRpcClientConfigImpl()
		config.serverURL = URL(URL)
		config.userAgent = USER_AGENT
		XmlRpcClient().apply { setConfig(config) }
	}

	fun fetch(hash: String, language: String): URL? {
		if(token == null) {
			token = login()
		}
		data class Result(
			val seconds: Double,
			val data: List<String>,
			val status: String
		)
		val result = xmlClient.execute(
			"SearchSubtitles",
			arrayOf<Any?>(
				token!!,
				arrayOf(
					mapOf(
						"sublanguageid" to language,
						"movieHash" to hash
					)
				)
			)
		)

		val found: List<String> = result.responseToMap()["data"] as List<String>
		return found.firstOrNull()?.let { URL(it) }
	}

	fun login(user: String = "", password: String = ""): String {
		val result: Map<String, Any?> = xmlClient
			.execute("LogIn", arrayOf<String>(user, password, LANGUAGE, USER_AGENT))
			.responseToMap()
		println(result)
		return result["token"]!!.toString()
	}

	companion object {
		const val URL = "https://api.opensubtitles.org/xml-rpc"
		const val USER_AGENT = "Jsubfetch/v0.1"
		const val LANGUAGE = "en"
	}
}

fun Any.responseToMap(): Map<String, Any?> {
	val node: JsonNode = jacksonObjectMapper().valueToTree<JsonNode>(this)
	return jacksonObjectMapper().convertValue(node)
}