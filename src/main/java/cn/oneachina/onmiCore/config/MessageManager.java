package cn.oneachina.onmiCore.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageManager {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages = new HashMap<>();
    private Locale currentLocale = Locale.CHINESE;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        messages.clear();

        File zhFile = new File(plugin.getDataFolder(), "messages_zh.yml");
        File enFile = new File(plugin.getDataFolder(), "messages_en.yml");

        if (!zhFile.exists()) plugin.saveResource("messages_zh.yml", false);
        if (!enFile.exists()) plugin.saveResource("messages_en.yml", false);

        currentLocale = detectLocale();

        YamlConfiguration langConfig;
        String langFile = currentLocale == Locale.CHINESE ? "messages_zh.yml" : "messages_en.yml";
        File langPath = new File(plugin.getDataFolder(), langFile);
        langConfig = YamlConfiguration.loadConfiguration(langPath);

        // Load defaults from jar
        try (InputStream defStream = plugin.getResource(langFile)) {
            if (defStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defConfig);
            }
        } catch (Exception e) {
            plugin.getSLF4JLogger().warn("Failed to load default messages", e);
        }

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                messages.put(key, langConfig.getString(key));
            }
        }
    }

    private Locale detectLocale() {
        String lang = plugin.getConfig().getString("language", "zh");
        return lang.equalsIgnoreCase("en") ? Locale.ENGLISH : Locale.CHINESE;
    }

    public Component get(String key, Object... args) {
        String template = messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
        if (args.length > 0) {
            template = new MessageFormat(template.replace("'", "''")).format(args);
        }
        return miniMessage.deserialize(template);
    }

    public String raw(String key) {
        return messages.getOrDefault(key, "Missing message: " + key);
    }
}
