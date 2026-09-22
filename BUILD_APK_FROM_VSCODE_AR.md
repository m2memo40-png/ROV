# بناء APK بدون Android Studio

## أسهل طريقة
1. فك ضغط المشروع.
2. أنشئ مستودع GitHub جديد.
3. ارفع مجلد `WiFiQuotaManager` كاملًا إلى المستودع.
4. بعد الرفع افتح تبويب **Actions**.
5. ستجد Workflow باسم **Build Android APK**.
6. شغّله يدويًا من **Run workflow**، أو اعمل Push على فرع `main` وسيبدأ تلقائيًا.
7. بعد نجاح البناء افتح الـ workflow الناجح ثم قسم **Artifacts**.
8. نزّل `WiFiQuotaManager-debug-apk` وستجد بداخله `app-debug.apk`.

## لو ستستخدم VS Code + Git
افتح Terminal داخل مجلد المشروع:

```powershell
git init
git branch -M main
git add .
git commit -m "Initial WiFi Quota Manager"
git remote add origin YOUR_GITHUB_REPO_URL
git push -u origin main
```

استبدل `YOUR_GITHUB_REPO_URL` برابط المستودع الذي أنشأته في GitHub.

> لا تحتاج Android Studio لبناء الـAPK بهذه الطريقة؛ البناء يتم داخل GitHub Actions باستخدام JDK 17 وGradle 9.6.1 وAndroid SDK 36.


## إصلاح خطأ Gradle Wrapper

لا تحتاج إلى `gradlew` في هذه النسخة. ملف GitHub Actions يستخدم `gradle/actions/setup-gradle@v6` لتنزيل Gradle 9.6.1 على خادم GitHub ثم يبني التطبيق مباشرة.

تأكد أن ملف `.github/workflows/build-apk.yml` موجود في المستودع، ثم افتح Actions وشغّل `Build Android APK`.
