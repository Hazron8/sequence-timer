# Firebase Setup Guide

This app uses Firebase for cloud sync. Follow these steps to set up your own Firebase project.

## Prerequisites

- A Google account
- Android Studio

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter a project name (e.g., "Sequence Timer")
4. Disable Google Analytics (optional) and create the project

## Step 2: Add Android App to Firebase

1. In your Firebase project, click "Add app" and select Android
2. Enter the package name: `com.hazron.sequencetimer`
3. Enter a nickname (e.g., "Sequence Timer Android")
4. You can skip the SHA-1 for now (or add it for Google Sign-In - see below)
5. Click "Register app"

## Step 3: Download Configuration

1. Download the `google-services.json` file
2. Place it in the `app/` directory of this project
3. The file should be at: `app/google-services.json`

## Step 4: Enable Authentication

1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Go to "Sign-in method" tab
4. Enable "Google" provider
5. Configure with your support email
6. Copy the "Web client ID" - you'll need this

## Step 5: Enable Firestore

1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Start in "production mode" or "test mode"
4. Select a region close to you
5. Click "Enable"

## Step 6: Set Firestore Rules

Go to Firestore > Rules and set:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

This ensures users can only read/write their own data.

## Step 7: Add SHA-1 Certificate (Required for Google Sign-In)

### Debug Certificate

```bash
cd android
./gradlew signingReport
```

Or directly:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### Release Certificate

Use your release keystore:

```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-alias
```

Add both SHA-1 fingerprints to your Firebase Android app settings.

## Step 8: Update Web Client ID

The `default_web_client_id` string resource should be automatically populated from `google-services.json`. If not, manually update `app/src/main/res/values/strings.xml`:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
```

## Troubleshooting

### Sign-in fails with "ApiException: 10"
- Make sure SHA-1 fingerprint is added to Firebase
- Verify the package name matches exactly

### Sign-in fails with "ApiException: 12500"
- Web client ID may be incorrect
- Check that Google Sign-In is enabled in Firebase Console

### Firestore permission denied
- Check your Firestore rules
- Make sure the user is authenticated

## Data Structure

Your data will be stored under:
```
users/{userId}/
  categories/{categoryId}
  timers/{timerId}
  sequences/{sequenceId}
  sequence_steps/{stepId}
```
