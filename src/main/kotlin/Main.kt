import java.net.URL
/**
 * Хранилище параметров конфигурации CLI-приложения.
 */
data class AppConfig(
    val packageName: String,
    val repositoryUrlOrPath: String,
    val isTestMode: Boolean,
    val packageVersion: String,
    val maxDepth: Int,
    val filterSubstring: String
)

/**
 * Точка входа в приложение. Разбирает аргументы и запускает сбор данных.
 */
fun main(args: Array<String>) {
    try {
        val config = parseArgs(args)

        if (!config.isTestMode) {
            val dependencies = fetchPipDependencies(
                config.repositoryUrlOrPath,
                config.packageName,
                config.packageVersion
            )
            dependencies.forEach { println(it) }
        }
    } catch (e: IllegalArgumentException) {
        System.err.println("Ошибка конфигурации: ${e.message}")
        System.exit(1)
    } catch (e: Exception) {
        System.err.println("Ошибка выполнения: ${e.message}")
        System.exit(1)
    }
}

/**
 * Загружает данные о пакете с PyPI, используя URL-адрес из конфигурации.
 */
fun fetchPipDependencies(repoUrl: String, packageName: String, version: String): List<String> {
    val baseUrl = repoUrl.trimEnd('/')
    val urlString = "$baseUrl/pypi/$packageName/$version/json"

    val jsonText = try {
        URL(urlString).readText()
    } catch (e: Exception) {
        throw RuntimeException("Не удалось получить данные с PyPI для пакета $packageName")
    }
    return parseRequiresDist(jsonText)
}

/**
 * Извлекает чистые имена пакетов из массива "requires_dist" в JSON-тексте ответа PyPI.
 */
fun parseRequiresDist(jsonText: String): List<String> {
    val result = mutableListOf<String>()
    val marker = "\"requires_dist\":"
    val index = jsonText.indexOf(marker)
    if (index == -1) return emptyList()

    val startIndex = jsonText.indexOf("[", index)
    val endIndex = jsonText.indexOf("]", startIndex)
    if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) return emptyList()

    val arrayContent = jsonText.substring(startIndex + 1, endIndex)
    if (arrayContent.isBlank()) return emptyList()

    val items = arrayContent.split(",")
    for (item in items) {
        val cleaned = item.trim().trim('"').trim('\'')
        if (cleaned.isNotBlank()) {
            val parts = cleaned.split(Regex("[\\s;\\(><=!]"))
            val rawName = parts.firstOrNull() ?: ""
            if (rawName.isNotBlank() && !result.contains(rawName)) {
                result.add(rawName)
            }
        }
    }
    return result
}

/**
 * Разбирает и валидирует аргументы командной строки.
 */
fun parseArgs(args: Array<String>): AppConfig {
    var packageName = ""
    var repoUrlOrPath = ""
    var isTestMode: Boolean? = null
    var version = ""
    var maxDepth: Int? = null
    var filterSubstring = "none"

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--package" -> {
                if (packageName.isNotEmpty()) throw IllegalArgumentException("Флаг --package дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено имя пакета")
                packageName = args[++i]
            }
            "--repo" -> {
                if (repoUrlOrPath.isNotEmpty()) throw IllegalArgumentException("Флаг --repo дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущен путь к репозиторию")
                repoUrlOrPath = args[++i]
            }
            "--test-mode" -> {
                if (isTestMode != null) throw IllegalArgumentException("Флаг --test-mode дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено значение теста")
                val nextArg = args[++i]
                isTestMode = nextArg.toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("Режим теста должен быть 'true' или 'false'")
            }
            "--version" -> {
                if (version.isNotEmpty()) throw IllegalArgumentException("Флаг --version дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущена версия пакета")
                version = args[++i]
            }
            "--max-depth" -> {
                if (maxDepth != null) throw IllegalArgumentException("Флаг --max-depth дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено значение глубины")
                val nextArg = args[++i]
                maxDepth = nextArg.toIntOrNull()
                    ?: throw IllegalArgumentException("Глубина анализа должна быть числом")
            }
            "--filter" -> {
                if (filterSubstring != "none") throw IllegalArgumentException("Флаг --filter дублируется")
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущена подстрока фильтра")
                filterSubstring = args[++i]
            }
            else -> throw IllegalArgumentException("Неизвестный параметр: ${args[i]}")
        }
        i++
    }

    if (packageName.isBlank()) throw IllegalArgumentException("Параметр --package обязателен")
    if (repoUrlOrPath.isBlank()) throw IllegalArgumentException("Параметр --repo обязателен")
    if (version.isBlank()) throw IllegalArgumentException("Параметр --version обязателен")
    if (isTestMode == null) throw IllegalArgumentException("Параметр --test-mode обязателен")
    if (maxDepth == null) throw IllegalArgumentException("Параметр --max-depth обязателен")
    if (maxDepth < 0) throw IllegalArgumentException("Глубина анализа не может быть меньше 0")

    return AppConfig(packageName, repoUrlOrPath, isTestMode, version, maxDepth, filterSubstring)
}
