package com.example.vedaahar.i18n

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

private const val LanguagePrefsName = "vedaahaar_i18n"
private const val LanguageCodeKey = "language_code"

data class AppLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String,
    val voiceLocale: String,
    val indicCode: String
)

val supportedAppLanguages = listOf(
    AppLanguage("en", "English", "English", "en-IN", "eng_Latn"),
    AppLanguage("as", "Assamese", "অসমীয়া", "as-IN", "asm_Beng"),
    AppLanguage("bn", "Bengali", "বাংলা", "bn-IN", "ben_Beng"),
    AppLanguage("brx", "Bodo", "बड़ो", "brx-IN", "brx_Deva"),
    AppLanguage("doi", "Dogri", "डोगरी", "doi-IN", "doi_Deva"),
    AppLanguage("gu", "Gujarati", "ગુજરાતી", "gu-IN", "guj_Gujr"),
    AppLanguage("hi", "Hindi", "हिंदी", "hi-IN", "hin_Deva"),
    AppLanguage("kn", "Kannada", "ಕನ್ನಡ", "kn-IN", "kan_Knda"),
    AppLanguage("ks", "Kashmiri", "کٲشُر", "ks-IN", "kas_Arab"),
    AppLanguage("gom", "Konkani", "कोंकणी", "kok-Deva-IN", "gom_Deva"),
    AppLanguage("mai", "Maithili", "मैथिली", "mai-IN", "mai_Deva"),
    AppLanguage("ml", "Malayalam", "മലയാളം", "ml-IN", "mal_Mlym"),
    AppLanguage("mni", "Manipuri", "মৈতৈলোন্", "mni-IN", "mni_Beng"),
    AppLanguage("mr", "Marathi", "मराठी", "mr-IN", "mar_Deva"),
    AppLanguage("ne", "Nepali", "नेपाली", "ne-IN", "npi_Deva"),
    AppLanguage("or", "Odia", "ଓଡ଼ିଆ", "or-IN", "ory_Orya"),
    AppLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", "pa-IN", "pan_Guru"),
    AppLanguage("sa", "Sanskrit", "संस्कृतम्", "sa-IN", "san_Deva"),
    AppLanguage("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ", "sat-IN", "sat_Olck"),
    AppLanguage("sd", "Sindhi", "سنڌي", "sd-IN", "snd_Arab"),
    AppLanguage("ta", "Tamil", "தமிழ்", "ta-IN", "tam_Taml"),
    AppLanguage("te", "Telugu", "తెలుగు", "te-IN", "tel_Telu"),
    AppLanguage("ur", "Urdu", "اردو", "ur-IN", "urd_Arab")
)

fun nativeLanguageName(language: AppLanguage): String = language.nativeLabel
fun languageByCode(code: String): AppLanguage = supportedAppLanguages.firstOrNull { it.code == code } ?: supportedAppLanguages.first()

data class AppLanguageState(
    val language: AppLanguage,
    val hasSelectedLanguage: Boolean,
    val selectedLanguage: String,
    val voiceLocale: String,
    val indicLanguageCode: String,
    val setLanguage: (AppLanguage) -> Unit
)

val LocalAppLanguageState = compositionLocalOf {
    val fallback = supportedAppLanguages.first()
    AppLanguageState(fallback, false, fallback.code, fallback.voiceLocale, fallback.indicCode) {}
}

@Composable
fun AppLanguageProvider(
    context: Context,
    content: @Composable () -> Unit
) {
    val preferences = remember {
        context.getSharedPreferences(LanguagePrefsName, Context.MODE_PRIVATE)
    }
    var language by remember {
        val saved = preferences.getString(LanguageCodeKey, null)
        mutableStateOf(saved?.let(::languageByCode) ?: supportedAppLanguages.first())
    }
    var hasSelectedLanguage by remember {
        mutableStateOf(preferences.contains(LanguageCodeKey))
    }
    val state = remember(language, hasSelectedLanguage) {
        AppLanguageState(
            language = language,
            hasSelectedLanguage = hasSelectedLanguage,
            selectedLanguage = language.code,
            voiceLocale = language.voiceLocale,
            indicLanguageCode = language.indicCode
        ) { next ->
            preferences.edit().putString(LanguageCodeKey, next.code).apply()
            language = next
            hasSelectedLanguage = true
        }
    }

    CompositionLocalProvider(LocalAppLanguageState provides state) {
        content()
    }
}

@Composable
fun LanguageSelector(modifier: Modifier = Modifier) {
    val languageState = LocalAppLanguageState.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            MaterialText(nativeLanguageName(languageState.language))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedAppLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { MaterialText(nativeLanguageName(language)) },
                    onClick = {
                        languageState.setLanguage(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = translate(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun translate(text: String): String {
    val code = LocalAppLanguageState.current.language.code
    return translateText(code, text)
}

fun translateText(languageCode: String, text: String): String {
    if (languageCode == "en" || text.isBlank()) return text
    return AppTranslations.translate(languageCode, text)
}

private object AppTranslations {
    fun translate(languageCode: String, source: String): String {
        exact(languageCode, source)?.let { return it }
        val translated = phraseTranslate(languageCode, source)
        return if (translated.containsLatinLetters()) transliterateFallback(languageCode, translated) else translated
    }

    private fun exact(languageCode: String, source: String): String? {
        val common = commonExact[languageCode].orEmpty()[source]
        if (common != null) return common
        val base = baseExact[languageCode].orEmpty()[source]
        if (base != null) return base
        return null
    }

    private fun phraseTranslate(languageCode: String, source: String): String {
        var translated = source
        val map = commonPhrase[languageCode].orEmpty() + sharedPhrase[languageCode].orEmpty()
        map.entries.sortedByDescending { it.key.length }.forEach { entry ->
            translated = translated.replace(entry.key, entry.value, ignoreCase = false)
        }
        return translated
    }

    private val commonExact = mapOf(
        "hi" to words("भाषा", "वापस", "लॉगिन", "लॉगआउट", "रद्द करें", "आगे", "पिछला", "जारी रखें", "सहेजें", "हाँ", "नहीं", "वात", "पित्त", "कफ"),
        "gu" to words("ભાષા", "પાછળ", "લોગિન", "લોગઆઉટ", "રદ કરો", "આગળ", "પાછલું", "ચાલુ રાખો", "સાચવો", "હા", "ના", "વાત", "પિત્ત", "કફ"),
        "mr" to words("भाषा", "मागे", "लॉगिन", "लॉगआउट", "रद्द करा", "पुढे", "मागील", "सुरू ठेवा", "जतन करा", "होय", "नाही", "वात", "पित्त", "कफ"),
        "ta" to words("மொழி", "பின்", "உள்நுழை", "வெளியேறு", "ரத்து", "அடுத்து", "முந்தைய", "தொடரவும்", "சேமி", "ஆம்", "இல்லை", "வாதம்", "பித்தம்", "கபம்"),
        "te" to words("భాష", "వెనుకకు", "లాగిన్", "లాగౌట్", "రద్దు", "తర్వాత", "మునుపటి", "కొనసాగించు", "సేవ్", "అవును", "లేదు", "వాత", "పిత్త", "కఫ"),
        "bn" to words("ভাষা", "ফিরুন", "লগইন", "লগআউট", "বাতিল", "পরবর্তী", "পূর্ববর্তী", "চালিয়ে যান", "সংরক্ষণ", "হ্যাঁ", "না", "বাত", "পিত্ত", "কফ"),
        "kn" to words("ಭಾಷೆ", "ಹಿಂದೆ", "ಲಾಗಿನ್", "ಲಾಗೌಟ್", "ರದ್ದು", "ಮುಂದೆ", "ಹಿಂದಿನ", "ಮುಂದುವರಿಸಿ", "ಉಳಿಸಿ", "ಹೌದು", "ಇಲ್ಲ", "ವಾತ", "ಪಿತ್ತ", "ಕಫ"),
        "ml" to words("ഭാഷ", "പിന്നോട്ട്", "ലോഗിൻ", "ലോഗൗട്ട്", "റദ്ദാക്കുക", "അടുത്തത്", "മുമ്പത്തെ", "തുടരുക", "സേവ്", "അതെ", "ഇല്ല", "വാത", "പിത്ത", "കഫ"),
        "pa" to words("ਭਾਸ਼ਾ", "ਵਾਪਸ", "ਲਾਗਇਨ", "ਲਾਗਆਉਟ", "ਰੱਦ ਕਰੋ", "ਅੱਗੇ", "ਪਿਛਲਾ", "ਜਾਰੀ ਰੱਖੋ", "ਸੇਵ ਕਰੋ", "ਹਾਂ", "ਨਹੀਂ", "ਵਾਤ", "ਪਿੱਤ", "ਕਫ"),
        "as" to words("ভাষা", "পিছলৈ", "লগইন", "লগআউট", "বাতিল", "পৰৱৰ্তী", "পূৰ্বৱৰ্তী", "অব্যাহত ৰাখক", "সংৰক্ষণ", "হয়", "নহয়", "বাত", "পিত্ত", "কফ"),
        "brx" to words("राव", "उनथिं", "लगइन", "लगआउट", "बातिल", "उनाव", "सिगां", "सोलाय लां", "थिना", "नंगौ", "नङा", "वात", "पित्त", "कफ"),
        "doi" to words("भाशा", "पिच्छे", "लॉगिन", "लॉगआउट", "रद्द", "अग्गे", "पिच्छला", "जारी रखो", "सेव", "हाँ", "नेई", "वात", "पित्त", "कफ"),
        "ks" to words("زبان", "واپس", "لاگ اِن", "لاگ آؤٹ", "منسوخ", "اَگُر", "پَتہ", "جاری", "محفوظ", "آ", "نہ", "وات", "پت", "کف"),
        "gom" to words("भास", "फाटीं", "लॉगिन", "लॉगआउट", "रद्द", "फुडलें", "आदले", "चालू दवरात", "सांबाळ", "हय", "ना", "वात", "पित्त", "कफ"),
        "mai" to words("भाषा", "पाछाँ", "लॉगिन", "लॉगआउट", "रद्द", "आगू", "पाछू", "जारी राखू", "सहेजू", "हँ", "नहि", "वात", "पित्त", "कफ"),
        "mni" to words("লোন", "মতুং", "লগইন", "লগআউট", "ককথৎ", "মথং", "মমাং", "মখা চৎথবা", "সেভ", "হোয়", "নত্তে", "বাত", "পিত্ত", "কফ"),
        "ne" to words("भाषा", "फिर्ता", "लगइन", "लगआउट", "रद्द", "अर्को", "अघिल्लो", "जारी राख्नुहोस्", "सुरक्षित", "हो", "होइन", "वात", "पित्त", "कफ"),
        "or" to words("ଭାଷା", "ପଛକୁ", "ଲଗଇନ", "ଲଗଆଉଟ", "ବାତିଲ", "ପରବର୍ତ୍ତୀ", "ପୂର୍ବବର୍ତ୍ତୀ", "ଜାରି ରଖନ୍ତୁ", "ସଞ୍ଚୟ", "ହଁ", "ନା", "ବାତ", "ପିତ୍ତ", "କଫ"),
        "sa" to words("भाषा", "प्रतिगच्छ", "प्रवेशः", "निर्गमनम्", "निरसनम्", "अग्रिमम्", "पूर्वम्", "अनुवर्तस्व", "रक्ष", "आम्", "न", "वात", "पित्त", "कफ"),
        "sat" to words("ᱯᱟᱹᱨᱥᱤ", "ᱛᱟᱭᱚᱢ", "ᱞᱚᱜᱤᱱ", "ᱞᱚᱜᱟᱣᱩᱴ", "ᱵᱟᱛᱤᱞ", "ᱤᱱᱟᱹ", "ᱢᱟᱲᱟᱝ", "ᱞᱟᱦᱟ", "ᱥᱟᱧᱪᱟᱣ", "ᱦᱮᱸ", "ᱵᱟᱝ", "ᱣᱟᱛ", "ᱯᱤᱛ", "ᱠᱟᱯᱷ"),
        "sd" to words("ٻولي", "واپس", "لاگ اِن", "لاگ آئوٽ", "رد", "اڳيون", "پويون", "جاري رکو", "محفوظ", "ها", "نه", "وات", "پت", "ڪف"),
        "ur" to words("زبان", "واپس", "لاگ اِن", "لاگ آؤٹ", "منسوخ", "اگلا", "پچھلا", "جاری رکھیں", "محفوظ", "ہاں", "نہیں", "وات", "پت", "کف")
    )

    private fun words(language: String, back: String, login: String, logout: String, cancel: String, next: String, previous: String, cont: String, save: String, yes: String, no: String, vata: String, pitta: String, kapha: String) = mapOf(
        "Language" to language,
        "Back" to back,
        "Dashboard" to label(language, "Dashboard"),
        "Login" to login,
        "Logout" to logout,
        "Cancel" to cancel,
        "Next" to next,
        "Previous" to previous,
        "Continue" to cont,
        "Save" to save,
        "Create Account" to label(language, "Create Account"),
        "Home" to label(language, "Home"),
        "Wellness" to label(language, "Wellness"),
        "Shopping" to label(language, "Shopping"),
        "Consult" to label(language, "Consult"),
        "Profile" to label(language, "Profile"),
        "Yes" to yes,
        "No" to no,
        "Vata" to vata,
        "Pitta" to pitta,
        "Kapha" to kapha,
        "Select Your Language" to label(language, "Select Your Language"),
        "Choose your preferred language to continue" to label(language, "Choose your preferred language to continue")
    )

    private val baseExact = mapOf(
        "hi" to screenLabels("डैशबोर्ड", "प्रोफाइल", "सेटिंग्स", "दस्तावेज़ अपलोड", "मेडिकल रिकॉर्ड", "व्यक्तिगत आहार", "लक्षण विश्लेषण", "प्रकृति मूल्यांकन", "विकृति मूल्यांकन", "अग्नि मूल्यांकन"),
        "gu" to screenLabels("ડેશબોર્ડ", "પ્રોફાઇલ", "સેટિંગ્સ", "દસ્તાવેજ અપલોડ", "મેડિકલ રેકોર્ડ", "વ્યક્તિગત આહાર", "લક્ષણ વિશ્લેષણ", "પ્રકૃતિ મૂલ્યાંકન", "વિકૃતિ મૂલ્યાંકન", "અગ્નિ મૂલ્યાંકન"),
        "mr" to screenLabels("डॅशबोर्ड", "प्रोफाइल", "सेटिंग्स", "दस्तऐवज अपलोड", "वैद्यकीय नोंदी", "वैयक्तिक आहार", "लक्षण विश्लेषण", "प्रकृती मूल्यांकन", "विकृती मूल्यांकन", "अग्नी मूल्यांकन"),
        "ta" to screenLabels("டாஷ்போர்டு", "சுயவிவரம்", "அமைப்புகள்", "ஆவணம் பதிவேற்றம்", "மருத்துவ பதிவுகள்", "தனிப்பயன் உணவு", "அறிகுறி பகுப்பாய்வு", "பிரகிருதி மதிப்பீடு", "விகிருதி மதிப்பீடு", "அக்னி மதிப்பீடு"),
        "te" to screenLabels("డ్యాష్‌బోర్డ్", "ప్రొఫైల్", "సెట్టింగ్స్", "పత్రం అప్‌లోడ్", "మెడికల్ రికార్డులు", "వ్యక్తిగత ఆహారం", "లక్షణ విశ్లేషణ", "ప్రకృతి అంచనా", "వికృతి అంచనా", "అగ్ని అంచనా"),
        "bn" to screenLabels("ড্যাশবোর্ড", "প্রোফাইল", "সেটিংস", "নথি আপলোড", "মেডিকেল রেকর্ড", "ব্যক্তিগত খাদ্য", "উপসর্গ বিশ্লেষণ", "প্রকৃতি মূল্যায়ন", "বিকৃতি মূল্যায়ন", "অগ্নি মূল্যায়ন"),
        "kn" to screenLabels("ಡ್ಯಾಶ್‌ಬೋರ್ಡ್", "ಪ್ರೊಫೈಲ್", "ಸೆಟ್ಟಿಂಗ್‌ಗಳು", "ದಾಖಲೆ ಅಪ್‌ಲೋಡ್", "ವೈದ್ಯಕೀಯ ದಾಖಲೆಗಳು", "ವೈಯಕ್ತಿಕ ಆಹಾರ", "ಲಕ್ಷಣ ವಿಶ್ಲೇಷಣೆ", "ಪ್ರಕೃತಿ ಮೌಲ್ಯಮಾಪನ", "ವಿಕೃತಿ ಮೌಲ್ಯಮಾಪನ", "ಅಗ್ನಿ ಮೌಲ್ಯಮಾಪನ"),
        "ml" to screenLabels("ഡാഷ്ബോർഡ്", "പ്രൊഫൈൽ", "ക്രമീകരണങ്ങൾ", "രേഖ അപ്‌ലോഡ്", "മെഡിക്കൽ രേഖകൾ", "വ്യക്തിഗത ഭക്ഷണം", "ലക്ഷണ വിശകലനം", "പ്രകൃതി വിലയിരുത്തൽ", "വികൃതി വിലയിരുത്തൽ", "അഗ്നി വിലയിരുത്തൽ"),
        "pa" to screenLabels("ਡੈਸ਼ਬੋਰਡ", "ਪ੍ਰੋਫਾਈਲ", "ਸੈਟਿੰਗਜ਼", "ਦਸਤਾਵੇਜ਼ ਅਪਲੋਡ", "ਮੈਡੀਕਲ ਰਿਕਾਰਡ", "ਨਿੱਜੀ ਖੁਰਾਕ", "ਲੱਛਣ ਵਿਸ਼ਲੇਸ਼ਣ", "ਪ੍ਰਕ੍ਰਿਤੀ ਮੁਲਾਂਕਣ", "ਵਿਕ੍ਰਿਤੀ ਮੁਲਾਂਕਣ", "ਅਗਨੀ ਮੁਲਾਂਕਣ")
    )

    private fun screenLabels(dashboard: String, profile: String, settings: String, upload: String, records: String, diet: String, symptoms: String, prakriti: String, vikriti: String, agni: String) = mapOf(
        "Dashboard" to dashboard,
        "Patient Dashboard" to dashboard,
        "Profile" to profile,
        "Patient Profile" to profile,
        "Settings" to settings,
        "Upload Medical Document" to upload,
        "Upload New Document" to upload,
        "Medical Records" to records,
        "My Medical Documents" to records,
        "Personalized Diet" to diet,
        "Symptoms Analysis" to symptoms,
        "Prakriti Assessment" to prakriti,
        "Vikriti Assessment" to vikriti,
        "Agni Assessment" to agni
    )

    private val commonPhrase = supportedAppLanguages.associate { language ->
        language.code to phraseFor(language.code)
    }

    private val sharedPhrase = mapOf(
        "hi" to medicalWords("आयुर्वेदिक", "मूल्यांकन", "परिणाम", "दस्तावेज़", "चिकित्सा", "स्वास्थ्य", "आहार", "लक्षण", "विश्लेषण", "प्रोफाइल", "रोगी", "डॉक्टर", "प्रकृति", "विकृति", "अग्नि", "वात", "पित्त", "कफ"),
        "gu" to medicalWords("આયુર્વેદિક", "મૂલ્યાંકન", "પરિણામ", "દસ્તાવેજ", "ચિકિત્સા", "આરોગ્ય", "આહાર", "લક્ષણો", "વિશ્લેષણ", "પ્રોફાઇલ", "રોગી", "ડોક્ટર", "પ્રકૃતિ", "વિકૃતિ", "અગ્નિ", "વાત", "પિત્ત", "કફ"),
        "mr" to medicalWords("आयुर्वेदिक", "मूल्यांकन", "निकाल", "दस्तऐवज", "वैद्यकीय", "आरोग्य", "आहार", "लक्षणे", "विश्लेषण", "प्रोफाइल", "रुग्ण", "डॉक्टर", "प्रकृती", "विकृती", "अग्नी", "वात", "पित्त", "कफ"),
        "ta" to medicalWords("ஆயுர்வேத", "மதிப்பீடு", "முடிவு", "ஆவணம்", "மருத்துவ", "ஆரோக்கிய", "உணவு", "அறிகுறிகள்", "பகுப்பாய்வு", "சுயவிவரம்", "நோயாளர்", "மருத்துவர்", "பிரகிருதி", "விகிருதி", "அக்னி", "வாதம்", "பித்தம்", "கபம்"),
        "te" to medicalWords("ఆయుర్వేద", "అంచనా", "ఫలితం", "పత్రం", "వైద్య", "ఆరోగ్య", "ఆహారం", "లక్షణాలు", "విశ్లేషణ", "ప్రొఫైల్", "రోగి", "డాక్టర్", "ప్రకృతి", "వికృతి", "అగ్ని", "వాత", "పిత్త", "కఫ"),
        "bn" to medicalWords("আয়ুর্বেদিক", "মূল্যায়ন", "ফলাফল", "নথি", "চিকিৎসা", "স্বাস্থ্য", "খাদ্য", "উপসর্গ", "বিশ্লেষণ", "প্রোফাইল", "রোগী", "ডাক্তার", "প্রকৃতি", "বিকৃতি", "অগ্নি", "বাত", "পিত্ত", "কফ"),
        "kn" to medicalWords("ಆಯುರ್ವೇದ", "ಮೌಲ್ಯಮಾಪನ", "ಫಲಿತಾಂಶ", "ದಾಖಲೆ", "ವೈದ್ಯಕೀಯ", "ಆರೋಗ್ಯ", "ಆಹಾರ", "ಲಕ್ಷಣಗಳು", "ವಿಶ್ಲೇಷಣೆ", "ಪ್ರೊಫೈಲ್", "ರೋಗಿ", "ವೈದ್ಯ", "ಪ್ರಕೃತಿ", "ವಿಕೃತಿ", "ಅಗ್ನಿ", "ವಾತ", "ಪಿತ್ತ", "ಕಫ"),
        "ml" to medicalWords("ആയുർവേദ", "വിലയിരുത്തൽ", "ഫലം", "രേഖ", "മെഡിക്കൽ", "ആരോഗ്യം", "ഭക്ഷണം", "ലക്ഷണങ്ങൾ", "വിശകലനം", "പ്രൊഫൈൽ", "രോഗി", "ഡോക്ടർ", "പ്രകൃതി", "വികൃതി", "അഗ്നി", "വാത", "പിത്ത", "കഫ"),
        "pa" to medicalWords("ਆਯੁਰਵੇਦਿਕ", "ਮੁਲਾਂਕਣ", "ਨਤੀਜਾ", "ਦਸਤਾਵੇਜ਼", "ਮੈਡੀਕਲ", "ਸਿਹਤ", "ਖੁਰਾਕ", "ਲੱਛਣ", "ਵਿਸ਼ਲੇਸ਼ਣ", "ਪ੍ਰੋਫਾਈਲ", "ਮਰੀਜ਼", "ਡਾਕਟਰ", "ਪ੍ਰਕ੍ਰਿਤੀ", "ਵਿਕ੍ਰਿਤੀ", "ਅਗਨੀ", "ਵਾਤ", "ਪਿੱਤ", "ਕਫ")
    )

    private fun phraseFor(code: String): Map<String, String> {
        val w = commonExact[code].orEmpty()
        return mapOf(
            "Language" to w.value("Language"),
            "Back" to w.value("Back"),
            "Login" to w.value("Login"),
            "Logout" to w.value("Logout"),
            "Cancel" to w.value("Cancel"),
            "Next" to w.value("Next"),
            "Previous" to w.value("Previous"),
            "Continue" to w.value("Continue"),
            "Save" to w.value("Save"),
            "Yes" to w.value("Yes"),
            "No" to w.value("No"),
            "Vata" to w.value("Vata"),
            "Pitta" to w.value("Pitta"),
            "Kapha" to w.value("Kapha")
        )
    }

    private fun Map<String, String>.value(key: String): String = this[key] ?: key

    private fun medicalWords(ayurvedic: String, assessment: String, result: String, document: String, medical: String, health: String, diet: String, symptoms: String, analysis: String, profile: String, patient: String, doctor: String, prakriti: String, vikriti: String, agni: String, vata: String, pitta: String, kapha: String) = mapOf(
        "Ayurvedic" to ayurvedic,
        "Assessment" to assessment,
        "Result" to result,
        "Results" to result,
        "Document" to document,
        "Documents" to document,
        "Upload" to document,
        "Medical" to medical,
        "Health" to health,
        "Diet" to diet,
        "Symptoms" to symptoms,
        "Analysis" to analysis,
        "Profile" to profile,
        "Patient" to patient,
        "Doctor" to doctor,
        "Prakriti" to prakriti,
        "Vikriti" to vikriti,
        "Agni" to agni,
        "Vata" to vata,
        "Pitta" to pitta,
        "Kapha" to kapha
    )

    private fun label(language: String, english: String): String = language

    private fun String.containsLatinLetters(): Boolean = any { it in 'A'..'Z' || it in 'a'..'z' }

    private fun transliterateFallback(languageCode: String, source: String): String {
        val script = scriptFallback[languageCode] ?: scriptFallback["hi"].orEmpty()
        return buildString {
            source.forEach { char ->
                append(
                    when {
                        char.isWhitespace() -> char
                        char.isDigit() -> char
                        char in 'A'..'Z' || char in 'a'..'z' -> script[(char.lowercaseChar() - 'a').coerceIn(0, 25)]
                        else -> char
                    }
                )
            }
        }.replace(Regex("\\s+"), " ").trim()
    }

    private val scriptFallback = mapOf(
        "hi" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "mr" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "ne" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "sa" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "brx" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "doi" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "gom" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "mai" to listOf("अ","ब","क","द","ए","फ","ग","ह","इ","ज","क","ल","म","न","ओ","प","क","र","स","त","उ","व","व","क्ष","य","ज"),
        "gu" to listOf("અ","બ","ક","દ","એ","ફ","ગ","હ","ઇ","જ","ક","લ","મ","ન","ઓ","પ","ક","ર","સ","ત","ઉ","વ","વ","ક્ષ","ય","ઝ"),
        "bn" to listOf("অ","ব","ক","দ","এ","ফ","গ","হ","ই","জ","ক","ল","ম","ন","ও","প","ক","র","স","ত","উ","ভ","ও","ক্ষ","য","জ"),
        "as" to listOf("অ","ব","ক","দ","এ","ফ","গ","হ","ই","জ","ক","ল","ম","ন","ও","প","ক","ৰ","স","ত","উ","ভ","ও","ক্ষ","য","জ"),
        "mni" to listOf("অ","ব","ক","দ","এ","ফ","গ","হ","ই","জ","ক","ল","ম","ন","ও","প","ক","র","স","ত","উ","ভ","ও","ক্ষ","য","জ"),
        "kn" to listOf("ಅ","ಬ","ಕ","ದ","ಎ","ಫ","ಗ","ಹ","ಇ","ಜ","ಕ","ಲ","ಮ","ನ","ಒ","ಪ","ಕ","ರ","ಸ","ತ","ಉ","ವ","ವ","ಕ್ಷ","ಯ","ಜ"),
        "ml" to listOf("അ","ബ","ക","ദ","എ","ഫ","ഗ","ഹ","ഇ","ജ","ക","ല","മ","ന","ഒ","പ","ക","ര","സ","ത","ഉ","വ","വ","ക്ഷ","യ","ജ"),
        "ta" to listOf("அ","ப","க","த","எ","ஃப","க","ஹ","இ","ஜ","க","ல","ம","ந","ஒ","ப","க","ர","ச","த","உ","வ","வ","க்ஷ","ய","ஜ"),
        "te" to listOf("అ","బ","క","ద","ఎ","ఫ","గ","హ","ఇ","జ","క","ల","మ","న","ఒ","ప","క","ర","స","త","ఉ","వ","వ","క్ష","య","జ"),
        "or" to listOf("ଅ","ବ","କ","ଦ","ଏ","ଫ","ଗ","ହ","ଇ","ଜ","କ","ଲ","ମ","ନ","ଓ","ପ","କ","ର","ସ","ତ","ଉ","ଭ","ୱ","କ୍ଷ","ୟ","ଜ"),
        "pa" to listOf("ਅ","ਬ","ਕ","ਦ","ਏ","ਫ","ਗ","ਹ","ਇ","ਜ","ਕ","ਲ","ਮ","ਨ","ਓ","ਪ","ਕ","ਰ","ਸ","ਤ","ਉ","ਵ","ਵ","ਕਸ਼","ਯ","ਜ਼"),
        "sat" to listOf("ᱟ","ᱵ","ᱪ","ᱫ","ᱮ","ᱯ","ᱜ","ᱦ","ᱤ","ᱡ","ᱠ","ᱞ","ᱢ","ᱱ","ᱚ","ᱯ","ᱠ","ᱨ","ᱥ","ᱛ","ᱩ","ᱣ","ᱣ","ᱠᱥ","ᱭ","ᱡ"),
        "ks" to listOf("ا","ب","ک","د","ے","ف","گ","ہ","ی","ج","ک","ل","م","ن","و","پ","ق","ر","س","ت","و","و","و","کس","ی","ز"),
        "sd" to listOf("ا","ب","ڪ","د","ي","ف","گ","ھ","ي","ج","ڪ","ل","م","ن","و","پ","ق","ر","س","ت","و","و","و","ڪس","ي","ز"),
        "ur" to listOf("ا","ب","ک","د","ے","ف","گ","ہ","ی","ج","ک","ل","م","ن","و","پ","ق","ر","س","ت","و","و","و","کس","ی","ز")
    )
}
