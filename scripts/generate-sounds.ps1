<#
.SYNOPSIS
    Generates Lamla's 5 notification sounds as .ogg files via ffmpeg.

.DESCRIPTION
    Each sound is designed for a specific notification category:

      chime.ogg   — Class reminder. Two-note ascending bell (880Hz → 1320Hz).
                    Soft, attention-getting without urgency. ~0.7s.

      urgent.ogg  — Deadline warning. Falling minor third (660Hz → 528Hz),
                    repeated once for emphasis. ~1.0s.

      alarm.ogg   — Deadline imminent (<1h). Rapid alternating 880/660Hz
                    pattern. Insistent — designed to break attention.  ~1.4s.

      ping.ogg    — Study session / office hours. Single high ping (1200Hz)
                    with quick decay. Polite, non-disruptive.  ~0.25s.

      bell.ogg    — Exam alert. Slow descending arpeggio (1200→800→400Hz)
                    over a sustained tone. Distinctive, signals importance. ~1.3s.

    All files are 22.05kHz mono Ogg Vorbis at quality 4 (~24kbps avg).
    Total size: ~25KB for all five.

.NOTES
    Requires ffmpeg in PATH. Install with:
        winget install Gyan.FFmpeg
    or
        choco install ffmpeg

    If ffmpeg isn't installed, the script exits cleanly and the app falls
    back to the Android system default notification sound for every channel.
    No crash, no missing-resource error — just a less distinctive audio
    experience until you run this.
#>
param(
    [string]$OutDir = (Join-Path $PSScriptRoot "..\app\src\main\res\raw")
)

$ErrorActionPreference = "Stop"

# --- ffmpeg presence check -----------------------------------------------------
$ff = Get-Command ffmpeg -ErrorAction SilentlyContinue
if (-not $ff) {
    Write-Host ""
    Write-Host "ffmpeg not installed." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Install one of these and re-run:"
    Write-Host "    winget install Gyan.FFmpeg"
    Write-Host "    choco install ffmpeg"
    Write-Host ""
    Write-Host "Lamla works without these — channels fall back to the Android"
    Write-Host "system default notification sound. No crash, just less distinctive."
    Write-Host ""
    exit 0
}

# --- Output directory ----------------------------------------------------------
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$OutDir = (Resolve-Path $OutDir).Path
Write-Host "Writing to: $OutDir" -ForegroundColor Cyan
Write-Host ""

# --- Generators ---------------------------------------------------------------
# Helper: run ffmpeg with consistent flags + silent stderr unless error
function Invoke-Ffmpeg([string[]]$args, [string]$label) {
    Write-Host "  $label" -NoNewline
    $out = & ffmpeg -y -hide_banner -loglevel error @args 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host " FAILED" -ForegroundColor Red
        Write-Host $out
        throw "ffmpeg failed for $label"
    }
    Write-Host " ✓" -ForegroundColor Green
}

# 1. chime — two-note ascending bell
Invoke-Ffmpeg @(
    "-f","lavfi","-i","sine=f=880:d=0.18:r=22050",
    "-f","lavfi","-i","sine=f=1320:d=0.5:r=22050",
    "-filter_complex","[0:a]afade=in:d=0.02,afade=out:st=0.12:d=0.06[a];" +
                      "[1:a]adelay=170|170,afade=in:d=0.02,afade=out:st=0.4:d=0.1[b];" +
                      "[a][b]amix=inputs=2:duration=longest,volume=0.7",
    "-ac","1","-c:a","libvorbis","-q:a","4",
    (Join-Path $OutDir "chime.ogg")
) "chime.ogg   (class reminder)"

# 2. urgent — falling minor third, repeated
Invoke-Ffmpeg @(
    "-f","lavfi","-i","sine=f=660:d=0.22:r=22050",
    "-f","lavfi","-i","sine=f=528:d=0.32:r=22050",
    "-f","lavfi","-i","sine=f=660:d=0.22:r=22050",
    "-f","lavfi","-i","sine=f=528:d=0.32:r=22050",
    "-filter_complex","[0:a]afade=in:d=0.02,afade=out:st=0.16:d=0.06[a];" +
                      "[1:a]adelay=210|210,afade=in:d=0.02,afade=out:st=0.26:d=0.06[b];" +
                      "[2:a]adelay=520|520,afade=in:d=0.02,afade=out:st=0.16:d=0.06[c];" +
                      "[3:a]adelay=730|730,afade=in:d=0.02,afade=out:st=0.26:d=0.06[d];" +
                      "[a][b][c][d]amix=inputs=4:duration=longest,volume=0.8",
    "-ac","1","-c:a","libvorbis","-q:a","4",
    (Join-Path $OutDir "urgent.ogg")
) "urgent.ogg  (deadline warning)"

# 3. alarm — alternating 880/660 pulses (insistent)
Invoke-Ffmpeg @(
    "-f","lavfi","-i","sine=f=880:d=0.12:r=22050",
    "-f","lavfi","-i","sine=f=660:d=0.12:r=22050",
    "-f","lavfi","-i","sine=f=880:d=0.12:r=22050",
    "-f","lavfi","-i","sine=f=660:d=0.12:r=22050",
    "-f","lavfi","-i","sine=f=880:d=0.12:r=22050",
    "-f","lavfi","-i","sine=f=660:d=0.4:r=22050",
    "-filter_complex","[0:a]afade=in:d=0.01,afade=out:st=0.09:d=0.03[a];" +
                      "[1:a]adelay=170|170,afade=in:d=0.01,afade=out:st=0.09:d=0.03[b];" +
                      "[2:a]adelay=340|340,afade=in:d=0.01,afade=out:st=0.09:d=0.03[c];" +
                      "[3:a]adelay=510|510,afade=in:d=0.01,afade=out:st=0.09:d=0.03[d];" +
                      "[4:a]adelay=680|680,afade=in:d=0.01,afade=out:st=0.09:d=0.03[e];" +
                      "[5:a]adelay=850|850,afade=in:d=0.02,afade=out:st=0.32:d=0.08[f];" +
                      "[a][b][c][d][e][f]amix=inputs=6:duration=longest,volume=0.9",
    "-ac","1","-c:a","libvorbis","-q:a","4",
    (Join-Path $OutDir "alarm.ogg")
) "alarm.ogg   (imminent deadline)"

# 4. ping — short high-frequency pop
Invoke-Ffmpeg @(
    "-f","lavfi","-i","sine=f=1200:d=0.22:r=22050",
    "-af","afade=in:d=0.005,afade=out:st=0.08:d=0.13,volume=0.65",
    "-ac","1","-c:a","libvorbis","-q:a","4",
    (Join-Path $OutDir "ping.ogg")
) "ping.ogg    (study / office hours)"

# 5. bell — descending arpeggio over sustained tone
Invoke-Ffmpeg @(
    "-f","lavfi","-i","sine=f=400:d=1.0:r=22050",
    "-f","lavfi","-i","sine=f=1200:d=0.35:r=22050",
    "-f","lavfi","-i","sine=f=800:d=0.35:r=22050",
    "-f","lavfi","-i","sine=f=400:d=0.45:r=22050",
    "-filter_complex","[0:a]volume=0.4,afade=in:d=0.05,afade=out:st=0.85:d=0.15[a];" +
                      "[1:a]afade=in:d=0.02,afade=out:st=0.28:d=0.07[b];" +
                      "[2:a]adelay=280|280,afade=in:d=0.02,afade=out:st=0.28:d=0.07[c];" +
                      "[3:a]adelay=560|560,afade=in:d=0.02,afade=out:st=0.38:d=0.07[d];" +
                      "[a][b][c][d]amix=inputs=4:duration=longest,volume=0.85",
    "-ac","1","-c:a","libvorbis","-q:a","4",
    (Join-Path $OutDir "bell.ogg")
) "bell.ogg    (exam alert)"

Write-Host ""
Write-Host "Done — 5 .ogg files written. Total size:" -ForegroundColor Cyan
Get-ChildItem -Path $OutDir -Filter "*.ogg" |
    Select-Object Name, @{n='KB';e={[math]::Round($_.Length/1024,1)}} |
    Format-Table -AutoSize
