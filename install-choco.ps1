<#
  install-choco.ps1
  Simple script to install Chocolatey using the official installer.
  Run this PowerShell script in an elevated (Run as Administrator) PowerShell.

  Usage (from repository folder):
    Right-click PowerShell -> Run as Administrator
    cd 'C:\Users\karll\Downloads\sss\azure_final'
    .\install-choco.ps1
#>

# Check for administrative privileges
$isAdmin = (New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "This script must be run as Administrator."
    Write-Host "Open Start → PowerShell → Right-click → 'Run as Administrator', then run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Running Chocolatey installer (requires Internet access)..." -ForegroundColor Cyan

try {
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
} catch {
    Write-Host "Chocolatey installation failed: $_" -ForegroundColor Red
    exit 2
}

# After install, show choco version (you may need to open a new shell)
if (Get-Command choco -ErrorAction SilentlyContinue) {
    Write-Host "Chocolatey installation completed." -ForegroundColor Green
    choco -v
} else {
    Write-Host "Chocolatey installed, but 'choco' command not found in this session." -ForegroundColor Yellow
    Write-Host "Close and reopen PowerShell (Run as Administrator) and run `choco -v` to verify." -ForegroundColor Yellow
}
