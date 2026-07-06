package br.com.devpaulo.legendchat.text;

import java.util.Locale;

import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Central bridge for the plugin's legacy configuration strings.
 *
 * Existing Legendchat configs use Bukkit-style '&' color codes. Keep those
 * configs compatible, but deserialize them through Adventure at send time.
 */
public final class TextUtils {
	public static final char LEGACY_COLOR_CHAR = '\u00A7';
	private static final String LEGACY_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";
	private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

	private TextUtils() {
	}

	public static String colorizeLegacy(String input) {
		if(input==null)
			return "";
		StringBuilder out = new StringBuilder(input.length());
		for(int i=0;i<input.length();i++) {
			char current = input.charAt(i);
			if(current=='&'&&i+1<input.length()&&LEGACY_CODES.indexOf(input.charAt(i+1))>=0) {
				out.append(LEGACY_COLOR_CHAR);
				out.append(Character.toLowerCase(input.charAt(++i)));
			}
			else
				out.append(current);
		}
		return out.toString();
	}

	public static String legacyCode(char code) {
		return String.valueOf(LEGACY_COLOR_CHAR)+Character.toLowerCase(code);
	}

	public static Component toComponent(String input) {
		return LEGACY_SECTION.deserialize(colorizeLegacy(input));
	}

	public static String componentToLegacy(Component component) {
		if(component==null)
			return "";
		return LEGACY_SECTION.serialize(component);
	}

	public static void send(CommandSender target, String message) {
		target.sendMessage(toComponent(message));
	}

	public static String stripLegacy(String input) {
		return PlainTextComponentSerializer.plainText().serialize(toComponent(input));
	}

	public static String normalizeColorName(String color) {
		if(color==null)
			return "white";
		return color.toLowerCase(Locale.ROOT).replace("_", "");
	}
}
