
# Step-by-Step Guide: Finding Your Google Web Client ID

This guide will walk you through the exact steps to find the correct **Web Application Client ID** required for Google Sign-In to work in your Android app.

**The most common mistake is using the *Android* Client ID in your `strings.xml` file. You need the *Web* Client ID.**

---

### **Step 1: Open the Correct Google Cloud Console Page**

1.  Click on this direct link to open the credentials page for your project:
    [**https://console.cloud.google.com/apis/credentials?project=madadgar-app**](https://console.cloud.google.com/apis/credentials?project=madadgar-app)

2.  You will be asked to log in to your Google account if you haven't already. Make sure it's the account associated with your Firebase project.

---

### **Step 2: Identify the Correct Client ID**

Once the page loads, you will see a section titled **"OAuth 2.0 Client IDs"**. You need to look for two distinct entries.

It will look something like this:

| Name                 | Type              | Client ID                                                              |
| -------------------- | ----------------- | ---------------------------------------------------------------------- |
| Android client 1     | **Android**       | `763585977372-8r5539sv82f8vg3ic1e82nh80g4e52jl.apps.googleusercontent.com` |
| Web client 1         | **Web application** | `763585977372-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com` |

**Your Task:**

1.  Find the entry where the **Type** is **`Web application`**.
2.  This is the key you need. The `Client ID` in this row is your **Web Client ID**.
3.  Click the "copy" icon next to the **Web Application Client ID** to copy it to your clipboard.

**Critical Check:**
*   The **Android** Client ID is the one you *currently* have in your `strings.xml`. **DO NOT USE THIS ONE.**
*   The **Web application** Client ID will have a different random string of characters at the end. **THIS IS THE CORRECT ONE.**

---

### **Step 3: What to Do If You DON'T See a "Web application" Client**

If you only see the "Android" client, you must create a new Web client.

1.  At the top of the page, click **"+ CREATE CREDENTIALS"**.
2.  From the dropdown, select **"OAuth client ID"**.
3.  For **"Application type"**, choose **`Web application`**.
4.  Give it a **Name**, for example, "MADADGAR Web Client".
5.  **LEAVE THE "Authorized JavaScript origins" and "Authorized redirect URIs" SECTIONS COMPLETELY EMPTY.** This is very important.
6.  Click the **"CREATE"** button.
7.  A pop-up will appear showing your new **Client ID** and **Client Secret**. **Copy the `Client ID`**.

---

### **Step 4: Update Your `strings.xml` File**

Now that you have the correct **Web Application Client ID** on your clipboard:

1.  Go back to your Android Studio project.
2.  Open this file: `app/src/main/res/values/strings.xml`
3.  Find the line for `google_web_client_id` (around line 152).
4.  **Replace the existing value** with the new Web Client ID you just copied.

**It should look like this (with your new ID):**
```xml
<!-- Before -->
<string name="google_web_client_id">763585977372-8r5539sv82f8vg3ic1e82nh80g4e52jl.apps.googleusercontent.com</string>

<!-- After (Example) -->
<string name="google_web_client_id">763585977372-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6.apps.googleusercontent.com</string>
```

---

### **Final Step: Test the App**

1.  **Clean and rebuild** your project in Android Studio.
2.  **Run the app** on your device or emulator.
3.  Open **Logcat** in Android Studio and filter by `AuthSelection`.
4.  Click the "Continue with Google" button.

You should no longer see the `DEVELOPER_ERROR: 10`. The Google Sign-In screen should appear.

Follow these instructions carefully. The issue is almost certainly a mix-up between the Android and Web client IDs. Let me know how it goes!

