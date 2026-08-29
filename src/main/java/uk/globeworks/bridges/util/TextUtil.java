package uk.globeworks.bridges.util;

import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern COLOUR = Pattern.compile(
        "(?i)[§&][0-9a-fk-orx]|[§&]#[0-9a-f]{6}"
    );

    private TextUtil() {}

    public static String strip(String input) {
        if (input == null) return null;
        String s = COLOUR.matcher(input).replaceAll("");
        s = s.replace('_', ' ');
        return s.replaceAll("\\s+", " ").trim();
    }
}
