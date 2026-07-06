package br.com.devpaulo.legendchat.channels.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

import br.com.devpaulo.legendchat.Main;
import br.com.devpaulo.legendchat.api.Legendchat;
import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import br.com.devpaulo.legendchat.channels.types.BungeecordChannel;
import br.com.devpaulo.legendchat.channels.types.Channel;
import br.com.devpaulo.legendchat.channels.types.TemporaryChannel;
import br.com.devpaulo.legendchat.text.PlaceholderUtils;
import br.com.devpaulo.legendchat.text.TextUtils;

public class ChannelUtils {
	public static void fakeMessage(final Channel c, final Player sender, final String message) {
		// Modern Paper chat uses AsyncChatEvent + Adventure components. Do not synthesize the
		// deprecated PlayerChatEvent/AsyncPlayerChatEvent just to obtain a Bukkit format string.
		c.sendMessage(sender, message, "", false);
	}
	
	public static void realMessage(Channel c, Player sender, String message, String bukkit_format, boolean cancelled) {
		if(c instanceof TemporaryChannel) {
			if(!((TemporaryChannel)c).user_list().contains(sender)) {
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("tc_error8"));
				return;
			}
		}
		else {
			if(!sender.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".chat")&&!sender.hasPermission("legendchat.admin")) {
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error2"));
				return;
			}
			if(sender.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".blockwrite")&&!sender.hasPermission("legendchat.admin")) {
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error2"));
				return;
			}
		}
		if(c.isFocusNeeded()) {
			if(Legendchat.getPlayerManager().getPlayerFocusedChannel(sender)!=c) {
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error12"));
				return;
			}
		}
		int delay = Legendchat.getDelayManager().getPlayerDelayFromChannel(sender.getName(), c);
		if(delay>0) {
			TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error11").replace("@time", Integer.toString(delay)));
			return;
		}
		if(Legendchat.getMuteManager().isPlayerMuted(sender.getName())) {
			int time = Legendchat.getMuteManager().getPlayerMuteTimeLeft(sender.getName());
			if(time==0)
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("mute_error4"));
			else
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("mute_error5").replace("@time", Integer.toString(time)));
			return;
		}
		if(Legendchat.getMuteManager().isServerMuted()) {
			TextUtils.send(sender, Legendchat.getMessageManager().getMessage("mute_error8"));
			return;
		}
		if(Legendchat.getIgnoreManager().hasPlayerIgnoredChannel(sender, c)) {
			TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error14"));
			return;
		}
		Set<Player> recipients = new HashSet<Player>();
		if(c instanceof TemporaryChannel) {
			recipients.addAll(((TemporaryChannel)c).user_list());
		}
		else {
			for(Player p : Bukkit.getOnlinePlayers())
				if(p.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".chat")||p.hasPermission("legendchat.admin"))
					recipients.add(p);
		}
		Set<Player> recipients2 = new HashSet<Player>();
		recipients2.addAll(recipients);

		for(Player p : recipients2) {
			if(c.getMaxDistance()!=0) {
				if(sender.getWorld()!=p.getWorld()) {
					recipients.remove(p);
					continue;
				}
				else if(sender.getLocation().distance(p.getLocation())>c.getMaxDistance()) {
					recipients.remove(p);
					continue;
				}
			}
			else {
				if(!c.isCrossworlds())
					if(sender.getWorld()!=p.getWorld()) {
						recipients.remove(p);
						continue;
					}
			}
			if(Legendchat.getIgnoreManager().hasPlayerIgnoredPlayer(p, sender.getName())) {
				recipients.remove(p);
				continue;
			}
			if(Legendchat.getIgnoreManager().hasPlayerIgnoredChannel(p, c)) {
				recipients.remove(p);
				continue;
			}
			if(c.isFocusNeeded())
				if(Legendchat.getPlayerManager().getPlayerFocusedChannel(p)!=c)
					recipients.remove(p);
		}
		
		boolean gastou = false;
		if(!Main.block_econ&&c.getMessageCost()>0) {
			if(!sender.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".free")&&!sender.hasPermission("legendchat.admin")) {
				if(Main.econ.getBalance(sender)<c.getMessageCost()) {
					TextUtils.send(sender, Legendchat.getMessageManager().getMessage("error3").replace("@price", Double.toString(c.getMessageCost())));
					return;
				}
				Main.econ.withdrawPlayer(sender, c.getMessageCost());
				gastou=true;
			}
		}
		String n_format_p_p = "";
		String n_format_p = "";
		String n_format_s = "";
		if(bukkit_format.contains("<")&&bukkit_format.contains(">")) {
			String name_code = null;
			if(bukkit_format.contains("%1$s"))
				name_code="%1$s";
			else if(bukkit_format.contains("%s"))
				name_code="%s";
			int seploc = bukkit_format.indexOf(name_code);
			int finalloc = -1;
			for(int i=seploc;i>=0;i--)
				if(bukkit_format.charAt(i)=='<') {
					finalloc=i;
					break;
				}
			if(finalloc!=-1) {
				n_format_p_p = bukkit_format.substring(0, finalloc);
				if(name_code!=null) {
					String[] n_format = bukkit_format.substring(finalloc+1).split(">")[0].split(name_code);
					if(n_format.length>0)
						n_format_p = n_format[0].replace(name_code, "").replace("{factions_relcolor}", "");
					if(n_format.length>1)
						n_format_s = n_format[1];
				}
			}
		}
		HashMap<String,String> tags = new HashMap<String,String>();
		tags.put("name", c.getName());
		tags.put("nick", c.getNickname());
		tags.put("color", c.getColor());
		tags.put("sender", TextUtils.componentToLegacy(sender.displayName()));
		tags.put("plainsender", sender.getName());
		tags.put("world", sender.getWorld().getName());
		tags.put("bprefix", (Legendchat.forceRemoveDoubleSpacesFromBukkit()?(n_format_p_p.equals(" ")?"":n_format_p_p.replace("  ", " ")):n_format_p_p));
		tags.put("bprefix2", (Legendchat.forceRemoveDoubleSpacesFromBukkit()?(n_format_p.equals(" ")?"":n_format_p.replace("  ", " ")):n_format_p));
		tags.put("bsuffix", (Legendchat.forceRemoveDoubleSpacesFromBukkit()?(n_format_s.equals(" ")?"":n_format_s.replace("  ", " ")):n_format_s));
		tags.put("server", Legendchat.getMessageManager().getMessage("bungeecord_server"));
		tags.put("time_hour", Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
		tags.put("time_min", Integer.toString(Calendar.getInstance().get(Calendar.MINUTE)));
		tags.put("time_sec", Integer.toString(Calendar.getInstance().get(Calendar.SECOND)));
		tags.put("date_day", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
		tags.put("date_month", Integer.toString(Calendar.getInstance().get(Calendar.MONTH)));
		tags.put("date_year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
		if(!Main.block_chat) {
			tags.put("prefix", tag(sender, Main.chat.getPlayerPrefix(sender)));
			tags.put("suffix", tag(sender, Main.chat.getPlayerSuffix(sender)));
			tags.put("groupprefix", tag(sender, Main.chat.getGroupPrefix(sender.getWorld(), Main.chat.getPrimaryGroup(sender))));
			tags.put("groupsuffix", tag(sender, Main.chat.getGroupSuffix(sender.getWorld(), Main.chat.getPrimaryGroup(sender))));
			for(String g : Main.chat.getPlayerGroups(sender)) {
				tags.put(g.toLowerCase()+"prefix", tag(sender, Main.chat.getGroupPrefix(sender.getWorld(), g)));
				tags.put(g.toLowerCase()+"suffix", tag(sender, Main.chat.getGroupSuffix(sender.getWorld(), g)));
			}
		}
		HashMap<String,String> ttt = Legendchat.textToTag();
		if(ttt.size()>0) {
			HashSet<Player> p = new HashSet<Player>();
			p.add(sender);
			int i=1;
			for(String n : ttt.keySet()) {
				String tag = "";
				try {tag=bukkit_format.split("°"+i+"º°")[1].split("°"+(i+1)+"º°")[0];}
				catch(Exception e) {tag="";}
				tags.put(n, tag);
				i++;
			}
		}
		ChatMessageEvent e = new ChatMessageEvent(c,sender,message,Legendchat.format(c.getFormat()),c.getFormat(),bukkit_format,recipients,tags,cancelled);
		Bukkit.getPluginManager().callEvent(e);
		if(e.isCancelled())
			return;
		sender = e.getSender();
		message = e.getMessage();
		if(Legendchat.isCensorActive())
			message = Legendchat.getCensorManager().censorFunction(message);
		String completa = e.getFormat();
		if(Legendchat.blockRepeatedTags()) {
			if(e.getTags().contains("prefix")&&e.getTags().contains("groupprefix"))
				if(e.getTagValue("prefix").equals(e.getTagValue("groupprefix")))
					e.setTagValue("prefix", "");
			if(e.getTags().contains("suffix")&&e.getTags().contains("groupsuffix"))
				if(e.getTagValue("suffix").equals(e.getTagValue("groupsuffix")))
					e.setTagValue("suffix", "");
		}
		for(String n : e.getTags())
			completa = completa.replace("{"+n+"}", TextUtils.colorizeLegacy(PlaceholderUtils.apply(sender, e.getTagValue(n))));
		completa = PlaceholderUtils.apply(sender, completa);
		completa = completa.replace("{msg}", translateAlternateChatColorsWithPermission(sender, message));
		
		for(Player p : e.getRecipients())
			TextUtils.send(p, completa);
		
		if(c.getDelayPerMessage()>0&&!sender.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".nodelay")&&!sender.hasPermission("legendchat.admin"))
			Legendchat.getDelayManager().addPlayerDelay(sender.getName(), c);
		
		if(c.getMaxDistance()!=0)
			if(Legendchat.showNoOneHearsYou()) {
				boolean show = false;
				if(e.getRecipients().size()==0)
					show=true;
				else if(e.getRecipients().size()==1&&e.getRecipients().contains(sender))
					show=true;
				else {
					show=true;
					for(Player p : e.getRecipients())
						if(p!=sender&&!Legendchat.getPlayerManager().isPlayerHiddenFromRecipients(p)) {
							show=false;
							break;
						}
				}
				if(show)
					TextUtils.send(sender, Legendchat.getMessageManager().getMessage("special"));
			}
		
		for(Player p : Legendchat.getPlayerManager().getOnlineSpys())
			if(!e.getRecipients().contains(p))
				TextUtils.send(p, TextUtils.colorizeLegacy(Legendchat.getFormat("spy").replace("{msg}", TextUtils.stripLegacy(completa))));
		
		if(gastou)
			if(c.showCostMessage())
				TextUtils.send(sender, Legendchat.getMessageManager().getMessage("message9").replace("@money", Double.toString(c.getCostPerMessage())));
		
		if(Legendchat.logToBukkit())
			TextUtils.send(Bukkit.getConsoleSender(), completa);
		
		if(Legendchat.logToFile())
			Legendchat.getLogManager().addLogToCache(TextUtils.stripLegacy(completa));
		
		if(c instanceof BungeecordChannel) {
			if(Legendchat.isBungeecordActive()) {
				if(Legendchat.getBungeecordChannel()==((BungeecordChannel)c)) {
					ByteArrayOutputStream b = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream(b);
					try {
						HashMap<String,String> tags_packet = new HashMap<String,String>();
						for(String tag_packet : e.getTags())
							tags_packet.put(tag_packet,e.getTagValue(tag_packet));
						out.writeUTF(tags_packet.toString());
						out.writeUTF(translateAlternateChatColorsWithPermission(sender, message));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					sender.sendPluginMessage(Bukkit.getPluginManager().getPlugin("Legendchat"), Main.PLUGIN_MESSAGE_CHANNEL, b.toByteArray());
				}
			}
		}
	}
	
	public static void otherMessage(Channel c, String message) {
		Set<Player> recipients = new HashSet<Player>();
		if(c instanceof TemporaryChannel) {
			recipients.addAll(((TemporaryChannel)c).user_list());
		}
		else {
			for(Player p : Bukkit.getOnlinePlayers())
				if(p.hasPermission("legendchat.channel."+c.getName().toLowerCase()+".chat")||p.hasPermission("legendchat.admin"))
					recipients.add(p);
		}
		Set<Player> recipients2 = new HashSet<Player>();
		recipients2.addAll(recipients);

		for(Player p : recipients2) {
			if(Legendchat.getIgnoreManager().hasPlayerIgnoredChannel(p, c)) {
				recipients.remove(p);
				continue;
			}
			if(c.isFocusNeeded())
				if(Legendchat.getPlayerManager().getPlayerFocusedChannel(p)!=c)
					recipients.remove(p);
		}
		
		/*ChatMessageEvent e = new ChatMessageEvent(c,sender,message,Legendchat.format(c.getFormat()),c.getFormat(),recipients,tags,cancelled);
		Bukkit.getPluginManager().callEvent(e);
		if(e.isCancelled())
			return;
		sender = e.getSender();
		message = e.getMessage();
		
		for(Player p : e.getRecipients())
			TextUtils.send(p, completa);*/
		for(Player p : recipients)
			TextUtils.send(p, message);
		
		if(Legendchat.logToBukkit())
			TextUtils.send(Bukkit.getConsoleSender(), message);
		
		if(Legendchat.logToFile())
			Legendchat.getLogManager().addLogToCache(TextUtils.stripLegacy(message));
	}
	
	public static String translateStringColor(String color) {
		switch(TextUtils.normalizeColorName(color)) {
			case "black": {return TextUtils.legacyCode('0');}
			case "darkblue": {return TextUtils.legacyCode('1');}
			case "darkgreen": {return TextUtils.legacyCode('2');}
			case "darkaqua": {return TextUtils.legacyCode('3');}
			case "darkred": {return TextUtils.legacyCode('4');}
			case "darkpurple": {return TextUtils.legacyCode('5');}
			case "gold": {return TextUtils.legacyCode('6');}
			case "gray": {return TextUtils.legacyCode('7');}
			case "darkgray": {return TextUtils.legacyCode('8');}
			case "blue": {return TextUtils.legacyCode('9');}
			case "green": {return TextUtils.legacyCode('a');}
			case "aqua": {return TextUtils.legacyCode('b');}
			case "red": {return TextUtils.legacyCode('c');}
			case "lightpurple": {return TextUtils.legacyCode('d');}
			case "yellow": {return TextUtils.legacyCode('e');}
			default: {return TextUtils.legacyCode('f');}
		}
	}
	
	public static NamedTextColor translateStringColorToTextColor(String color) {
		switch(TextUtils.normalizeColorName(color)) {
			case "black": {return NamedTextColor.BLACK;}
			case "darkblue": {return NamedTextColor.DARK_BLUE;}
			case "darkgreen": {return NamedTextColor.DARK_GREEN;}
			case "darkaqua": {return NamedTextColor.DARK_AQUA;}
			case "darkred": {return NamedTextColor.DARK_RED;}
			case "darkpurple": {return NamedTextColor.DARK_PURPLE;}
			case "gold": {return NamedTextColor.GOLD;}
			case "gray": {return NamedTextColor.GRAY;}
			case "darkgray": {return NamedTextColor.DARK_GRAY;}
			case "blue": {return NamedTextColor.BLUE;}
			case "green": {return NamedTextColor.GREEN;}
			case "aqua": {return NamedTextColor.AQUA;}
			case "red": {return NamedTextColor.RED;}
			case "lightpurple": {return NamedTextColor.LIGHT_PURPLE;}
			case "yellow": {return NamedTextColor.YELLOW;}
			default: {return NamedTextColor.WHITE;}
		}
	}
	
	public static String translateTextColorToStringColor(NamedTextColor color) {
		if(NamedTextColor.BLACK.equals(color)) return "black";
		if(NamedTextColor.DARK_BLUE.equals(color)) return "darkblue";
		if(NamedTextColor.DARK_GREEN.equals(color)) return "darkgreen";
		if(NamedTextColor.DARK_AQUA.equals(color)) return "darkaqua";
		if(NamedTextColor.DARK_RED.equals(color)) return "darkred";
		if(NamedTextColor.DARK_PURPLE.equals(color)) return "darkpurple";
		if(NamedTextColor.GOLD.equals(color)) return "gold";
		if(NamedTextColor.GRAY.equals(color)) return "gray";
		if(NamedTextColor.DARK_GRAY.equals(color)) return "darkgray";
		if(NamedTextColor.BLUE.equals(color)) return "blue";
		if(NamedTextColor.GREEN.equals(color)) return "green";
		if(NamedTextColor.AQUA.equals(color)) return "aqua";
		if(NamedTextColor.RED.equals(color)) return "red";
		if(NamedTextColor.LIGHT_PURPLE.equals(color)) return "lightpurple";
		if(NamedTextColor.YELLOW.equals(color)) return "yellow";
		return "white";
	}
	
	private static String tag(Player player, String tag) {
		if(tag==null)
			return "";
		return PlaceholderUtils.apply(player, tag);
	}
	
	public static String translateAlternateChatColorsWithPermission(Player p, String msg) {
		if(msg==null)
			return "";
		return replacePermittedCode(p, msg, '0', "black", false)
				.replace("&1", canUseColor(p, "darkblue", false) ? TextUtils.legacyCode('1') : "&1")
				.replace("&2", canUseColor(p, "darkgreen", false) ? TextUtils.legacyCode('2') : "&2")
				.replace("&3", canUseColor(p, "darkaqua", false) ? TextUtils.legacyCode('3') : "&3")
				.replace("&4", canUseColor(p, "darkred", false) ? TextUtils.legacyCode('4') : "&4")
				.replace("&5", canUseColor(p, "darkpurple", false) ? TextUtils.legacyCode('5') : "&5")
				.replace("&6", canUseColor(p, "gold", false) ? TextUtils.legacyCode('6') : "&6")
				.replace("&7", canUseColor(p, "gray", false) ? TextUtils.legacyCode('7') : "&7")
				.replace("&8", canUseColor(p, "darkgray", false) ? TextUtils.legacyCode('8') : "&8")
				.replace("&9", canUseColor(p, "blue", false) ? TextUtils.legacyCode('9') : "&9")
				.replace("&a", canUseColor(p, "green", false) ? TextUtils.legacyCode('a') : "&a")
				.replace("&b", canUseColor(p, "aqua", false) ? TextUtils.legacyCode('b') : "&b")
				.replace("&c", canUseColor(p, "red", false) ? TextUtils.legacyCode('c') : "&c")
				.replace("&d", canUseColor(p, "lightpurple", false) ? TextUtils.legacyCode('d') : "&d")
				.replace("&e", canUseColor(p, "yellow", false) ? TextUtils.legacyCode('e') : "&e")
				.replace("&f", canUseColor(p, "white", false) ? TextUtils.legacyCode('f') : "&f")
				.replace("&A", canUseColor(p, "green", false) ? TextUtils.legacyCode('a') : "&A")
				.replace("&B", canUseColor(p, "aqua", false) ? TextUtils.legacyCode('b') : "&B")
				.replace("&C", canUseColor(p, "red", false) ? TextUtils.legacyCode('c') : "&C")
				.replace("&D", canUseColor(p, "lightpurple", false) ? TextUtils.legacyCode('d') : "&D")
				.replace("&E", canUseColor(p, "yellow", false) ? TextUtils.legacyCode('e') : "&E")
				.replace("&F", canUseColor(p, "white", false) ? TextUtils.legacyCode('f') : "&F")
				.replace("&k", canUseColor(p, "obfuscated", true) || canUseColor(p, "obfuscate", true) ? TextUtils.legacyCode('k') : "&k")
				.replace("&l", canUseColor(p, "bold", true) ? TextUtils.legacyCode('l') : "&l")
				.replace("&m", canUseColor(p, "strikethrough", true) ? TextUtils.legacyCode('m') : "&m")
				.replace("&n", canUseColor(p, "underline", true) ? TextUtils.legacyCode('n') : "&n")
				.replace("&o", canUseColor(p, "italic", true) ? TextUtils.legacyCode('o') : "&o")
				.replace("&r", canUseColor(p, "reset", true) ? TextUtils.legacyCode('r') : "&r")
				.replace("&K", canUseColor(p, "obfuscated", true) || canUseColor(p, "obfuscate", true) ? TextUtils.legacyCode('k') : "&K")
				.replace("&L", canUseColor(p, "bold", true) ? TextUtils.legacyCode('l') : "&L")
				.replace("&M", canUseColor(p, "strikethrough", true) ? TextUtils.legacyCode('m') : "&M")
				.replace("&N", canUseColor(p, "underline", true) ? TextUtils.legacyCode('n') : "&N")
				.replace("&O", canUseColor(p, "italic", true) ? TextUtils.legacyCode('o') : "&O")
				.replace("&R", canUseColor(p, "reset", true) ? TextUtils.legacyCode('r') : "&R");
	}
	
	private static String replacePermittedCode(Player p, String msg, char code, String permissionName, boolean format) {
		String legacy = "&"+code;
		return msg.replace(legacy, canUseColor(p, permissionName, format) ? TextUtils.legacyCode(code) : legacy);
	}
	
	private static boolean canUseColor(Player p, String name, boolean format) {
		return p.hasPermission("legendchat.color."+name)
				|| p.hasPermission(format ? "legendchat.color.allformats" : "legendchat.color.allcolors")
				|| p.hasPermission("legendchat.admin");
	}
}