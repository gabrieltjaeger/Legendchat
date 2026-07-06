package br.com.devpaulo.legendchat.text;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Optional PlaceholderAPI bridge.
 *
 * Legendchat receives Vault prefixes/suffixes as legacy strings. Plugins such as
 * Oraxen commonly expose chat glyphs through PlaceholderAPI placeholders, so we
 * need to expand configured/tag text before converting it to Adventure
 * components.
 */
public final class PlaceholderUtils {
	private PlaceholderUtils() {
	}

	public static boolean isAvailable() {
		return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
	}

	public static String apply(Player player, String input) {
		if(input==null || input.isEmpty())
			return input==null ? "" : input;
		if(player==null || !isAvailable())
			return input;
		return PlaceholderAPI.setPlaceholders(player, input);
	}
}
