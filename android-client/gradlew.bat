@ECHO OFF
SET DIR=%~dp0
SET CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO Missing gradle wrapper jar: %CLASSPATH%
  EXIT /B 1
)

java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
