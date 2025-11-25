<#
run-gui.ps1
Attempts to download OpenJFX (Windows x64), compile the project with JavaFX on the module-path,
and launch the JavaFX `MainApp`.

Usage:
  Open PowerShell (normal is fine), cd to the project root and run:
    .\run-gui.ps1

If automatic download fails, the script will print manual steps.
#>

set -e

Write-Host "Run GUI helper starting..." -ForegroundColor Cyan

# Ensure Java is available
try {
    & java -version > $null 2>&1
} catch {
    Write-Host "Java not found in PATH. Install JDK 11+ and ensure 'java' is on PATH." -ForegroundColor Red
    exit 1
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $projectRoot) { $projectRoot = Get-Location }
Set-Location $projectRoot

# Destination for SDK (prefer user folder to avoid requiring admin rights)
$destRoot = Join-Path $env:USERPROFILE 'javafx-sdk-25'

if (-not (Test-Path $destRoot)) {
    Write-Host "OpenJFX SDK not found at $destRoot. Attempting download..." -ForegroundColor Yellow
    $zip = Join-Path $env:TEMP 'openjfx-sdk.zip'
    $urls = @(
        'https://download2.gluonhq.com/openjfx/25.0.2/openjfx-25.0.2_windows-x64_bin-sdk.zip',
        'https://download2.gluonhq.com/openjfx/25.0.1/openjfx-25.0.1_windows-x64_bin-sdk.zip'
    )
    $got = $false
    foreach ($u in $urls) {
        try {
            Write-Host "Downloading $u ..." -ForegroundColor Cyan
            Invoke-WebRequest -Uri $u -OutFile $zip -UseBasicParsing -TimeoutSec 120
            $got = $true; break
        } catch {
            Write-Host "Download failed for $u : $_" -ForegroundColor Yellow
        }
    }
    if (-not $got) {
        Write-Host "Automatic download failed. Please download OpenJFX SDK for Windows x64 manually from:" -ForegroundColor Red
        Write-Host "  https://gluonhq.com/products/javafx/" -ForegroundColor Red
        Write-Host "Then unzip it to a folder such as C:\\javafx-sdk-25 and re-run this script." -ForegroundColor Red
        exit 2
    }

    Write-Host "Extracting SDK to $destRoot ..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path $destRoot -Force | Out-Null
    try {
        Expand-Archive -Path $zip -DestinationPath $destRoot -Force
        Remove-Item $zip -Force
    } catch {
        Write-Host "Failed to extract SDK: $_" -ForegroundColor Red
        exit 3
    }
}

# Find the lib folder under the destRoot
$libDir = Get-ChildItem -Path $destRoot -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName 'lib') } | Select-Object -First 1
if ($libDir) { $javafx = Join-Path $libDir.FullName 'lib' } else { $javafx = Join-Path $destRoot 'lib' }

if (-not (Test-Path $javafx)) {
    Write-Host "Could not find JavaFX lib folder. Expected under $destRoot. Please verify SDK extraction." -ForegroundColor Red
    exit 4
}

Write-Host "JavaFX libs found in: $javafx" -ForegroundColor Green

# Compile all sources (including MainApp)
if (!(Test-Path out)) { New-Item -ItemType Directory out | Out-Null }
Write-Host "Compiling Java sources..." -ForegroundColor Cyan
try {
    $files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    if ($files.Count -eq 0) { Write-Host "No .java files found."; exit 5 }
    javac --module-path $javafx --add-modules javafx.controls,javafx.fxml -d out $files
} catch {
    Write-Host "Compilation failed: $_" -ForegroundColor Red
    exit 6
}

Write-Host "Launching GUI..." -ForegroundColor Cyan
try {
    java --module-path $javafx --add-modules javafx.controls,javafx.fxml -cp out azurewallet.main.MainApp
} catch {
    Write-Host "Failed to launch JavaFX app: $_" -ForegroundColor Red
    exit 7
}
