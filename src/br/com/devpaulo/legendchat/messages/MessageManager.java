package br.com.devpaulo.legendchat.messages;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.configuration.file.YamlConfiguration;

import br.com.devpaulo.legendchat.text.TextUtils;

public class MessageManager {
	private HashMap<String,String> msgs = new  HashMap<String,String>();
	private File file = null;
	public MessageManager() {
	}
	
	public void loadMessages(File f) {
		file=f;
		msgs.clear();
		YamlConfiguration msglist = YamlConfiguration.loadConfiguration(f);
		for(String n : msglist.getConfigurationSection("").getKeys(false))
			msgs.put(n.toLowerCase(), msglist.getString(n));
	}
	
	public String getMessage(String msg) {
		return TextUtils.colorizeLegacy(msgs.get(msg.toLowerCase()));
	}
	
	public void addMessageToFile(String name, String msg) {
		YamlConfiguration msglist = YamlConfiguration.loadConfiguration(file);
		msglist.set(name, msg);
		try {
			msglist.save(file);
		} catch (IOException e) {e.printStackTrace();}
	}
	
	public boolean hasMessage(String name) {
		return msgs.containsKey(name.toLowerCase());
	}
	
	public void registerLanguageFile(File f) {
		file=f;
	}
	
}