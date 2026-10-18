/**
 * Хранилище параметров конфигурации.
 *
 * @property packageName Имя анализируемого пакета.
 * @property repositoryUrlOrPath URL-адрес репозитория или путь к тестовому файлу.
 * @property isTestMode Режим работы с тестовым репозиторием (true/false).
 * @property packageVersion Версия анализируемого пакета.
 * @property maxDepth Максимальная глубина анализа зависимостей.
 * @property filterSubstring Подстрока для исключения пакетов из анализа.
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
 * Точка входа визуализатора графа зависимостей.
 *
 * @param args Массив аргументов командной строки.
 */
fun main(args: Array<String>) {
    try {
        val config = parseArgs(args)
        printConfig(config)
    } catch (e: IllegalArgumentException) {
        System.err.println("Ошибка конфигурации: ${e.message}")
        System.exit(1)
    }
}
/**
 * Разбирает и валидирует аргументы командной строки.
 * Гарантирует отсутствие дубликатов параметров и безопасный разбор типов.
 *
 * @param args Массив переданных CLI-опций.
 * @return Сформированный объект конфигурации [AppConfig].
 * @throws IllegalArgumentException При любых ошибках валидации или формата.
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
                if (packageName.isNotEmpty()) {
                    throw IllegalArgumentException("Параметр --package указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено имя пакета")
                packageName = args[++i]
            }
            "--repo" -> {
                if (repoUrlOrPath.isNotEmpty()) {
                    throw IllegalArgumentException("Параметр --repo указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущен путь/URL репозитория")
                repoUrlOrPath = args[++i]
            }
            "--test-mode" -> {
                if (isTestMode != null) {
                    throw IllegalArgumentException("Параметр --test-mode указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено значение режима теста")
                val nextArg = args[++i]
                isTestMode = nextArg.toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("Режим теста должен быть 'true' или 'false' (получено: '$nextArg')")
            }
            "--version" -> {
                if (version.isNotEmpty()) {
                    throw IllegalArgumentException("Параметр --version указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущена версия пакета")
                version = args[++i]
            }
            "--max-depth" -> {
                if (maxDepth != null) {
                    throw IllegalArgumentException("Параметр --max-depth указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущено значение глубины")
                val nextArg = args[++i]
                maxDepth = nextArg.toIntOrNull()
                    ?: throw IllegalArgumentException("Глубина анализа должна быть целым числом (получено: '$nextArg')")
            }
            "--filter" -> {
                if (filterSubstring != "none") {
                    throw IllegalArgumentException("Параметр --filter указан дважды")
                }
                if (i + 1 >= args.size) throw IllegalArgumentException("Пропущена подстрока фильтра")
                filterSubstring = args[++i]
            }
            else -> throw IllegalArgumentException("Неизвестный параметр: ${args[i]}")
        }
        i++
    }

    if (packageName.isBlank()) throw IllegalArgumentException("Параметр --package является обязательным")
    if (repoUrlOrPath.isBlank()) throw IllegalArgumentException("Параметр --repo является обязательным")
    if (version.isBlank()) throw IllegalArgumentException("Параметр --version является обязательным")
    if (isTestMode == null) throw IllegalArgumentException("Параметр --test-mode является обязательным")
    if (maxDepth == null) throw IllegalArgumentException("Параметр --max-depth является обязательным")
    if (maxDepth < 0) throw IllegalArgumentException("Максимальная глубина анализа не может быть отрицательной")

    return AppConfig(packageName, repoUrlOrPath, isTestMode, version, maxDepth, filterSubstring)
}
/**
 * Выводит все параметры конфигурации пользователя на экран в формате ключ-значение.
 *
 * @param config Объект текущей конфигурации для вывода.
 */
fun printConfig(config: AppConfig) {
    println("packageName: ${config.packageName}")
    println("repositoryUrlOrPath: ${config.repositoryUrlOrPath}")
    println("isTestMode: ${config.isTestMode}")
    println("packageVersion: ${config.packageVersion}")
    println("maxDepth: ${config.maxDepth}")
    println("filterSubstring: ${config.filterSubstring}")
}
