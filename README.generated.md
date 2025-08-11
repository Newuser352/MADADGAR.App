# MADADGARApp

A community sharing Android application for posting and browsing items (food and non-food), with automatic expiry for perishable items and push notifications for updates.


## Table of Contents
- Overview
- Key Features
- Tech Stack
- Architecture
- Project Structure
- Data Model (Overview)
- Notifications Flow
- Expiry System
- Location Features
- Getting Started
  - Prerequisites
  - Local Setup
  - Firebase Setup
  - Supabase Setup
  - Environment & Secrets
  - Build & Run
- Testing
- Troubleshooting
- Roadmap / Future Improvements
- Contributing
- License


## Overview
MADADGARApp enables users to share items with the community, particularly focusing on food and non-food categories. Users can list items with images/videos, browse active listings, view details, and receive notifications for new items or events. Perishable items automatically expire and are removed from public browsing after a scheduled time.


## Key Features
- Authentication
  - Supabase GoTrue (email/password, email verification links)
  - Google Sign-In via Android Credentials + Play Services
  - Deep links for email verification and OAuth callback (handled by MainActivity)
- Items & Media
  - Create item posts with image/video uploads to Supabase Storage
  - Browse active items; view item details; manage own posts
  - Category selection dialog (Food vs Non-Food with subcategories)
- Notifications
  - Firebase Cloud Messaging (FCM) for push notifications
  - Device token management, unread/all notifications list, deletion
  - Tapping a push opens specific items in-app (when possible)
- Auto-Expiry
  - Food items auto-expire via scheduled background service and are hidden from browsing once expired
- Location
  - Utilities for location access; client-side bounding-box item filtering for nearby items


## Tech Stack
- Languages: Kotlin + Java (mixed)
- Android: minSdk 23, target/compile SDK 35
- DI: Hilt (com.google.dagger:hilt-android)
- Backend SDK: Supabase (postgrest-kt, gotrue-kt, realtime-kt, storage-kt)
- Networking: Ktor client (required by Supabase SDK)
- JSON: Kotlinx Serialization
- Concurrency: Kotlin Coroutines
- UI: AndroidX Fragments, Lifecycle (ViewModel, LiveData), Material Components, ConstraintLayout, ViewBinding
- Media: Glide
- Background: WorkManager, Service for expiry
- Notifications: Firebase Cloud Messaging
- Tests: JUnit, AndroidX test, Espresso; fragment-testing in debug
- Build: Gradle (Kotlin DSL), Java 17, core library desugaring

See app/build.gradle.kts for the complete dependency set and versions.


## Architecture
- Application Layer
  - MADADGARApplication (@HiltAndroidApp)
    - Initializes SupabaseClient
    - Sets up Notification channels
    - Schedules FoodExpiry job via FoodExpiryScheduler
- UI Layer
  - Single-activity pattern with MainActivity hosting fragments via BottomNavigation
    - ItemsFragment (default dashboard)
    - ShareItemFragment (create item)
    - MyPostsFragment (user’s own items)
    - NotificationsFragment
    - AccountFragment
  - Reselection of Items tab shows a category dialog (Food vs Non-Food subcategories)
  - AuthManager is observed to redirect unauthenticated users to AuthSelectionActivity
- Data Layer
  - Repositories encapsulate Supabase operations and storage uploads
    - ItemRepository: CRUD (soft delete), media uploads, client-side filtering and paging
    - NotificationRepository: unread/all fetch, delete, device token updates with duplicate-key resilience, markRead placeholder
- Services & Workers
  - FCMService: Receives FCM messages and token refresh events
  - NotificationService: Builds and shows notifications
  - FoodItemExpiryService + FoodExpiryScheduler: Periodic cleanup of expired items
- Utilities
  - SupabaseClient: Central SDK initialization/config
  - AuthManager: Auth state (LiveData) + sign-out
  - NotificationManager, FCMTokenManager, LocationUtils, MediaUtils, ThemeUtils, TimeUtils, FavoriteManager


## Project Structure
- app/src/main/java/com/example/madadgarapp/
  - MADADGARApplication.kt
  - MainActivity.java
  - SplashActivity.java, LoginActivity.java, SignUpActivity.java, AuthSelectionActivity.java, EmailAuthSelectionActivity.java
  - WebViewOAuthActivity.java, OAuthCallbackActivity.java
  - activities/
    - ItemDetailActivity.java, FullScreenImageActivity.java
  - fragments/
    - ItemsFragment.java, ShareItemFragment.java, MyPostsFragment.java, NotificationsFragment.kt, AccountFragment.java, SavedPostsFragment.java, CategoriesFragment.java, LocationPickerFragment.kt
  - dialogs/
    - CategoryDialogFragment.java
  - adapters/
    - ItemAdapter.java, MediaAdapter.java, MyPostsAdapter.java, NotificationAdapter.java
  - repository/
    - ItemRepository.kt, NotificationRepository.kt, ItemRepositoryBridge.java, SupabaseItemBridge.kt
  - models/
    - SupabaseItem.kt, Item.java, SupabaseNotification.kt, NotificationModels.kt
  - services/
    - FCMService.kt, NotificationService.kt, FoodItemExpiryService.java
  - utils/
    - SupabaseClient.kt, AuthManager.kt, NotificationManager.kt, FCMTokenManager.kt, LocationUtils.kt, MediaUtils.java, ThemeUtils.java, TimeUtils.java, FavoriteManager.java, FoodExpiryScheduler.java, FoodExpiryTester.java
- app/src/main/res/layout/
  - Activity, fragment, item cell, and dialog XMLs (e.g., dialog_categories.xml)
- app/src/main/AndroidManifest.xml
- supabase_edge_function_send_push_notifications.ts (example Edge Function)


## Data Model (Overview)
- Item (SupabaseItem / Item)
  - id, title, description
  - mainCategory (Food/Non-Food), subCategory
  - location (address), latitude, longitude
  - contactNumber, contact1, contact2
  - ownerId
  - expiresAt
  - imageUrls (List<String>), videoUrl
  - isActive (soft delete/expiry control)
  - createdAt
- Notifications
  - UserNotification: id, userId, title, body, data payload, isRead, createdAt
  - Device token tables: userId, deviceToken, platform, isActive

Note: Some queries currently fetch-all then filter on client due to RLS/policy constraints. See Roadmap.


## Notifications Flow
1) App starts; FCM token is obtained/updated
   - FCMTokenManager updates Supabase (NotificationRepository.updateDeviceToken)
2) Server (Supabase Edge Function or backend) sends push via FCM
3) FCMService receives message; NotificationService builds an OS notification
4) User taps notification → MainActivity receives intent with extras
   - opened_from_notification=true
   - action=open_item (optional)
   - target_item_id=<id> (optional)
5) MainActivity navigates to Items and calls itemsFragment.openItemById(id) when available


## Expiry System
- FoodExpiryScheduler schedules periodic checks (e.g., via WorkManager/JobScheduler)
- FoodItemExpiryService runs and marks expired items as inactive (is_active=false)
- UI queries filter to active items, so expired ones no longer appear


## Location Features
- Permissions: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
- Items can be filtered client-side within a bounding box (min/max lat/lng)
- LocationPickerFragment helps choose a location; LocationUtils provides helpers


## Getting Started
### Prerequisites
- Android Studio (latest)
- Android SDK 35
- JDK 17
- A Firebase project (for FCM + Auth)
- A Supabase project (Postgrest, GoTrue, Storage)

### Local Setup
1) Open the project in Android Studio
2) Sync Gradle
3) Ensure you are using JDK 17 and compile SDK 35

### Firebase Setup
1) In Firebase Console, create a project and add an Android app with package name `com.example.madadgarapp`
2) Download google-services.json and place it under `app/`
3) Enable Firebase Cloud Messaging and (optionally) Firebase Auth providers you need

### Supabase Setup
1) Create a Supabase project and obtain:
   - Supabase URL
   - anon/public key (for client-side)
2) Configure tables:
   - items: holds item listings (columns corresponding to the model above)
   - user_notifications: holds notifications for users
   - user_device_tokens: stores device tokens per user
3) Storage buckets:
   - item-images (public read)
   - item-videos (public read)
4) RLS Policies:
   - Allow read for public active items
   - Allow write for authenticated users to their own items
   - Allow device token upsert for authenticated users
   - Add policies for notifications (read by userId, write by server, etc.)

### Environment & Secrets
- Do NOT hardcode secrets in source control
- Recommended approaches:
  - Use BuildConfig fields or local properties to supply Supabase URL and key
  - Store runtime tokens in EncryptedSharedPreferences (androidx.security:security-crypto)

### Build & Run
- Build variants: debug (development), release (minify + shrink)
- Run on device/emulator (API 23+)
- Grant runtime permissions when prompted (location, media, notifications)


## Testing
- Unit tests: `app/src/test` (ExampleUnitTest)
- Instrumented tests: `app/src/androidTest` (e.g., ExampleInstrumentedTest, MainActivityTest)
- Debug-only UI tests: fragment-testing dependency enabled in debug


## Troubleshooting
- Deep links for Supabase
  - Ensure intent-filters in AndroidManifest include:
    - https://crsqhxztqbfguylrgcnt.supabase.co/auth/v1/verify
    - https://crsqhxztqbfguylrgcnt.supabase.co/auth/v1/callback
- No notifications arriving
  - Confirm google-services.json is correct and project is properly linked
  - Make sure FCM is enabled in Firebase console
  - Verify device token is being stored in Supabase; check NotificationRepository logs
- Media upload fails
  - Confirm Storage buckets exist and are public-read
  - Check SupabaseClient initialization and network logs
- Items not appearing
  - Ensure `isActive` is true on items you expect to see
  - Check client-side filtering and timezones for `expiresAt`


## Roadmap / Future Improvements
- Move client-side filters and paging to Postgrest with server-side filtering (RLS-friendly)
- Implement markRead and removeDeviceToken with proper Postgrest filters
- Wire category selection to actual item queries for filtering
- Add robust empty/error/loading UI states
- Introduce proper pagination (limit/offset) and caching
- Strengthen input validation and form UX in ShareItemFragment


## Contributing
- Prefer Kotlin for new code where possible
- Use Hilt for dependency injection
- Keep repositories suspending and UI-agnostic
- Add tests for repository logic and critical UI flows
- Run lint/format before submitting PRs


## License
- Add your license of choice (e.g., MIT, Apache-2.0) here.

