# بناء APK من GitHub بدون Android Studio

1. ارفع محتويات مجلد `WiFiQuotaManager` إلى Repository على GitHub.
2. تأكد أن الملف `.github/workflows/build-apk.yml` موجود داخل المستودع.
3. افتح تبويب **Actions**.
4. اختر **Build Android APK**.
5. اضغط **Run workflow**.
6. بعد نجاح المهمة، افتح التشغيل الناجح ثم قسم **Artifacts**.
7. نزّل `WiFiQuotaManager-debug-apk` وستجد داخله ملف `app-debug.apk`.

لا تحتاج إلى تثبيت Android Studio أو Android SDK على اللابتوب. GitHub Actions يجهز Java وGradle وAndroid SDK تلقائيًا.

## مهم عند الرفع

إذا كان Repository نفسه يحتوي على ملفات المشروع مثل `settings.gradle` و`app/`، ارفع **محتويات** مجلد `WiFiQuotaManager` وليس المجلد كطبقة إضافية.

الـWorkflow يتعامل أيضًا مع الحالة التي يكون فيها المشروع داخل مجلد `WiFiQuotaManager` داخل الـRepository.
