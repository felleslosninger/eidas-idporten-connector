//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package eu.eidas.auth.commons;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public final class EidasStringUtil {
    private static final Pattern STRING_SPLITTER = Pattern.compile("[,;]");

    @Nonnull
    public static byte[] decodeBytesFromBase64(@Nonnull String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    @Nonnull
    public static String decodeStringFromBase64(@Nonnull String base64String) {
        return toString(decodeBytesFromBase64(base64String));
    }

    @Nonnull
    public static String encodeToBase64(@Nonnull byte[] bytes) {
        return bytes.length == 0 ? "" : Base64.getEncoder().encodeToString(bytes);
    }

    @Nonnull
    public static String encodeToBase64(@Nonnull String value) {
        return encodeToBase64(getBytes(value));
    }

    @Nonnull
    public static byte[] getBytes(@Nonnull String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String toString(@Nonnull byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static List<String> getTokens(String tokens) {
        return (StringUtils.isNotEmpty(tokens) ? Arrays.asList(STRING_SPLITTER.split(tokens)) : new ArrayList<>());
    }

    private EidasStringUtil() {
    }
}
