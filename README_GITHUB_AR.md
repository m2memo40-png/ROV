# طريقة الرفع الصحيحة إلى GitHub

مهم: ارفع **محتويات هذا المجلد** إلى جذر Repository، وليس المجلد نفسه داخل مجلد آخر.

يجب أن يكون الشكل داخل GitHub هكذا:

```text
.github/
  workflows/
    build-apk.yml
app/
build.gradle
gradle.properties
settings.gradle
```

بعد الرفع:
1. افتح تبويب Actions.
2. اختر Build Android APK.
3. اضغط Run workflow.
4. بعد نجاح البناء، افتح Artifacts.
5. نزّل WiFiQuotaManager-debug-apk.

لا تحتاج إلى Android Studio أو Gradle مثبتًا على اللابتوب؛ GitHub Actions يثبت Gradle وAndroid SDK على الـrunner أثناء البناء.
