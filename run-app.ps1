# Simple PowerShell script to run the Azure Wallet application
Write-Host "Starting Azure Digital Wallet..." -ForegroundColor Cyan

# Check if JavaFX SDK exists
$javafxPath = "$env:USERPROFILE\javafx-sdk-21.0.1"
if (-not (Test-Path $javafxPath)) {
    Write-Host "JavaFX SDK not found at $javafxPath" -ForegroundColor Yellow
    Write-Host "Downloading JavaFX SDK 21.0.1..." -ForegroundColor Cyan
    
    $zipFile = "$env:TEMP\javafx-sdk.zip"
    $url = "https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_windows-x64_bin-sdk.zip"
    
    try {
        Invoke-WebRequest -Uri $url -OutFile $zipFile -UseBasicParsing
        Write-Host "Extracting JavaFX SDK..." -ForegroundColor Cyan
        Expand-Archive -Path $zipFile -DestinationPath $env:USERPROFILE -Force
        Remove-Item $zipFile
        Write-Host "JavaFX SDK installed successfully!" -ForegroundColor Green
    } catch {
        Write-Host "Failed to download JavaFX SDK. Please download manually from:" -ForegroundColor Red
        Write-Host $url -ForegroundColor Yellow
        Write-Host "Extract to: $javafxPath" -ForegroundColor Yellow
        exit 1
    }
}

# Compile if needed
if (-not (Test-Path "target\classes\azurewallet\main\MainApp.class")) {
    Write-Host "Compiling project..." -ForegroundColor Cyan
    mvn clean compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Compilation failed!" -ForegroundColor Red
        exit 1
    }
}

# Run the application
Write-Host "Launching application..." -ForegroundColor Green
$javafxLib = "$javafxPath\lib"

java --module-path $javafxLib --add-modules javafx.controls,javafx.fxml -cp target\classes azurewallet.main.MainApp

Write-Host "`nApplication closed." -ForegroundColor Cyan
