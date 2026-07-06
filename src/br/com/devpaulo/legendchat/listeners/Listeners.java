package br.com.devpaulo.legendchat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import br.com.devpaulo.legendchat.Main;
import br.com.devpaulo.legendchat.api.Legendchat;
import br.com.devpaulo.legendchat.channels.types.Channel;

public class Listeners implements Listener {
	@EventHandler(priority = EventPriority.MONITOR)
	private void onJoin(PlayerJoinEvent e) {
		Legendchat.getPlayerManager().setPlayerFocusedChannel(e.getPlayer(), Legendchat.getDefaultChannel(), false);
		if(hasAnyPermission(e.getPlayer())) {
    		final Player p = e.getPlayer();
    		Bukkit.getServer().getScheduler().runTaskLater(Legendchat.getPlugin(), new Runnable() {
    			public void run() {
    				if(Main.need_update!=null) {
						p.sendMessage(Component.text("[Legendchat] ", NamedTextColor.GOLD)
								.append(Component.text("New update available: ", NamedTextColor.WHITE))
								.append(Component.text("V" + Main.need_update + "!", NamedTextColor.AQUA)));
						p.sendMessage(Component.text("Download: ", NamedTextColor.GOLD)
								.append(Component.text("http://dev.bukkit.org/bukkit-plugins/legendchat/", NamedTextColor.WHITE)));
    				}
    			}
    		}, 60L);
    	}
	}
	
	@EventHandler
	private void onQuit(PlayerQuitEvent e) {
		Legendchat.getPlayerManager().playerDisconnect(e.getPlayer());
		Legendchat.getPrivateMessageManager().playerDisconnect(e.getPlayer());
		Legendchat.getIgnoreManager().playerDisconnect(e.getPlayer());
		Legendchat.getTemporaryChannelManager().playerDisconnect(e.getPlayer());
		Legendchat.getAfkManager().playerDisconnect(e.getPlayer());
	}
	
	@EventHandler
	private void onKick(PlayerKickEvent e) {
		Legendchat.getPlayerManager().playerDisconnect(e.getPlayer());
		Legendchat.getPrivateMessageManager().playerDisconnect(e.getPlayer());
		Legendchat.getIgnoreManager().playerDisconnect(e.getPlayer());
		Legendchat.getTemporaryChannelManager().playerDisconnect(e.getPlayer());
		Legendchat.getAfkManager().playerDisconnect(e.getPlayer());
	}
	
	@EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
	private void onChat(AsyncChatEvent e) {
		if(e.isCancelled())
			return;
		
		final Player player = e.getPlayer();
		final String message = PlainTextComponentSerializer.plainText().serialize(e.message());
		
		// Legendchat performs channel routing, economy, Vault lookups, permissions and recipient filtering.
		// AsyncChatEvent is async, so cancel the native chat event and run the plugin routing on the main thread.
		e.setCancelled(true);
		Bukkit.getScheduler().runTask(Legendchat.getPlugin(), new Runnable() {
			public void run() {
				if(!player.isOnline())
					return;
				Legendchat.getAfkManager().removeAfk(player);
				if(Legendchat.getPrivateMessageManager().isPlayerTellLocked(player)) {
					Legendchat.getPrivateMessageManager().tellPlayer(player, null, message);
				}
				else {
					if(Legendchat.getPlayerManager().isPlayerFocusedInAnyChannel(player))
						Legendchat.getPlayerManager().getPlayerFocusedChannel(player).sendMessage(player, message, "", false);
					else
						player.sendMessage(Legendchat.getMessageManager().getMessage("error1"));
				}
			}
		});
	}
	
	@EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
	private void onChat(PlayerCommandPreprocessEvent e) {
		boolean block = false;
		if(Legendchat.blockShortcutsWhenCancelled())
			if(e.isCancelled())
				block=true;
		if(!block) {
			for(Channel c : Legendchat.getChannelManager().getChannels()) {
				String lowered_msg = e.getMessage().toLowerCase();
				if(c.isShortcutAllowed()) {
					if(lowered_msg.startsWith("/"+c.getNickname().toLowerCase())) {
						if(e.getMessage().length()==("/"+c.getNickname()).length()) {
							e.getPlayer().sendMessage(Legendchat.getMessageManager().getMessage("wrongcmd").replace("@command", "/"+c.getNickname().toLowerCase()+" <"+Legendchat.getMessageManager().getMessage("message")+">"));
							e.setCancelled(true);
						}
						else if(lowered_msg.startsWith("/"+c.getNickname().toLowerCase()+" ")) {
							String message = "";
							String[] split = e.getMessage().split(" ");
							for(int i=1;i<split.length;i++) {
								if(message.length()==0)
									message=split[i];
								else
									message+=" "+split[i];
							}
							c.sendMessage(e.getPlayer(), message);
							e.setCancelled(true);
						}
					}
					if(lowered_msg.startsWith("/"+c.getName().toLowerCase())) {
						if(e.getMessage().length()==("/"+c.getName()).length()) {
							e.getPlayer().sendMessage(Legendchat.getMessageManager().getMessage("wrongcmd").replace("@command", "/"+c.getName().toLowerCase()+" <"+Legendchat.getMessageManager().getMessage("message")+">"));
							e.setCancelled(true);
						}
						else if(lowered_msg.startsWith("/"+c.getName().toLowerCase()+" ")) {
							String message = "";
							String[] split = e.getMessage().split(" ");
							for(int i=1;i<split.length;i++) {
								if(message.length()==0)
									message=split[i];
								else
									message+=" "+split[i];
							}
							c.sendMessage(e.getPlayer(), message);
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	private boolean hasAnyPermission(Player sender) {
		if(sender.hasPermission("legendchat.admin.channel"))
			return true;
		if(sender.hasPermission("legendchat.admin.spy"))
			return true;
		if(sender.hasPermission("legendchat.admin.hide"))
			return true;
		if(sender.hasPermission("legendchat.admin.mute"))
			return true;
		if(sender.hasPermission("legendchat.admin.unmute"))
			return true;
		if(sender.hasPermission("legendchat.admin.muteall"))
			return true;
		if(sender.hasPermission("legendchat.admin.unmuteall"))
			return true;
		if(sender.hasPermission("legendchat.admin.reload"))
			return true;
		return false;
	}
}