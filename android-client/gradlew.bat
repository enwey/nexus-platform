@ECHO OFF
SET DIR=%~dp0
SET CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO Missing gradle wrapper jar: %CLASSPATH%
  EXIT /B 1
)

IF DEFINED JAVA_HOME (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) ELSE (
  SET JAVA_EXE=java
)

IF NOT EXIST "%JAVA_EXE%" (
  IF "%JAVA_EXE%"=="java" (
    ECHO Java not found in PATH. Please set JAVA_HOME or install JDK.
  ) ELSE (
    ECHO Java executable not found: %JAVA_EXE%
  )
  EXIT /B 1
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
