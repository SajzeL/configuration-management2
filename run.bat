@echo off
CHCP 65001 > nul
rem Сборка проекта перед запуском (можно закомментировать, если запускаете повторно)
call gradlew.bat assemble -q

rem Запуск скомпилированного Kotlin-приложения с передачей всех CLI аргументов
java -jar build/libs/File_Manager-1.0-SNAPSHOT.jar %*
