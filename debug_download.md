# Debugging Model Download Issues

## How to Debug the Download Error

### 1. View Logs in Real-time

**Option A: Android Studio Logcat (Recommended)**
1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Select your device/emulator
4. In the filter box, enter: `HuggingFaceDownloadManager`
5. Set log level to "Debug" or "Verbose"

**Option B: If you have Android SDK installed**
```bash
# Add Android SDK platform-tools to your PATH first:
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"

# Then run:
adb logcat -s HuggingFaceDownloadManager:* -v time
```

**Option C: Install Android SDK Platform Tools**
```bash
# Using Homebrew (macOS)
brew install android-platform-tools

# Then use:
adb logcat | grep -E "(SolMind|HuggingFace|ModelManager)"
```

### 2. Steps to Test Download
1. Open the SolMind app
2. Go to Settings
3. Scroll to "AI Settings" section
4. Tap "Select AI Model"
5. Choose a model (e.g., "FLAN-T5 Small")
6. Tap the download button
7. Watch the terminal for log messages

### 3. Common Issues to Look For

#### Network Issues:
- `java.net.UnknownHostException` - DNS resolution failed
- `java.net.ConnectException` - Cannot connect to server
- `javax.net.ssl.SSLException` - SSL/TLS certificate issues
- `java.net.SocketTimeoutException` - Request timeout

#### Permission Issues:
- `java.io.FileNotFoundException` - Cannot create/write files
- `SecurityException` - Missing permissions

#### API Issues:
- `HTTP 403` - Forbidden (may need authentication)
- `HTTP 404` - File not found
- `HTTP 429` - Rate limited
- `HTTP 500+` - Server errors

### 4. Expected Log Messages
When download works correctly, you should see:
```
D/HuggingFaceDownloadManager: Starting download for model: flan-t5-small
D/HuggingFaceDownloadManager: Download URL: https://huggingface.co/google/flan-t5-small/resolve/main/pytorch_model.bin
D/HuggingFaceDownloadManager: Making API call to download model file...
D/HuggingFaceDownloadManager: API response received - Code: 200, Message: OK
D/HuggingFaceDownloadManager: Content length: 307905733 bytes
V/HuggingFaceDownloadManager: Download progress: 1.2% (3686400/307905733)
...
D/HuggingFaceDownloadManager: Download completed successfully for model: flan-t5-small
```

### 5. Quick Fixes to Try

#### If you see SSL/Certificate errors:
Add this to AndroidManifest.xml in the `<application>` tag:
```xml
android:usesCleartextTraffic="true"
```

#### If you see permission errors:
Check that these permissions are in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

#### If you see timeout errors:
The timeouts have been increased to 120 seconds, but you can try downloading a smaller model first.

### 6. Alternative Test
You can test the download URL directly:
```bash
curl -I "https://huggingface.co/google/flan-t5-small/resolve/main/pytorch_model.bin"
```

This should return a 302 redirect to an AWS S3 URL.

### 7. Model Sizes
- FLAN-T5 Small: ~308 MB
- FLAN-T5 Base: ~990 MB  
- DistilBERT Base: ~268 MB

Start with the smallest model (FLAN-T5 Small) for testing.