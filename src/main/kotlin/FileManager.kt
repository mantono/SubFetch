package com.mantono.subfetch

import com.mantono.OpenSubtitlesHasher.computeHash
import java.io.File
import java.lang.RuntimeException

fun mostRecentFile(path: File): File? {
	require(path.isDirectory) { "Path $path was not a directory" }
	val presentSubTitles: List<String> = path.listFiles(subFilter)
		.filter { it.isFile }
		.map { it.nameWithoutExtension }
		.toList()
	return path.listFiles(movieFilter)
		.asSequence()
		.filter { it.canRead() }
		.filter { it.isFile }
		.filter { it.nameWithoutExtension !in presentSubTitles }
		.sortedByDescending { it.lastModified() }
		.firstOrNull()
}

private const val BANNER = "*** Subtitles service powered by www.OpenSubtitles.org ***\n"

fun main(args: Array<String>) {
	println(BANNER)
	val path: File = if(args.isNotEmpty()) {
		File(args[0])
	} else {
		File(System.getProperty("user.dir"))
	}
	val movieFile: File = if(path.isDirectory) {
		mostRecentFile(path) ?: exit(1, "Could not find a valid file in $path")
	} else {
		path
	}

	val language: String = if(args.size > 1) args[1] else "en"
	val hash: String = computeHash(movieFile)
	println("Found file $movieFile")
	println(hash)
	println(ApiClient().fetch(hash, language))
}

fun exit(exitCode: Int, error: String): Nothing {
	System.err.println(error)
	System.exit(exitCode)
	throw RuntimeException(error)
}