<#
.SYNOPSIS
    Packages the clean source code of csv-vat-calculator for release delivery.
.DESCRIPTION
    Excludes build artifacts (target/), IDE files (.idea/, .vscode/, .settings/), 
    git directory (.git/), and previous zip archives.
#>
param(
    [string]$OutputFile = "csv-vat-calculator-final.zip"
)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
if (-not $projectRoot) {
    $projectRoot = Get-Location
}

$zipPath = Join-Path $projectRoot $OutputFile
if (Test-Path $zipPath) {
    Write-Host "Removing existing $OutputFile..." -ForegroundColor Yellow
    Remove-Item $zipPath -Force
}

Write-Host "Packaging project into $OutputFile..." -ForegroundColor Cyan

# Directories and files to exclude
$excludeDirs = @('target', '.git', '.idea', '.vscode', '.settings', 'bin')
$excludeFiles = @('.classpath', '.project', '*.zip', '*.log')

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $allFiles = Get-ChildItem -Path $projectRoot -Recurse -File
    $count = 0

    foreach ($file in $allFiles) {
        $relPath = $file.FullName.Substring($projectRoot.Length).TrimStart('\', '/')
        $pathParts = $relPath.Split([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)

        # Check directory exclusions
        $skip = $false
        foreach ($part in $pathParts[0..($pathParts.Length - 2)]) {
            if ($excludeDirs -contains $part) {
                $skip = $true
                break
            }
        }
        if ($skip) { continue }

        # Check filename exclusions
        foreach ($pattern in $excludeFiles) {
            if ($file.Name -like $pattern) {
                $skip = $true
                break
            }
        }
        if ($skip) { continue }

        $entryName = $relPath.Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $entryName) | Out-Null
        $count++
    }

    Write-Host "Successfully packaged $count files into $OutputFile!" -ForegroundColor Green
    $fileSizeKb = [math]::Round((Get-Item $zipPath).Length / 1KB, 2)
    Write-Host "Archive size: $fileSizeKb KB" -ForegroundColor Green
} finally {
    $zip.Dispose()
}
