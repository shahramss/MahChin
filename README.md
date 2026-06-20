# ماه‌چین - MahChin

اپلیکیشن اندرویدی فارسی و راست‌چین برای برنامه‌ریزی ماهانه و پیگیری روزانه.

## امکانات نسخه ۱

- Kotlin + Jetpack Compose
- MVVM ساده
- Room Database آفلاین
- WorkManager برای یادآوری دوره‌ای
- تقویم شمسی داخلی بدون وابستگی خارجی
- پشتیبانی سال‌های ۱۴۰۵ تا ۱۵۰۰
- قالب ماهانه تکرارشونده
- تسک اختصاصی برای تاریخ خاص
- ساخت خودکار برنامه امروز از قالب ماهانه
- انتقال تسک‌های روز ۳۱ به آخرین روز ماه‌های کوتاه‌تر
- انتقال روز ۳۰ اسفند به روز ۲۹ در سال غیرکبیسه
- وضعیت‌ها: انجام نشده، در حال انجام، انجام شد، موکول به فردا، موکول به تاریخ دلخواه، لغو شد
- اولویت‌ها: عادی، مهم، فوری
- نوتیفیکیشن با دکمه‌های نمایش برنامه، یادآوری ۱ ساعت دیگر، انتقال باقی‌مانده‌ها به فردا
- تنظیم شدت یادآوری: آرام، معمولی، جدی، خیلی جدی
- تنظیم ساعت شروع و پایان یادآوری
- حالت تاریک
- بکاپ ساده دیتابیس از طریق Android Auto Backup

## ساختار پروژه

```text
MahChin/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/mahchin/app/
        │   ├── MainActivity.kt
        │   ├── MahChinApplication.kt
        │   ├── data/
        │   │   ├── dao/TaskDao.kt
        │   │   ├── db/AppDatabase.kt
        │   │   ├── db/Converters.kt
        │   │   ├── model/*.kt
        │   │   └── repository/TaskRepository.kt
        │   ├── domain/JalaliCalendar.kt
        │   ├── domain/JalaliDate.kt
        │   ├── notification/*.kt
        │   └── ui/
        │       ├── components/*.kt
        │       ├── navigation/AppNav.kt
        │       ├── screens/*.kt
        │       ├── theme/Theme.kt
        │       └── viewmodel/MainViewModel.kt
        └── res/
            ├── drawable/ic_launcher.xml
            ├── values/styles.xml
            └── xml/*.xml
```

## معماری

- `MainActivity`: راه‌اندازی Compose، RTL، درخواست مجوز نوتیفیکیشن.
- `MainViewModel`: وضعیت UI و عملیات کاربر.
- `TaskRepository`: منطق اصلی برنامه، ساخت تسک از قالب، موکول کردن، گزارش.
- `Room`: چهار موجودیت اصلی `MonthlyTemplateTask`, `DailyTaskInstance`, `OneTimeTask`, `UserSettings`.
- `JalaliCalendar`: تبدیل تاریخ، طول ماه، کبیسه، انتقال روزهای ۳۰ و ۳۱.
- `ReminderWorker`: بررسی تسک‌های باز امروز و ارسال نوتیفیکیشن.
- `ReminderActionReceiver`: عملیات دکمه‌های نوتیفیکیشن.

## اجرای پروژه در Android Studio

1. Android Studio جدید نصب کن.
2. از منوی `File > Open` پوشه `MahChin` را باز کن.
3. اگر Android Studio خواست SDK 36 یا JDK 21 را نصب کند، اجازه بده نصب کند.
4. صبر کن Gradle Sync تمام شود.
5. گوشی یا Emulator را وصل کن.
6. دکمه Run را بزن.

## گرفتن APK نصب‌شدنی

### روش گرافیکی

1. در Android Studio برو به:
   `Build > Build Bundle(s) / APK(s) > Build APK(s)`
2. بعد از پایان بیلد، روی `locate` بزن.
3. فایل APK معمولاً اینجاست:
   `app/build/outputs/apk/debug/app-debug.apk`

### روش ترمینال

اگر Gradle روی سیستم نصب است:

```bash
gradle :app:assembleDebug
```

اگر خواستی Gradle Wrapper استاندارد بسازی:

```bash
gradle wrapper --gradle-version 9.5.1
./gradlew :app:assembleDebug
```

در ویندوز:

```powershell
.\gradlew.bat :app:assembleDebug
```

## نکته درباره یادآوری اندروید

در Android 13 به بعد، کاربر باید مجوز Notification را بدهد. اپ در شروع این مجوز را درخواست می‌کند. یادآوری با WorkManager انجام می‌شود و با نسخه‌های جدید اندروید سازگارتر از باز کردن اجباری تمام‌صفحه است.

## محدودیت‌های نسخه اول

- ویرایش تسک ثابت ماهانه در نسخه اول به دو حالت عملی پیاده‌سازی شده: «همه ماه‌ها» برای قالب و «فقط همین تاریخ» برای نمونه ساخته‌شده روزانه.
- بکاپ دستی JSON در نسخه اول اضافه نشده؛ بکاپ ساده دیتابیس با Auto Backup اندروید فعال است.
- APK در محیطی که Android SDK و Gradle ندارد ساخته نمی‌شود، اما پروژه برای Android Studio کامل است.
