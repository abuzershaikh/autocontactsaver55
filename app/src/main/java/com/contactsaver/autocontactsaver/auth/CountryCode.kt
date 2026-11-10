package com.contactsaver.autocontactsaver.auth

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import java.util.Locale

data class CountryCode(
    val flag: String,
    val name: String,
    val code: String,
    val iso: String
)

val ALL_COUNTRIES = listOf(
    CountryCode("🇮🇳", "India", "+91", "in"),
    CountryCode("🇺🇸", "United States", "+1", "us"),
    CountryCode("🇬🇧", "United Kingdom", "+44", "gb"),
    CountryCode("🇦🇪", "United Arab Emirates", "+971", "ae"),
    CountryCode("🇸🇦", "Saudi Arabia", "+966", "sa"),
    CountryCode("🇨🇦", "Canada", "+1", "ca"),
    CountryCode("🇦🇺", "Australia", "+61", "au"),
    CountryCode("🇸🇬", "Singapore", "+65", "sg"),
    CountryCode("🇵🇰", "Pakistan", "+92", "pk"),
    CountryCode("🇧🇩", "Bangladesh", "+880", "bd"),
    CountryCode("🇳🇵", "Nepal", "+977", "np"),
    CountryCode("🇱🇱", "Sri Lanka", "+94", "lk"),
    CountryCode("🇲🇾", "Malaysia", "+60", "my"),
    CountryCode("🇿🇦", "South Africa", "+27", "za"),
    CountryCode("🇶🇦", "Qatar", "+974", "qa"),
    CountryCode("🇰🇼", "Kuwait", "+965", "kw"),
    CountryCode("🇴🇲", "Oman", "+968", "om"),
    CountryCode("🇧🇭", "Bahrain", "+973", "bh"),
    CountryCode("🇩🇪", "Germany", "+49", "de"),
    CountryCode("🇫🇷", "France", "+33", "fr"),
    CountryCode("🇮🇹", "Italy", "+39", "it"),
    CountryCode("🇪🇸", "Spain", "+34", "es"),
    CountryCode("🇯🇵", "Japan", "+81", "jp"),
    CountryCode("🇨🇳", "China", "+86", "cn"),
    CountryCode("🇧🇷", "Brazil", "+55", "br"),
    CountryCode("🇲🇽", "Mexico", "+52", "mx"),
    CountryCode("🇷🇺", "Russia", "+7", "ru"),
    CountryCode("🇮🇩", "Indonesia", "+62", "id"),
    CountryCode("🇹🇭", "Thailand", "+66", "th"),
    CountryCode("🇻🇳", "Vietnam", "+84", "vn"),
    CountryCode("🇵🇭", "Philippines", "+63", "ph"),
    CountryCode("🇪🇬", "Egypt", "+20", "eg"),
    CountryCode("🇳🇬", "Nigeria", "+234", "ng"),
    CountryCode("🇰🇪", "Kenya", "+254", "ke"),
    CountryCode("🇦🇷", "Argentina", "+54", "ar"),
    CountryCode("🇨🇱", "Chile", "+56", "cl"),
    CountryCode("🇨🇴", "Colombia", "+57", "co"),
    CountryCode("🇵🇪", "Peru", "+51", "pe"),
    CountryCode("🇳🇱", "Netherlands", "+31", "nl"),
    CountryCode("🇧🇪", "Belgium", "+32", "be"),
    CountryCode("🇨🇭", "Switzerland", "+41", "ch"),
    CountryCode("🇦🇹", "Austria", "+43", "at"),
    CountryCode("🇸🇪", "Sweden", "+46", "se"),
    CountryCode("🇳🇴", "Norway", "+47", "no"),
    CountryCode("🇩🇰", "Denmark", "+45", "dk"),
    CountryCode("🇫🇮", "Finland", "+358", "fi"),
    CountryCode("🇵🇹", "Portugal", "+351", "pt"),
    CountryCode("🇬🇷", "Greece", "+30", "gr"),
    CountryCode("🇹🇷", "Turkey", "+90", "tr"),
    CountryCode("🇮🇪", "Ireland", "+353", "ie"),
    CountryCode("🇳🇿", "New Zealand", "+64", "nz"),
    CountryCode("🇵🇱", "Poland", "+48", "pl"),
    CountryCode("🇺🇦", "Ukraine", "+380", "ua"),
    CountryCode("🇷🇴", "Romania", "+40", "ro"),
    CountryCode("🇨🇿", "Czech Republic", "+420", "cz"),
    CountryCode("🇭🇺", "Hungary", "+36", "hu"),
    CountryCode("🇮🇱", "Israel", "+972", "il"),
    CountryCode("🇮🇷", "Iran", "+98", "ir"),
    CountryCode("🇮🇶", "Iraq", "+964", "iq"),
    CountryCode("🇯🇴", "Jordan", "+962", "jo"),
    CountryCode("🇱🇧", "Lebanon", "+961", "lb"),
    CountryCode("🇲🇦", "Morocco", "+212", "ma"),
    CountryCode("🇩🇿", "Algeria", "+213", "dz"),
    CountryCode("🇹🇳", "Tunisia", "+216", "tn"),
    CountryCode("🇪🇹", "Ethiopia", "+251", "et"),
    CountryCode("🇬🇭", "Ghana", "+233", "gh"),
    CountryCode("🇹🇿", "Tanzania", "+255", "tz"),
    CountryCode("🇺🇬", "Uganda", "+256", "ug"),
    CountryCode("🇿🇼", "Zimbabwe", "+263", "zw"),
    CountryCode("🇰🇷", "South Korea", "+82", "kr"),
    CountryCode("🇭🇰", "Hong Kong", "+852", "hk"),
    CountryCode("🇹🇼", "Taiwan", "+886", "tw"),
    CountryCode("🇲🇲", "Myanmar", "+95", "mm"),
    CountryCode("🇰🇭", "Cambodia", "+855", "kh"),
    CountryCode("LA", "Laos", "+856", "la"),
    CountryCode("🇲🇳", "Mongolia", "+976", "mn"),
    CountryCode("AF", "Afghanistan", "+93", "af"),
    CountryCode("UZ", "Uzbekistan", "+998", "uz"),
    CountryCode("KZ", "Kazakhstan", "+7", "kz"),
    CountryCode("AZ", "Azerbaijan", "+994", "az"),
    CountryCode("GE", "Georgia", "+995", "ge"),
    CountryCode("AM", "Armenia", "+374", "am"),
    CountryCode("IS", "Iceland", "+354", "is"),
    CountryCode("LU", "Luxembourg", "+352", "lu"),
    CountryCode("MT", "Malta", "+356", "mt"),
    CountryCode("CY", "Cyprus", "+357", "cy"),
    CountryCode("SK", "Slovakia", "+421", "sk"),
    CountryCode("SI", "Slovenia", "+386", "si"),
    CountryCode("HR", "Croatia", "+385", "hr"),
    CountryCode("RS", "Serbia", "+381", "rs"),
    CountryCode("BG", "Bulgaria", "+359", "bg"),
    CountryCode("BA", "Bosnia", "+387", "ba"),
    CountryCode("MK", "North Macedonia", "+389", "mk"),
    CountryCode("AL", "Albania", "+355", "al"),
    CountryCode("MD", "Moldova", "+373", "md"),
    CountryCode("BY", "Belarus", "+375", "by"),
    CountryCode("EE", "Estonia", "+372", "ee"),
    CountryCode("LV", "Latvia", "+371", "lv"),
    CountryCode("LT", "Lithuania", "+370", "lt"),
    CountryCode("UY", "Uruguay", "+598", "uy"),
    CountryCode("PY", "Paraguay", "+595", "py"),
    CountryCode("BO", "Bolivia", "+591", "bo"),
    CountryCode("EC", "Ecuador", "+593", "ec"),
    CountryCode("VE", "Venezuela", "+58", "ve"),
    CountryCode("CR", "Costa Rica", "+506", "cr"),
    CountryCode("PA", "Panama", "+507", "pa"),
    CountryCode("DO", "Dominican Republic", "+1-809", "do"),
    CountryCode("GT", "Guatemala", "+502", "gt"),
    CountryCode("HN", "Honduras", "+504", "hn"),
    CountryCode("SV", "El Salvador", "+503", "sv"),
    CountryCode("NI", "Nicaragua", "+505", "ni"),
    CountryCode("JM", "Jamaica", "+1-876", "jm"),
    CountryCode("TT", "Trinidad and Tobago", "+1-868", "tt"),
    CountryCode("CU", "Cuba", "+53", "cu"),
    CountryCode("HT", "Haiti", "+509", "ht"),
    CountryCode("BS", "Bahamas", "+1-242", "bs"),
    CountryCode("BB", "Barbados", "+1-246", "bb"),
    CountryCode("BZ", "Belize", "+501", "bz"),
    CountryCode("GY", "Guyana", "+592", "gy"),
    CountryCode("SR", "Suriname", "+597", "sr"),
    CountryCode("AO", "Angola", "+244", "ao"),
    CountryCode("MZ", "Mozambique", "+258", "mz"),
    CountryCode("CI", "Ivory Coast", "+225", "ci"),
    CountryCode("CM", "Cameroon", "+237", "cm"),
    CountryCode("SN", "Senegal", "+221", "sn"),
    CountryCode("MG", "Madagascar", "+261", "mg"),
    CountryCode("ZM", "Zambia", "+260", "zm"),
    CountryCode("ZW", "Zimbabwe", "+263", "zw"),
    CountryCode("BW", "Botswana", "+267", "bw"),
    CountryCode("NA", "Namibia", "+264", "na"),
    CountryCode("RW", "Rwanda", "+250", "rw"),
    CountryCode("MW", "Malawi", "+265", "mw"),
    CountryCode("GA", "Gabon", "+241", "ga"),
    CountryCode("CG", "Congo", "+242", "cg"),
    CountryCode("CD", "DR Congo", "+243", "cd"),
    CountryCode("GN", "Guinea", "+224", "gn"),
    CountryCode("ML", "Mali", "+223", "ml"),
    CountryCode("BF", "Burkina Faso", "+226", "bf"),
    CountryCode("NE", "Niger", "+227", "ne"),
    CountryCode("TD", "Chad", "+235", "td"),
    CountryCode("SD", "Sudan", "+249", "sd"),
    CountryCode("SS", "South Sudan", "+211", "ss"),
    CountryCode("SO", "Somalia", "+252", "so"),
    CountryCode("DJ", "Djibouti", "+253", "dj"),
    CountryCode("ER", "Eritrea", "+291", "er"),
    CountryCode("MU", "Mauritius", "+230", "mu"),
    CountryCode("SC", "Seychelles", "+248", "sc"),
    CountryCode("MV", "Maldives", "+960", "mv"),
    CountryCode("FJ", "Fiji", "+679", "fj"),
    CountryCode("PG", "Papua New Guinea", "+675", "pg"),
    CountryCode("NC", "New Caledonia", "+687", "nc"),
    CountryCode("PF", "French Polynesia", "+689", "pf"),
    CountryCode("WS", "Samoa", "+685", "ws"),
    CountryCode("TO", "Tonga", "+676", "to"),
    CountryCode("VU", "Vanuatu", "+678", "vu"),
    CountryCode("SB", "Solomon Islands", "+677", "sb"),
    CountryCode("FM", "Micronesia", "+691", "fm"),
    CountryCode("PW", "Palau", "+680", "pw"),
    CountryCode("NR", "Nauru", "+674", "nr"),
    CountryCode("TV", "Tuvalu", "+688", "tv"),
    CountryCode("KI", "Kiribati", "+686", "ki"),
    CountryCode("MH", "Marshall Islands", "+692", "mh"),
    CountryCode("AD", "Andorra", "+376", "ad"),
    CountryCode("MC", "Monaco", "+377", "mc"),
    CountryCode("SM", "San Marino", "+378", "sm"),
    CountryCode("VA", "Vatican City", "+379", "va"),
    CountryCode("LI", "Liechtenstein", "+423", "li"),
    CountryCode("GI", "Gibraltar", "+350", "gi"),
    CountryCode("FO", "Faroe Islands", "+298", "fo"),
    CountryCode("GL", "Greenland", "+299", "gl")
)

fun getAutoDetectedCountry(context: Context): CountryCode {
    try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        
        
        var simIso = tm?.simCountryIso?.lowercase()?.trim()
        
        
        if (simIso.isNullOrEmpty()) {
            simIso = tm?.networkCountryIso?.lowercase()?.trim()
        }
        
        
        if (simIso.isNullOrEmpty()) {
            simIso = Locale.getDefault().country?.lowercase()?.trim()
        }
        
        if (!simIso.isNullOrEmpty()) {
            val foundCountry = ALL_COUNTRIES.find { it.iso.lowercase() == simIso }
            if (foundCountry != null) {
                Log.d("CountryDetector", "Auto-detected country from SIM/Locale: ${foundCountry.name} (${foundCountry.code})")
                return foundCountry
            }
        }
    } catch (e: Exception) {
        Log.e("CountryDetector", "Failed to auto-detect SIM country: ${e.message}")
    }
    
    
    return ALL_COUNTRIES.find { it.iso == "in" } ?: ALL_COUNTRIES[0]
}
