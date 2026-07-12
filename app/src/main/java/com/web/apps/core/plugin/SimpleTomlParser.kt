package com.web.apps.core.plugin

object SimpleTomlParser {

    fun parseSectioned(text: String): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        var currentSection = ""
        result[currentSection] = mutableMapOf()

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEach

            if (line.startsWith("[") && line.endsWith("]") && !line.contains("=")) {
                currentSection = line.removePrefix("[").removeSuffix("]").trim()
                result.getOrPut(currentSection) { mutableMapOf() }
                return@forEach
            }

            val separatorIndex = line.indexOf('=')
            if (separatorIndex == -1) return@forEach

            val key = line.substring(0, separatorIndex).trim()
            var value = line.substring(separatorIndex + 1).trim()

            if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                value = value.substring(1, value.length - 1)
            }

            result.getOrPut(currentSection) { mutableMapOf() }[key] = value
        }

        return result
    }

    fun parseFlat(text: String): Map<String, String> {
        return parseSectioned(text)[""] ?: emptyMap()
    }

    fun parseInlineTableArray(text: String, arrayKey: String): List<Map<String, String>> {
        val startMarker = "$arrayKey = ["
        val startIndex = text.indexOf(startMarker)
        if (startIndex == -1) return emptyList()

        val contentStart = startIndex + startMarker.length
        var depth = 1
        var i = contentStart
        while (i < text.length && depth > 0) {
            when (text[i]) {
                '[' -> depth++
                ']' -> depth--
            }
            if (depth > 0) i++
        }
        val arrayBody = text.substring(contentStart, i)

        return extractInlineTables(arrayBody).map { parseInlineTableContent(it) }
    }

    private fun extractInlineTables(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in text.indices) {
            when (text[i]) {
                '{' -> {
                    if (depth == 0) start = i + 1
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        results.add(text.substring(start, i))
                        start = -1
                    }
                }
            }
        }
        return results
    }

    private fun parseInlineTableContent(content: String): Map<String, String> {
        val pairs = splitOutsideQuotes(content, ',')
        val map = mutableMapOf<String, String>()
        for (pair in pairs) {
            val trimmed = pair.trim()
            if (trimmed.isBlank()) continue
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex == -1) continue

            val key = trimmed.substring(0, eqIndex).trim()
            var value = trimmed.substring(eqIndex + 1).trim()
            if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                value = value.substring(1, value.length - 1)
            }
            map[key] = value
        }
        return map
    }

    private fun splitOutsideQuotes(text: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in text) {
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                c == delimiter && !inQuotes -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) parts.add(current.toString())
        return parts
    }
}