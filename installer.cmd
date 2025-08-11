@echo off
setlocal

set FXJMODS=C:\Users\sound\OneDrive\Projects\javafx-jmods-21.0.8
set JDKJMODS=%JAVA_HOME%\jmods

for /f "delims=" %%m in ('
  jdeps --multi-release 21 --ignore-missing-deps --print-module-deps ^
        --class-path target\lib\* ^
        target\json2csv.jar
') do set MODS=%%m

jlink ^
  --module-path "%FXJMODS%;%JDKJMODS%;target/lib" ^
  --add-modules %MODS%,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,jdk.crypto.ec ^
  --strip-debug --no-header-files --no-man-pages --compress=2 ^
  --output build\runtime

jpackage ^
  --type exe ^
  --name "Json2Csv" ^
  --app-version "1.0.0" ^
  --input target ^
  --main-jar json2csv.jar ^
  --main-class org.overb.jsontocsv.App ^
  --runtime-image build\runtime ^
  --icon src\main\resources\icons\j2c-48.ico ^
  --win-menu ^
  --win-shortcut ^
  --win-console
