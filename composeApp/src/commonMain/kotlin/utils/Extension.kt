package utils

/**
 * 为 String 类添加一个扩展函数，用于提取 "GREETING_MESSAGE:" 后面中括号[]内的内容。
 *
 * 用法:
 * val response = "..."
 * val greeting = response.getGreeting()
 *
 * @return 提取到的中括号内的字符串。如果找不到匹配的格式，则返回null。
 */
fun String.getGreeting(): String {
    // 函数的接收者 (this) 就是调用该函数的字符串实例本身
    val text = this

    val prefix = "GREETING_MESSAGE:"
    val prefixIndex = text.indexOf(prefix)
    if (prefixIndex == -1) {
        return text
    }

    val openBracketIndex = text.indexOf('[', startIndex = prefixIndex + prefix.length)
    if (openBracketIndex == -1) {
        return text
    }

    val closeBracketIndex = text.indexOf(']', startIndex = openBracketIndex + 1)
    if (closeBracketIndex == -1) {
        return text
    }

    return text.substring(openBracketIndex + 1, closeBracketIndex)
}