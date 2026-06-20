# گرفتن APK آنلاین برای اپ ماه‌چین

بهترین روش ساده برای گرفتن APK بدون نصب Android Studio، استفاده از GitHub Actions است.

## روش پیشنهادی: GitHub Actions

1. در سایت GitHub یک Repository جدید بسازید.
2. محتوای پوشه `MahChin` را داخل Repository آپلود کنید. دقت کنید فایل `settings.gradle.kts` باید در ریشه Repository باشد، نه داخل یک پوشه اضافه.
3. وارد تب **Actions** شوید.
4. Workflow با نام **Build MahChin APK** را انتخاب کنید.
5. روی **Run workflow** بزنید.
6. بعد از تمام شدن Build، در همان صفحه پایین بخش **Artifacts** فایل `MahChin-debug-apk` را دانلود کنید.
7. ZIP دانلودشده را باز کنید. داخل آن فایل APK قرار دارد.

## نکته نصب روی گوشی

فایل ساخته‌شده نسخه Debug است و برای تست و نصب دستی مناسب است. برای انتشار رسمی در بازار، مایکت یا Google Play باید Release APK/AAB با امضای اختصاصی ساخته شود.

## اگر Build خطا داد

در صفحه Actions روی Build ناموفق کلیک کنید و متن خطا را کپی کنید. معمولاً خطاها مربوط به نسخه Gradle، SDK یا یک import ساده در Kotlin هستند و قابل رفع‌اند.
