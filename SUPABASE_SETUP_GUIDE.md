# Supabase Setup Guide for MADADGAR Android App

## 🚀 What's Been Set Up

I've added the necessary Supabase dependencies and configuration files to your Android app:

### 1. Dependencies Added
- **Supabase Kotlin SDK** for database, auth, storage, and realtime
- **Ktor HTTP client** for network operations
- **Kotlinx Serialization** for JSON handling

### 2. Files Created
- `SupabaseClient.java` - Main configuration singleton
- `SupabaseAuthHelper.java` - Authentication helper class
- Updated `LoginActivity.java` - Integrated Supabase authentication

## 🔧 Next Steps to Complete Setup

### Step 1: Get Your Supabase Credentials

1. Go to [supabase.com](https://supabase.com) and create a new project
2. In your Supabase dashboard, go to **Settings → API**
3. Copy these values:
   - **Project URL** (looks like: `https://abcdefgh.supabase.co`)
   - **Anon/Public Key** (starts with `eyJ...`)

### Step 2: Update Supabase Configuration

Open `app/src/main/java/com/example/madadgarapp/config/SupabaseClient.java` and replace:

```java
private static final String SUPABASE_URL = "YOUR_SUPABASE_PROJECT_URL";
private static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";
```

With your actual credentials:

```java
private static final String SUPABASE_URL = "https://your-project.supabase.co";
private static final String SUPABASE_ANON_KEY = "eyJ...your-anon-key";
```

### Step 3: Create Database Tables

In your Supabase dashboard, go to **SQL Editor** and run these commands to create necessary tables:

```sql
-- Create users profile table
CREATE TABLE IF NOT EXISTS profiles (
    id UUID REFERENCES auth.users NOT NULL PRIMARY KEY,
    phone VARCHAR(20),
    full_name TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- Create items table for your marketplace
CREATE TABLE IF NOT EXISTS items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    category TEXT,
    condition TEXT,
    image_urls TEXT[],
    user_id UUID REFERENCES auth.users NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE items ENABLE ROW LEVEL SECURITY;

-- Create policies
CREATE POLICY "Users can view own profile" ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Anyone can view items" ON items FOR SELECT USING (true);
CREATE POLICY "Users can insert own items" ON items FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own items" ON items FOR UPDATE USING (auth.uid() = user_id);
```

### Step 4: Configure Authentication

1. In Supabase dashboard, go to **Authentication → Settings**
2. Configure your authentication providers (email/password is enabled by default)
3. **IMPORTANT**: Set up redirect URLs for magic links:
   - Go to **Authentication → URL Configuration**
   - Add `com.example.madadgarapp://magic-link` to the **Redirect URLs** list
   - Add `com.example.madadgarapp://facebook-callback` for Facebook OAuth
   - Add `com.example.madadgarapp://auth-callback` for general OAuth
4. In **Authentication → Providers**, ensure:
   - Email provider is enabled
   - "Confirm email" is enabled for security
   - "Enable email confirmations" is checked

### Step 5: Implement Real Authentication Logic

The current `SupabaseAuthHelper.java` has placeholder methods. You'll need to implement proper Kotlin coroutine handling. Here's an example approach:

#### Option A: Create Kotlin Bridge Classes
Create Kotlin files that handle coroutines and call them from Java.

#### Option B: Use RxJava/RxAndroid
Convert Kotlin coroutines to RxJava for better Java compatibility.

#### Option C: Use CompletableFuture
Wrap Supabase calls in CompletableFuture for async handling.

### Step 6: Test the Integration

1. Sync your project: **File → Sync Project with Gradle Files**
2. Build the project to ensure no compilation errors
3. Test login functionality with a test user

## 🔄 Usage Examples

### In your LoginActivity:
```java
// The login method is already integrated in your LoginActivity.java
// It will call Supabase authentication when user clicks login
```

### Creating a new user (for SignUpActivity):
```java
SupabaseAuthHelper authHelper = new SupabaseAuthHelper();
authHelper.signUp(email, password, new SupabaseAuthHelper.AuthCallback() {
    @Override
    public void onSuccess(String message) {
        // Handle success
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

### Database Operations:
```java
// You can access the database through:
SupabaseClient.getInstance().getDatabase();
// But you'll need to implement proper async handling
```

## ⚠️ Important Notes

1. **Security**: Store your Supabase credentials securely, preferably in environment variables or Android's `BuildConfig`
2. **Async Operations**: Supabase operations are asynchronous. The current implementation needs proper async handling
3. **Error Handling**: Implement comprehensive error handling for network issues
4. **Testing**: Test with real Supabase instance before production

## 🛠️ Troubleshooting

### Common Issues:

1. **Build Errors**: Make sure you've synced the project after adding dependencies
2. **Authentication Errors**: Verify your Supabase URL and keys are correct
3. **Network Issues**: Ensure your app has internet permission in AndroidManifest.xml

### Gradle Sync Issues:
If you encounter Gradle sync issues, try:
1. **File → Invalidate Caches and Restart**
2. Clean and rebuild project
3. Check that Kotlin plugin versions match

## 📚 Next Steps

1. Complete the authentication implementation with proper async handling
2. Implement database operations for your items/marketplace functionality
3. Add image upload functionality using Supabase Storage
4. Implement real-time features for chat or notifications

Would you like me to help you implement any specific part of this setup?

