package com.gmail.filoghost.customjoinitems;

import java.util.HashMap;
import java.util.Map;

public class AsciiSymbols {
    public static Map<String, String> replacements = new HashMap<String, String>();

    static {
        replacements.put("<3", "\u2764");
        replacements.put("[*]", "\u2605");
        replacements.put("[**]", "\u2739");
        replacements.put("[p]", "\u25cf");
        replacements.put("[v]", "\u2714");
        replacements.put("[+]", "\u25c6");
        replacements.put("[++]", "\u2726");
        replacements.put("[x]", "\u2588");
        replacements.put("[/]", "\u258c");
    }

    public static String placeholdersToSymbols(String s) {
        if (s == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            s = s.replace(entry.getKey(), entry.getValue());
        }
        return s;
    }

    public static String symbolsToPlaceholders(String s) {
        if (s == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            s.replace(entry.getValue(), entry.getKey());
        }
        return s;
    }

    public static String toAscii(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\\P{ASCII}", "");
    }
}

