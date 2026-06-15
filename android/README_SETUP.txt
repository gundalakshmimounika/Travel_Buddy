========================================================================
TRAVEL BUDDY - ANDROID FRONTEND SETUP GUIDE
========================================================================

Thank you for using Travel Buddy! This folder contains the complete, ready-to-run Android Studio project for the Travel Buddy app. All build caches and local properties have been cleaned to ensure it imports cleanly on any laptop.

------------------------------------------------------------------------
1. IMPORTING IN ANDROID STUDIO
------------------------------------------------------------------------
1. Open Android Studio.
2. Select "File" -> "New" -> "Import Project..." (or click "Open" on the Welcome Screen).
3. Navigate to the extracted folder: "TravelBuddy" and click "OK".
4. Android Studio will automatically configure the project, download the necessary Gradle version, and index the codebase. This may take 2-5 minutes depending on your internet connection.

------------------------------------------------------------------------
2. API CONFIGURATION & BACKEND CONNECTIVITY
------------------------------------------------------------------------
To connect the app to your backend:
1. Open "app/src/main/java/com/simats/travelbuddy/RetrofitClient.kt".
2. Locate the "BASE_URL" variable:
   - For local testing (XAMPP on the same Wi-Fi network): Set it to your laptop's local IP address (e.g., "http://192.168.1.100/TravelBuddybackend/").
   - For emulator testing: Use "http://10.0.2.2/TravelBuddybackend/" to point to your local localhost.
   - For live server: Set it to your domain name (e.g., "https://api.yourdomain.com/").

------------------------------------------------------------------------
3. RAZORPAY INTEGRATION (PAYMENTS)
------------------------------------------------------------------------
The Razorpay Payment SDK is fully integrated and tested.
1. The API key is set in "app/src/main/AndroidManifest.xml":
   <meta-data
       android:name="com.razorpay.ApiKey"
       android:value="rzp_test_SqOZwDnHPrqJm0" />
2. If you need to change the key for production/Play Store launch, simply replace this value with your live Razorpay Key ID.

------------------------------------------------------------------------
4. RELEASING TO PLAY STORE (BUILD APK/BUNDLE)
------------------------------------------------------------------------
To generate a release build for the Google Play Store:
1. In Android Studio, go to "Build" -> "Generate Signed Bundle / APK...".
2. Choose "Android App Bundle" (required for Play Store uploads) or "APK" (for direct sharing/testing).
3. Click "Next", select or create your KeyStore keystore file, fill in the alias and password, and click "Next".
4. Choose "release" build variant and click "Create".
5. The generated .aab file will be located in "app/release/".

Have a wonderful travel with Travel Buddy! ðŸ›«ðŸŒ´
