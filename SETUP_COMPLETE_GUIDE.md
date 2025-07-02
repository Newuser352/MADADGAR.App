# Complete Setup Guide: Supabase + Google Sign-In

## ✅ What's Already Configured

Your project is **almost ready** and has the following components already set up:

### Google Sign-In ✅
- ✅ Google Play Services Auth dependency added
- ✅ Google Services plugin configured
- ✅ `google-services.json` file present
- ✅ Web Client ID configured in `strings.xml`
- ✅ Google Sign-In UI and logic implemented

### Supabase ✅
- ✅ Supabase SDK dependencies added
- ✅ SupabaseClient.kt with complete auth, database, and storage setup
- ✅ SupabaseAuth.java wrapper for Java compatibility
- ✅ OTP and Google Sign-In flows implemented

## 🔧 What You Need to Configure

### 1. Configure Supabase Project

**You need to:**
1. Go to [supabase.com](https://supabase.com) and create a new project
2. Get your project credentials from **Settings → API**
3. Update `SupabaseClient.kt` with your actual credentials

**Current state:**
```kotlin
// File: app/src/main/java/com/example/madadgarapp/utils/SupabaseClient.kt
private const val SUPABASE_URL = "YOUR_SUPABASE_URL" // ← NEEDS UPDATE
private const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY" // ← NEEDS UPDATE
```

**What you need to replace:**
```kotlin
private const val SUPABASE_URL = "https://your-project-id.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // Your actual anon key
```

### 2. Set Up Supabase Authentication

In your Supabase dashboard:

1. **Enable Google Provider:**
   - Go to **Authentication → Providers**
   - Enable Google provider
   - Use your **Web Client ID**: `763585977372-4l23eh7fppjklu6apvoioq1dfgi9l8fh.apps.googleusercontent.com`
   - Get the **Client Secret** from Google Cloud Console

2. **Configure Redirect URLs:**
   - Add redirect URL: `com.example.madadgarapp://auth-callback`

### 3. Verify Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project (`madadgar-app`)
3. **Verify you have TWO client IDs:**
   - **Android Client ID** (for google-services.json) ✅
   - **Web Application Client ID** (for strings.xml) ✅

Your current Web Client ID in `strings.xml` looks correct: `763585977372-4l23eh7fppjklu6apvoioq1dfgi9l8fh.apps.googleusercontent.com`

## 🚀 Quick Start Instructions

### Step 1: Update Supabase Credentials

1. **Create Supabase Project:**
   ```
   1. Go to https://supabase.com
   2. Click "New Project"
   3. Choose organization and name your project
   4. Wait for setup to complete
   ```

2. **Get Your Credentials:**
   ```
   1. In Supabase Dashboard → Settings → API
   2. Copy "Project URL" (e.g., https://abcdefgh.supabase.co)
   3. Copy "anon public" key (starts with eyJ...)
   ```

3. **Update SupabaseClient.kt:**
   ```kotlin
   // Replace these lines in SupabaseClient.kt (around line 32-33)
   private const val SUPABASE_URL = "https://your-actual-project.supabase.co"
   private const val SUPABASE_ANON_KEY = "eyJ...your-actual-anon-key"
   ```

### Step 2: Configure Google Provider in Supabase

1. **In Supabase Dashboard:**
   ```
   1. Go to Authentication → Providers
   2. Find Google and click "Configure"
   3. Client ID: 763585977372-4l23eh7fppjklu6apvoioq1dfgi9l8fh.apps.googleusercontent.com
   4. Get Client Secret from Google Cloud Console → Credentials → Web client
   5. Enable the provider
   ```

### Step 3: Test Your Setup

1. **Clean and rebuild:**
   ```bash
   .\gradlew clean
   .\gradlew assembleDebug
   ```

2. **Run the app and test:**
   - Try Google Sign-In
   - Try Email/OTP authentication

## 🛠️ Troubleshooting

### If Google Sign-In shows Error 10:
- Verify you're using the **Web Client ID** in strings.xml (not Android Client ID)
- Check SHA-1 fingerprint matches in Google Cloud Console
- Ensure OAuth consent screen is configured

### If Supabase connection fails:
- Verify SUPABASE_URL and SUPABASE_ANON_KEY are correctly set
- Check network permissions in AndroidManifest.xml
- Look for initialization errors in logs

### For OTP/Email issues:
- Configure email provider in Supabase Authentication settings
- Set up redirect URLs in Authentication → URL Configuration

## 📋 Current Configuration Status

| Component | Status | Notes |
|-----------|--------|-------|
| Google Sign-In SDK | ✅ Ready | Dependencies and UI configured |
| Google Services File | ✅ Ready | google-services.json present |
| Google Web Client ID | ✅ Ready | Configured in strings.xml |
| Supabase SDK | ✅ Ready | All dependencies added |
| Supabase Client | ⚠️ Needs Config | Update credentials in SupabaseClient.kt |
| Google Provider | ⚠️ Needs Config | Configure in Supabase dashboard |
| Database Schema | ⚠️ Optional | Set up tables as needed |

## 🎯 Next Steps

1. **PRIORITY 1:** Configure Supabase project and update credentials
2. **PRIORITY 2:** Set up Google provider in Supabase
3. **PRIORITY 3:** Test authentication flows
4. **Optional:** Set up database tables and storage buckets

Once you complete steps 1 and 2, your app will be fully functional with both Google Sign-In and Email/OTP authentication!
