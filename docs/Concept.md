# Konzept des Tippspiels

## Grundidee

Tipper geben Vorhersagen für einzelne Turner an einzelnen Geräten ab. Je genauer die Vorhersage, desto mehr Punkte.

## Punktesystem

- **Exakt getroffen**: 3 Punkte
- **Abweichung bis 5%**: 2 Punkte
- **Abweichung bis 10%**: 1 Punkt
- **Mehr als 10% daneben**: 0 Punkte

## Datenmodell

Hier die wichtigsten Entitäten für deine Anwendung:

### Haupttabellen

```sql
-- Wettkämpfe
Competition
(
  id, 
  name (z.B. "Swiss Cup Halbfinal 1"),
  date,
  status (upcoming, live, finished)
)

-- Turner/Turnerinnen
Gymnast (
  id,
  name,
  team_name,
  gender
)

-- Geräte
Apparatus (
  id,
  name (Reck, Boden, etc.),
  gender (M/F)
)

-- Mögliche Kombinationen für einen Wettkampf
CompetitionEntry (
  id,
  competition_id,
  gymnast_id,
  apparatus_id,
  actual_score (null bis Resultat da ist)
)

-- Benutzer
User (
  id,
  username,
  email,
  password_hash,
  role (USER/ADMIN)
)

-- Tipps
Prediction (
  id,
  user_id,
  competition_entry_id,
  predicted_score,
  points_earned (berechnet nach Wettkampf),
  created_at
)
```

## Benutzeroberfläche

### Hauptseiten

1. **Registrierung/Login**
    - Einfaches Formular für neue Tipper

2. **Wettkampfübersicht**
    - Liste der kommenden Halbfinals und Finals
    - Status anzeigen (offen für Tipps, läuft, beendet)

3. **Tippabgabe**
    - Grid mit Turner/Gerät Kombinationen
    - Eingabefelder für Noten (z.B. 14.500)
    - Speichern Button

4. **Live Resultate**
    - Aktuelle Noten vom Wettkampf
    - Sofortige Punkteberechnung

5. **Rangliste**
    - Gesamtpunkte pro Tipper
    - Details pro Wettkampf

## Technische Umsetzung mit Vaadin

### Views

```java

@Route("predictions")
public class PredictionView extends VerticalLayout {
    private Grid<CompetitionEntry> grid;
    private Map<Long, TextField> scoreFields;

    // Grid zeigt Turner, Team, Gerät
    // TextField für jeden Tipp
    // Save Button speichert alle Tipps
}

@Route("leaderboard")
public class LeaderboardView extends VerticalLayout {
    private Grid<UserScore> leaderboard;
    // Zeigt Rangliste mit Punkten
}
```

### Punkteberechnung

```java
public int calculatePoints(double predicted, double actual) {
    double difference = Math.abs(predicted - actual);
    double percentage = (difference / actual) * 100;

    if (difference < 0.001) return 3;  // Exakt
    if (percentage <= 5) return 2;
    if (percentage <= 10) return 1;
    return 0;
}
```

## Zusätzliche Features

### Nice to have

- **Tippschluss**: Automatisch 30 Minuten vor Wettkampfbeginn
- **Push Notifications**: Bei Live Resultaten
- **Teamwertung**: Zusätzlich zur Einzelwertung
- **Historie**: Vergangene Tipps und Resultate ansehen
- **Statistiken**: Wer tippt am besten bei welchem Gerät

### Admin Bereich

- Wettkämpfe anlegen
- Turner/Geräte Kombinationen pflegen
- Resultate eingeben (oder Import via Excel/CSV)
