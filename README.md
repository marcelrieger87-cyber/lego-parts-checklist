# LEGO Teile-Checkliste (Android)

Android-App als GitHub-Startprojekt für den Import von LEGO-Anleitungen als PDF und die Umwandlung der Ersatzteilliste in eine abhakebare Inventarliste.

## Aktueller Stand

Diese Version ist speziell auf den Wunsch ausgelegt, **die letzten Seiten einer Anleitung zu analysieren** und die Teile **in derselben Reihenfolge wie in der PDF** in einer mobilen Liste darzustellen.

Bereits umgesetzt:

- PDF-Import über den Android-Dateiauswahldialog
- Analyse der letzten vier PDF-Seiten
- Erkennung von **Menge + Teilenummer** aus Inventarseiten
- Beibehaltung der ursprünglichen PDF-Reihenfolge
- Mobile Listenansicht: links Platz für Teilbild, rechts Abhakfunktion
- Eingabe von `Benötigt` und `Habe`
- Zusammenfassung fehlender Teile
- Kopierbare Fehlteilliste für externe Shops / Bricklink / LEGO Ersatzteilservice

## Aktuelle Grenzen

- **Teilnamen** stehen in vielen LEGO-PDFs nicht als Text im PDF und werden deshalb noch nicht automatisch erkannt.
- **Einzelbilder pro Teil** sind als Platzhalter vorbereitet, aber noch nicht automatisch aus den Inventarseiten ausgeschnitten.
- Für gescannte PDFs wäre zusätzlich **OCR / Bilderkennung** nötig.

## Nächster sinnvoller Ausbauschritt

1. Inventarseiten als Bitmap rendern
2. Kacheln/Elemente visuell segmentieren
3. Je Zeile Bildausschnitt + Menge + Teilenummer verbinden
4. Optional Teile-Namen über externe Datenquelle nachladen

## Tech-Stack

- Kotlin
- Jetpack Compose
- Android DataStore
- PDFBox-Android

## Projektstart

1. Projekt in Android Studio öffnen
2. Gradle synchronisieren
3. App auf Gerät oder Emulator starten
4. PDF laden und Inventar importieren

## Idee für GitHub-Repository

- `main`: stabile Version
- `feature/pdf-image-cropping`: automatische Bildausschnitte
- `feature/ocr`: OCR für gescannte Anleitungen
- `feature/part-name-lookup`: optionale Teiledatenbank-Anbindung
