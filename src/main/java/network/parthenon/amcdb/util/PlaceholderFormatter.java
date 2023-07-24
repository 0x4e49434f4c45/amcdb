package network.parthenon.amcdb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderFormatter {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(?<=^|[^\\\\])%.*?[^\\\\]%");

    /**
     * Formats the given string to a list of objects using the specified generators.
     * @param format The format string.
     * @param placeholderValueGenerator Function returning values to replace placeholders
     * @param formatValueGenerator Function returning values for non-placeholder segments of the format string
     * @return List of all accumulated objects
     * @param <T> Type of object returned by the generators
     */
    public static <T> List<T> formatToObjects(
            String format,
            Function<String, List<? extends T>> placeholderValueGenerator,
            Function<String, List<? extends T>> formatValueGenerator) {
        List<T> objects = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(format);
        int lastMatchEnd = 0;
        while(matcher.find()) {
            if(matcher.start() > lastMatchEnd) {
                objects.addAll(formatValueGenerator.apply(format.substring(lastMatchEnd, matcher.start()).replace("\\%", "%")));
            }
            lastMatchEnd = matcher.end();
            objects.addAll(placeholderValueGenerator.apply(matcher.group().replace("\\%", "%")));
        }
        if(lastMatchEnd < format.length()) {
            objects.addAll(formatValueGenerator.apply(format.substring(lastMatchEnd).replace("\\%", "%")));
        }
        return objects;
    }

    /**
     * Replaces placeholders in the format string with the given replacements.
     * @param format Format string
     * @param replacements Strings with which to replace placeholders in the format string
     * @return Format string with placeholders replaced
     */
    public static String formatPlaceholders(String format, Map<String, String> replacements) {
        return String.join("", formatToObjects(format,
                p -> List.of(replacements.containsKey(p) ? replacements.get(p) : p),
                f -> List.of(f)));
    }
}
