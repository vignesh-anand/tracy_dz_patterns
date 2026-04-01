# DZ Patterns - Skydiving Landing Pattern Calculator

Android app that calculates and visualizes right-hand landing patterns on a satellite map based on canopy performance and wind conditions.

## Setup

### Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project (or use an existing one)
3. Enable **Maps SDK for Android**
4. Create an API key under **Credentials**
5. Create a `local.properties` file in the project root:

```
MAPS_API_KEY=YOUR_API_KEY_HERE
```

### Build

Open in Android Studio, sync Gradle, and run on a device or emulator with Google Play Services.

## Features

- Satellite map view of the DZ
- Configurable canopy profiles (airspeed in knots, glide ratio)
- Manual or automatic wind input (via Open-Meteo API, no key needed)
- Tap to set landing target, drag to set approach heading
- Calculates right-hand pattern: entry (1000ft), base turn (600ft), final turn (300ft)
- Real-time pattern overlay on the map
