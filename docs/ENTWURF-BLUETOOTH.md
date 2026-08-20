# Entwurf: Dateien ohne Netz austauschen (Bluetooth)

Stand 17.08.2026. **Entwurf; der plattformneutrale Teil steht bereits, der
Android-Teil ist nicht gebaut.** Anlass ist der Wunsch vom 17.08.2026, „das ein
modul eingebaut wird … mit der man ohne internett also mit wlan oder blutooth
datein der app leicht austauschen kann".

## Warum nicht WLAN

WLAN-Direct und Wi-Fi Aware verlangen auf Android die Berechtigung
`INTERNET` (dazu `ACCESS_WIFI_STATE` und Ortungsrechte). Damit fiele die
Zusage, die in Regel 2 steht und die der Berechtigungswächter in
`androidApp/build.gradle.kts` bei jedem Bau erzwingt. **Genau so wurde es am
28.07.2026 entschieden:** Bluetooth ist der einzige Weg für ganze
Pakete, NFC und QR nur für Kleinstdaten. Der Wunsch „mit WLAN" ist damit nicht
umsetzbar, ohne die Grundzusage der App aufzugeben.

## Der Haken, den man kennen muss: Bluetooth-Suche will die Ortung

Auf Android **bis Fassung 30** verlangt das Suchen nach Geräten
(`startDiscovery`) die Berechtigung `ACCESS_FINE_LOCATION` — das System
behandelt eine Geräteliste als Ortsangabe. Eine App, die verspricht, den
Standort nie zu kennen, darf danach nicht fragen.

**Deshalb wird gar nicht gesucht.** Der Entwurf kommt ohne Suche, ohne
Sichtbarmachen und ohne Werben aus:

> **Gekoppelt wird in den Android-Einstellungen, nicht in dieser App.**
> Compass Zero spricht ausschließlich mit Geräten, die dort schon gekoppelt
> sind.

Das kostet einen Schritt beim ersten Mal und spart die Ortungsberechtigung
dauerhaft. Nötig sind dann nur:

| Android | Berechtigung | Ortung nötig? |
|---|---|---|
| bis 30 | `BLUETOOTH`, `BLUETOOTH_ADMIN` | **nein** (nur beim Suchen, und das entfällt) |
| ab 31 | `BLUETOOTH_CONNECT` | **nein** |

`BLUETOOTH_SCAN` und `BLUETOOTH_ADVERTISE` werden **nicht** verlangt. Die
Liste `ERLAUBTE_BERECHTIGUNGEN` bekommt genau die drei oben — bewusst und
einzeln, so wie es der Kommentar dort vorsieht.

## Der zweite Haken: wie lange es dauert

Gerechnet mit den tatsächlichen Dateigrößen des Projekts:

| Datei | 100 kB/s | 150 kB/s | 250 kB/s |
|---|---:|---:|---:|
| Inhaltspaket `europe-de` (10,4 MB) | 106 s | **71 s** | 43 s |
| Karte `europa-uebersicht` (437 MB) | 75 min | **50 min** | 30 min |
| Karte `deutschland-nord` (770 MB) | 2,2 h | **88 min** | 53 min |
| Detailkarte (2 GB) | 5,8 h | **3,9 h** | 2,3 h |

**Daraus folgt die ehrliche Einordnung:** Für Inhaltspakete ist Bluetooth
richtig — eine Minute, und der Nachbar hat das Handbuch. Für eine
Überblickskarte ist es eine Stunde, in der beide Geräte beieinander liegen
müssen. Für Detailkarten ist es keine Lösung; die gehören über Kabel oder
Speicherkarte.

Die Oberfläche muss das **vorher** sagen, nicht hinterher: geschätzte Dauer
neben dem Dateinamen, bevor man auf „Senden" tippt.

## Was schon steht

`core/transfer` ist plattformneutral fertig und mit 82 Prüfungen abgedeckt:

* `Rahmen`, `RahmenCodec`, `RahmenLeser` — das Rahmenformat,
* `Sender` und `Empfaenger` — die Gegenstellen,
* `Datenquelle` und `Datensenke` — die Abstraktion, hinter der eine beliebige
  Verbindung stecken darf.

**Der Android-Teil muss also nur einen Bluetooth-Socket hinter diese beiden
Schnittstellen hängen.** Das ist der ganze Rest.

## Was zu bauen ist

1. `ERLAUBTE_BERECHTIGUNGEN` um die drei Bluetooth-Rechte erweitern, Manifest
   entsprechend — der Wächter erzwingt, dass das eine bewusste Zeile ist.
2. `BluetoothVerbindung`: RFCOMM über eine feste UUID, Client und Server, die
   Ströme als `Datenquelle`/`Datensenke`.
3. Oberfläche unter „Pakete austauschen": Liste der **gekoppelten** Geräte,
   Auswahl der Datei, geschätzte Dauer, Fortschritt, Abbruch.
4. Jedes empfangene Paket durchläuft **dieselbe Signaturprüfung** wie ein von
   Hand kopiertes — es wird erst nach bestandener Prüfung an seinen Platz
   gelegt. Ein Paket über Bluetooth ist kein vertrauenswürdigeres Paket.

## Was noch zu entscheiden ist

1. **Reicht Bluetooth für den vorgesehenen Zweck?** Geht es um Karten, ist
   Kabel oder Speicherkarte der bessere Weg, und Bluetooth bleibt für die
   Inhaltspakete.
2. **Koppeln in den Android-Einstellungen** — einverstanden? Die Alternative
   wäre die Ortungsberechtigung, und die widerspricht der Zusage der App.
