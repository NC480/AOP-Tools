package hu.pelyh.aoptools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class StressBotNames {
    private static final Pattern PREFIX = Pattern.compile("[A-Za-z0-9_]{1,10}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private StressBotNames() { }

    public static List<String> prepare(int count, String prefix, List<String> requestedNames) {
        if (count < 1 || count > 10) throw new IllegalArgumentException("A botszám csak 1 és 10 között lehet.");
        String safePrefix = prefix == null ? "" : prefix.trim();
        if (!PREFIX.matcher(safePrefix).matches()) {
            throw new IllegalArgumentException("A névelőtag 1-10 angol betűt, számot vagy aláhúzást tartalmazhat.");
        }

        List<String> names = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        if (requestedNames != null) {
            if (requestedNames.size() > 10) {
                throw new IllegalArgumentException("Legfeljebb 10 egyedi botnév adható meg.");
            }
            for (String requested : requestedNames) {
                String name = requested == null ? "" : requested.trim();
                if (!USERNAME.matcher(name).matches()) {
                    throw new IllegalArgumentException("Érvénytelen botnév: " + (name.isEmpty() ? "(üres)" : name)
                            + ". 1-16 angol betű, szám vagy aláhúzás használható.");
                }
                if (!unique.add(name.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Duplikált botnév: " + name);
                }
                if (names.size() < count) names.add(name);
            }
        }

        for (int index = 1; names.size() < count; index++) {
            String generated = generated(safePrefix, index);
            if (unique.add(generated.toLowerCase(Locale.ROOT))) names.add(generated);
        }
        return List.copyOf(names);
    }

    private static String generated(String prefix, int index) {
        String suffix = String.format("%03d", index);
        int maxPrefix = 16 - suffix.length();
        return prefix.substring(0, Math.min(prefix.length(), maxPrefix)) + suffix;
    }
}
