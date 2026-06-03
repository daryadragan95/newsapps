# News Sport Winner

Android MVP for sports news and upcoming matches. The app shows a 3-day schedule by sport, a selected match card, and core data such as league, time, status, round, venue, and score when the API has published it.

## Features

- `Matches` tab with schedule and selected match details;
- `News` tab with an ESPN RSS feed for the selected sport;
- `Favorites` tab with locally saved teams;
- `Team` screen with logo, country, stadium, founded year, description, and recent/upcoming matches;
- match reminders: 30 minutes before and at match start;
- match, league, and team images loaded with Coil.

## API

The project uses the open TheSportsDB v1 JSON API:

- daily schedule: `eventsday.php`
- event details: `lookupevent.php`
- team search: `searchteams.php`
- team upcoming/recent matches: `eventsnext.php`, `eventslast.php`

News is loaded from ESPN RSS:

- soccer
- NBA
- NHL
- NFL

The default API key is the free demo key `123`. To use your own key, add this to `~/.gradle/gradle.properties` or pass it to the build command:

```properties
sportsDbApiKey=YOUR_KEY
```

```bash
./gradlew assembleDebug -PsportsDbApiKey=YOUR_KEY
```

## Run

```bash
./gradlew assembleDebug
```

The APK will be generated here:

```text
app/build/outputs/apk/debug/app-debug.apk
```
