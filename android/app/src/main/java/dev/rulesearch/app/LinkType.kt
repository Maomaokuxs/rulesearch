package dev.rulesearch.app

/** 链接类型：网盘 / 磁力 / 未知，用于跳转规则区分和弹窗展示。 */
data class LinkType(val id: String, val label: String)

object LinkTypes {
    val MAGNET = LinkType("magnet", "磁力链接")
    val UNKNOWN = LinkType("unknown", "未知链接")

    /** 域名关键字 -> 网盘名 */
    private val DOMAIN_MAP = listOf(
        "quark" to "夸克网盘",
        "alipan" to "阿里云盘",
        "aliyundrive" to "阿里云盘",
        "baidu" to "百度网盘",
        "189.cn" to "天翼云盘",
        "xunlei" to "迅雷云盘",
        "uc.cn" to "UC网盘",
        "123pan" to "123云盘",
        "123865" to "123云盘",
        "jianguoyun" to "坚果云",
        "lanzou" to "蓝奏云",
        "lanzn" to "蓝奏云",
        "weiyun" to "微云",
        "ctfile" to "城通网盘",
        "cowtransfer" to "奶牛快传",
        "vdisk" to "微盘",
        "pan.ecnu" to "微盘",
    )

    /** 标题里的 [xxx] 标签 -> 网盘名 */
    private val TAG_MAP = mapOf(
        "quark" to "夸克网盘",
        "aliyun" to "阿里云盘",
        "baidu" to "百度网盘",
        "189" to "天翼云盘",
        "xunlei" to "迅雷云盘",
        "uc" to "UC网盘",
    )

    fun detect(url: String, title: String): LinkType {
        if (url.startsWith("magnet:", ignoreCase = true)) return MAGNET
        val host = try {
            java.net.URI(url).host.orEmpty().lowercase()
        } catch (e: Exception) {
            ""
        }
        DOMAIN_MAP.firstOrNull { (key, _) -> key in host }
            ?.let { return LinkType(it.first, it.second) }
        // 域名没命中时，看标题里的 [quark] 这类标签
        val tag = Regex("""\[(\w+)]""").find(title)
            ?.groupValues?.getOrNull(1)?.lowercase()
        val label = tag?.let { TAG_MAP[it] }
        if (label != null && tag != null) return LinkType(tag, label)
        return UNKNOWN
    }
}
