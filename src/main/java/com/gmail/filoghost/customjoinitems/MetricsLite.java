package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.VersionUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

public class MetricsLite {
    private static final int REVISION = 7;
    private static final String BASE_URL = "http://report.mcstats.org";
    private static final String REPORT_URL = "/plugin/%s";
    private static final int PING_INTERVAL = 15;
    private final Plugin plugin;
    private final YamlConfiguration configuration;
    private final File configurationFile;
    private final String guid;
    private final boolean debug;
    private final Object optOutLock = new Object();
    private volatile BukkitTask task = null;

    public MetricsLite(Plugin plugin) throws IOException {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.configurationFile = this.getConfigFile();
        this.configuration = YamlConfiguration.loadConfiguration((File)this.configurationFile);
        this.configuration.addDefault("opt-out", (Object)false);
        this.configuration.addDefault("guid", (Object)UUID.randomUUID().toString());
        this.configuration.addDefault("debug", (Object)false);
        if (this.configuration.get("guid", null) == null) {
            this.configuration.options().header("http://mcstats.org").copyDefaults(true);
            this.configuration.save(this.configurationFile);
        }
        this.guid = this.configuration.getString("guid");
        this.debug = this.configuration.getBoolean("debug", false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean start() {
        Object object = this.optOutLock;
        synchronized (object) {
            block6: {
                block5: {
                    if (!this.isOptOut()) break block5;
                    return false;
                }
                if (this.task == null) break block6;
                return true;
            }
            this.task = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable(){
                private boolean firstPost = true;

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public void run() {
                    block6: {
                        try {
                            Object object = MetricsLite.this.optOutLock;
                            synchronized (object) {
                                if (MetricsLite.this.isOptOut() && MetricsLite.this.task != null) {
                                    MetricsLite.this.task.cancel();
                                    MetricsLite.this.task = null;
                                }
                            }
                            MetricsLite.this.postPlugin(!this.firstPost);
                            this.firstPost = false;
                        }
                        catch (IOException e) {
                            if (!MetricsLite.this.debug) break block6;
                            Bukkit.getLogger().log(Level.INFO, "[Metrics] " + e.getMessage());
                        }
                    }
                }
            }, 0L, 18000L);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean isOptOut() {
        Object object = this.optOutLock;
        synchronized (object) {
            try {
                this.configuration.load(this.getConfigFile());
            }
            catch (IOException ex) {
                if (this.debug) {
                    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
                }
                return true;
            }
            catch (InvalidConfigurationException ex) {
                if (this.debug) {
                    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
                }
                return true;
            }
            return this.configuration.getBoolean("opt-out", false);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void enable() throws IOException {
        Object object = this.optOutLock;
        synchronized (object) {
            if (this.isOptOut()) {
                this.configuration.set("opt-out", (Object)false);
                this.configuration.save(this.configurationFile);
            }
            if (this.task == null) {
                this.start();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void disable() throws IOException {
        Object object = this.optOutLock;
        synchronized (object) {
            if (!this.isOptOut()) {
                this.configuration.set("opt-out", (Object)true);
                this.configuration.save(this.configurationFile);
            }
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
        }
    }

    public File getConfigFile() {
        File pluginsFolder = this.plugin.getDataFolder().getParentFile();
        return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
    }

    private void postPlugin(boolean isPing) throws IOException {
        PluginDescriptionFile description = this.plugin.getDescription();
        String pluginName = description.getName();
        boolean onlineMode = Bukkit.getServer().getOnlineMode();
        String pluginVersion = description.getVersion();
        String serverVersion = Bukkit.getVersion();
        int playersOnline = VersionUtils.getOnlinePlayers().size();
        StringBuilder json = new StringBuilder(1024);
        json.append('{');
        MetricsLite.appendJSONPair(json, "guid", this.guid);
        MetricsLite.appendJSONPair(json, "plugin_version", pluginVersion);
        MetricsLite.appendJSONPair(json, "server_version", serverVersion);
        MetricsLite.appendJSONPair(json, "players_online", Integer.toString(playersOnline));
        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
        String osversion = System.getProperty("os.version");
        String java_version = System.getProperty("java.version");
        int coreCount = Runtime.getRuntime().availableProcessors();
        if (osarch.equals("amd64")) {
            osarch = "x86_64";
        }
        MetricsLite.appendJSONPair(json, "osname", osname);
        MetricsLite.appendJSONPair(json, "osarch", osarch);
        MetricsLite.appendJSONPair(json, "osversion", osversion);
        MetricsLite.appendJSONPair(json, "cores", Integer.toString(coreCount));
        MetricsLite.appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
        MetricsLite.appendJSONPair(json, "java_version", java_version);
        if (isPing) {
            MetricsLite.appendJSONPair(json, "ping", "1");
        }
        json.append('}');
        URL url = new URL(BASE_URL + String.format(REPORT_URL, MetricsLite.urlEncode(pluginName)));
        URLConnection connection = this.isMineshafterPresent() ? url.openConnection(Proxy.NO_PROXY) : url.openConnection();
        byte[] uncompressed = json.toString().getBytes();
        byte[] compressed = MetricsLite.gzip(json.toString());
        connection.addRequestProperty("User-Agent", "MCStats/7");
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.setDoOutput(true);
        if (this.debug) {
            System.out.println("[Metrics] Prepared request for " + pluginName + " uncompressed=" + uncompressed.length + " compressed=" + compressed.length);
        }
        OutputStream os = connection.getOutputStream();
        os.write(compressed);
        os.flush();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = reader.readLine();
        os.close();
        reader.close();
        if (response == null || response.startsWith("ERR") || response.startsWith("7")) {
            if (response == null) {
                response = "null";
            } else if (response.startsWith("7")) {
                response = response.substring(response.startsWith("7,") ? 2 : 1);
            }
            throw new IOException(response);
        }
    }

    public static byte[] gzip(String input) {
        ByteArrayOutputStream baos;
        block14: {
            baos = new ByteArrayOutputStream();
            GZIPOutputStream gzos = null;
            try {
                try {
                    gzos = new GZIPOutputStream(baos);
                    gzos.write(input.getBytes("UTF-8"));
                }
                catch (IOException e) {
                    e.printStackTrace();
                    if (gzos != null) {
                        try {
                            gzos.close();
                        }
                        catch (IOException iOException) {}
                    }
                    break block14;
                }
            }
            catch (Throwable throwable) {
                if (gzos != null) {
                    try {
                        gzos.close();
                    }
                    catch (IOException iOException) {
                        // empty catch block
                    }
                }
                throw throwable;
            }
            if (gzos != null) {
                try {
                    gzos.close();
                }
                catch (IOException iOException) {
                    // empty catch block
                }
            }
        }
        return baos.toByteArray();
    }

    private boolean isMineshafterPresent() {
        try {
            Class.forName("mineshafter.MineServer");
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private static void appendJSONPair(StringBuilder json, String key, String value) throws UnsupportedEncodingException {
        boolean isValueNumeric = false;
        try {
            if (value.equals("0") || !value.endsWith("0")) {
                Double.parseDouble(value);
                isValueNumeric = true;
            }
        }
        catch (NumberFormatException e) {
            isValueNumeric = false;
        }
        if (json.charAt(json.length() - 1) != '{') {
            json.append(',');
        }
        json.append(MetricsLite.escapeJSON(key));
        json.append(':');
        if (isValueNumeric) {
            json.append(value);
        } else {
            json.append(MetricsLite.escapeJSON(value));
        }
    }

    private static String escapeJSON(String text) {
        StringBuilder builder = new StringBuilder();
        builder.append('\"');
        int index = 0;
        while (index < text.length()) {
            char chr = text.charAt(index);
            switch (chr) {
                case '\"': 
                case '\\': {
                    builder.append('\\');
                    builder.append(chr);
                    break;
                }
                case '\b': {
                    builder.append("\\b");
                    break;
                }
                case '\t': {
                    builder.append("\\t");
                    break;
                }
                case '\n': {
                    builder.append("\\n");
                    break;
                }
                case '\r': {
                    builder.append("\\r");
                    break;
                }
                default: {
                    if (chr < ' ') {
                        String t = "000" + Integer.toHexString(chr);
                        builder.append("\\u" + t.substring(t.length() - 4));
                        break;
                    }
                    builder.append(chr);
                }
            }
            ++index;
        }
        builder.append('\"');
        return builder.toString();
    }

    private static String urlEncode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }
}

