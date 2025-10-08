import os

# Paths to each language-specific strings.xml
# ...existing code...
paths = [
     r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-ar\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-de\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-en-rGB\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-en-rUS\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-es\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-es-rMX\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-es-rUS\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-fa\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-fr\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-fr-rCA\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-hi\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-in\strings.xml",     # Indonesian
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-it\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-ja\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-ko\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-nl\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-pt\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-pt-rBR\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-ru\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-tr\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-uk\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-vi\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-zh\strings.xml",
    r"C:\TripSoft\hv102b-android-pdf-ns2b\app\src\main\res\values-zh-rTW\strings.xml",
]
#

key = "agree_share_info"

# Corresponding translations
# ...existing code...
translations = {
    "values":       "By sending this email, you agree to share the above information with us to help improve the app.",
    "values-ar":    "من خلال إرسال هذا البريد الإلكتروني، فإنك توافق على مشاركة المعلومات أعلاه معنا للمساعدة في تحسين التطبيق.",
    "values-de":    "Indem Sie diese E-Mail senden, stimmen Sie zu, die oben genannten Informationen mit uns zu teilen, um die App zu verbessern.",
    "values-en-rGB":"By sending this email, you agree to share the above information with us to help improve the app.",
    "values-en-rUS":"By sending this email, you agree to share the above information with us to help improve the app.",
    "values-es":    "Al enviar este correo electrónico, aceptas compartir la información anterior con nosotros para ayudar a mejorar la aplicación.",
    "values-es-rMX":"Al enviar este correo electrónico, aceptas compartir la información anterior con nosotros para ayudar a mejorar la aplicación.",
    "values-es-rUS":"Al enviar este correo electrónico, aceptas compartir la información anterior con nosotros para ayudar a mejorar la aplicación.",
    "values-fa":    "با ارسال این ایمیل، شما موافقت می‌کنید اطلاعات فوق را با ما به اشتراک بگذارید تا به بهبود برنامه کمک کند.",
    "values-fr":    "En envoyant cet e-mail, vous acceptez de partager les informations ci-dessus avec nous pour aider à améliorer l'application.",
    "values-fr-rCA":"En envoyant ce courriel, vous acceptez de partager les informations ci-dessus avec nous pour aider à améliorer l'application.",
    "values-hi":    "इस ईमेल को भेजकर, आप ऐप को बेहतर बनाने में हमारी सहायता के लिए उपरोक्त जानकारी साझा करने के लिए सहमत होते हैं।",
    "values-in":    "Dengan mengirim email ini, Anda setuju untuk membagikan informasi di atas dengan kami untuk membantu meningkatkan aplikasi.",
    "values-it":    "Inviando questa email, accetti di condividere le informazioni di cui sopra con noi per contribuire a migliorare l'app.",
    "values-ja":    "このメールを送信することで、上記の情報を共有し、アプリの改善にご協力いただくことに同意したものとみなされます。",
    "values-ko":    "이 이메일을 보내시면 위 정보를 공유하여 앱 개선을 돕는 데 동의하게 됩니다.",
    "values-night": "By sending this email, you agree to share the above information with us to help improve the app.",
    "values-nl":    "Door deze e-mail te verzenden, stemt u ermee in de bovenstaande informatie met ons te delen om de app te verbeteren.",
    "values-pt":    "Ao enviar este e-mail, você concorda em compartilhar as informações acima conosco para ajudar a melhorar o aplicativo.",
    "values-pt-rBR":"Ao enviar este e-mail, você concorda em compartilhar as informações acima conosco para ajudar a melhorar o aplicativo.",
    "values-ru":    "Отправляя это письмо, вы соглашаетесь поделиться указанной выше информацией с нами, чтобы помочь улучшить приложение.",
    "values-tr":    "Bu e-postayı göndererek, uygulamayı iyileştirmemize yardımcı olmak için yukarıdaki bilgileri bizimle paylaşmayı kabul etmiş olursunuz.",
    "values-uk":    "Надсилаючи цей лист, ви погоджуєтеся поділитися наведеними вище даними з нами, щоб допомогти покращити додаток.",
    "values-vi":    "Bằng cách gửi email này, bạn đồng ý chia sẻ thông tin trên với chúng tôi để giúp cải thiện ứng dụng.",
    "values-zh":    "发送此电子邮件即表示您同意与我们共享上述信息，以帮助改进该应用。",
    "values-zh-rTW":"傳送此電子郵件即表示您同意與我們共享上述資訊，以協助改進此應用程"
}
for path in paths:
    try:
        with open(path, 'r', encoding='utf-8') as file:
            content = file.read()

        # fix existence check to actually substitute key
        if f'name="{key}"' in content:
            print(f"'{key}' already exists in: {path}")
            continue

        # extract the folder name, e.g. "values-ar" or "values"
        folder_name = os.path.basename(os.path.dirname(path))

        # lookup the exact translation
        translated_text = translations.get(folder_name)
        if not translated_text:
            print(f"Translation for '{folder_name}' not found, skipping: {path}")
            continue

        # Construct the new <string> and inject it
        new_string = f'    <string name="{key}">{translated_text}</string>\n'
        updated_content = content.replace("</resources>", new_string + "</resources>")

        with open(path, 'w', encoding='utf-8') as file:
            file.write(updated_content)

        print(f"Added '{key}' to: {path}")

    except Exception as e:
        print(f"Error processing {path}: {e}")