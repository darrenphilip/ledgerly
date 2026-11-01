package com.ledgerly.app.util

/**
 * Minimal JSON reader/writer for the backup file. Deliberately small: the backup schema is
 * ours, and a hand-rolled codec keeps the app dependency-free.
 */
object Json {

    fun write(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

    private fun writeValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is String -> writeString(sb, value)
            is Boolean -> sb.append(if (value) "true" else "false")
            is Int, is Long -> sb.append(value.toString())
            is Double -> sb.append(if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                value.forEach { (key, item) ->
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, key.toString())
                    sb.append(':')
                    writeValue(sb, item)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                value.forEach { item ->
                    if (!first) sb.append(',')
                    first = false
                    writeValue(sb, item)
                }
                sb.append(']')
            }
            else -> throw IllegalArgumentException("Unsupported JSON value: ${value::class}")
        }
    }

    private fun writeString(sb: StringBuilder, value: String) {
        sb.append('"')
        value.forEach { char ->
            when (char) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (char < ' ') sb.append("\\u%04x".format(char.code)) else sb.append(char)
            }
        }
        sb.append('"')
    }

    fun parse(text: String): Any? {
        val reader = Reader(text)
        val value = reader.readValue()
        reader.skipWhitespace()
        require(reader.done) { "Trailing characters in JSON" }
        return value
    }

    private class Reader(private val text: String) {
        private var index = 0

        val done: Boolean get() = index >= text.length

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(): Any? {
            skipWhitespace()
            require(!done) { "Unexpected end of JSON" }
            return when (val char = text[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> if (char == '-' || char.isDigit()) readNumber() else error("Unexpected character '$char'")
            }
        }

        private fun readObject(): Map<String, Any?> {
            index++ // {
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (!done && text[index] == '}') {
                index++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                require(!done && text[index] == ':') { "Expected ':'" }
                index++
                map[key] = readValue()
                skipWhitespace()
                require(!done) { "Unterminated object" }
                when (text[index]) {
                    ',' -> index++
                    '}' -> { index++; return map }
                    else -> error("Expected ',' or '}'")
                }
            }
        }

        private fun readArray(): List<Any?> {
            index++ // [
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (!done && text[index] == ']') {
                index++
                return list
            }
            while (true) {
                list += readValue()
                skipWhitespace()
                require(!done) { "Unterminated array" }
                when (text[index]) {
                    ',' -> index++
                    ']' -> { index++; return list }
                    else -> error("Expected ',' or ']'")
                }
            }
        }

        private fun readString(): String {
            skipWhitespace()
            require(!done && text[index] == '"') { "Expected string" }
            index++
            val sb = StringBuilder()
            while (true) {
                require(!done) { "Unterminated string" }
                when (val char = text[index]) {
                    '"' -> { index++; return sb.toString() }
                    '\\' -> {
                        index++
                        require(!done) { "Unterminated escape" }
                        when (val escaped = text[index]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(index + 4 < text.length) { "Bad unicode escape" }
                                sb.append(text.substring(index + 1, index + 5).toInt(16).toChar())
                                index += 4
                            }
                            else -> error("Bad escape '\\$escaped'")
                        }
                        index++
                    }
                    else -> { sb.append(char); index++ }
                }
            }
        }

        private fun readNumber(): Number {
            val start = index
            if (!done && text[index] == '-') index++
            while (!done && (text[index].isDigit() || text[index] == '.' ||
                    text[index] == 'e' || text[index] == 'E' || text[index] == '+' || text[index] == '-')
            ) index++
            val raw = text.substring(start, index)
            return raw.toLongOrNull() ?: raw.toDouble()
        }

        private fun <T> readLiteral(literal: String, value: T): T {
            require(text.startsWith(literal, index)) { "Expected '$literal'" }
            index += literal.length
            return value
        }
    }
}