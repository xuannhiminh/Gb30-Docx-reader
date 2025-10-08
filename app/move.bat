@echo off
setlocal

set SOURCE_BASE=C:\TripSoft\hv102b-android-pdf-ns2b\document_lib\src\main\res
set DEST_BASE=C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res
set FILE_NAME=ic_notitication.png

set FOLDERS=drawable-hdpi drawable-mdpi drawable-xhdpi drawable-xxhdpi drawable-xxxhdpi

for %%F in (%FOLDERS%) do (
    if exist "%SOURCE_BASE%\%%F\%FILE_NAME%" (
        echo Moving %%F\%FILE_NAME% ...
        if not exist "%DEST_BASE%\%%F" (
            mkdir "%DEST_BASE%\%%F"
        )
        move /Y "%SOURCE_BASE%\%%F\%FILE_NAME%" "%DEST_BASE%\%%F\"
    ) else (
        echo File not found in %%F
    )
)

echo Done.
pause
