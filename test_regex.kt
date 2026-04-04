
fun main() {
    val text = "已知函数 $y = \sqrt{x}$，插值节点为 $x_0 = 0$。"
    val replaced = Regex("""(?<!\$)\$(?!\$)(.+?)(?<!\$)\$(?!\$)""").replace(text) { match ->
        "$$${match.groupValues[1].trim()}$$"
    }
    println(replaced)
}

