package vn.haqua.shop.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SlugUtil() {
    }

    /** Bỏ dấu tiếng Việt, hạ chữ thường, nối bằng dấu "-". */
    public static String toSlug(String input) {
        return NON_ALNUM.matcher(unaccent(input)).replaceAll("-").replaceAll("(^-|-$)", "");
    }

    public static String unaccent(String input) {
        if (input == null) {
            return "";
        }
        String n = Normalizer.normalize(input.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        return DIACRITICS.matcher(n).replaceAll("").toLowerCase(Locale.ROOT);

    }
}
