package com.gmail.filoghost.customjoinitems;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Updater {
    private Plugin plugin;
    private UpdateType type;
    private String versionName;
    private String versionLink;
    private String versionType;
    private String versionGameVersion;
    private boolean announce;
    private URL url;
    private File file;
    private Thread thread;
    private int id = -1;
    private String apiKey = null;
    private static final String TITLE_VALUE = "name";
    private static final String LINK_VALUE = "downloadUrl";
    private static final String TYPE_VALUE = "releaseType";
    private static final String VERSION_VALUE = "gameVersion";
    private static final String QUERY = "/servermods/files?projectIds=";
    private static final String HOST = "https://api.curseforge.com";
    private static final String[] NO_UPDATE_TAG = new String[]{"-DEV", "-PRE", "-SNAPSHOT"};
    private static final int BYTE_SIZE = 1024;
    private YamlConfiguration config;
    private String updateFolder;
    private UpdateResult result = UpdateResult.SUCCESS;

    public Updater(Plugin plugin, int id, File file, UpdateType type, boolean announce) {
        this.plugin = plugin;
        this.type = type;
        this.announce = announce;
        this.file = file;
        this.id = id;
        this.updateFolder = plugin.getServer().getUpdateFolder();
        File pluginFile = plugin.getDataFolder().getParentFile();
        File updaterFile = new File(pluginFile, "Updater");
        File updaterConfigFile = new File(updaterFile, "config.yml");
        if (!updaterFile.exists()) {
            updaterFile.mkdir();
        }
        if (!updaterConfigFile.exists()) {
            try {
                updaterConfigFile.createNewFile();
            }
            catch (IOException e) {
                plugin.getLogger().severe("The updater could not create a configuration in " + updaterFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration((File)updaterConfigFile);
        this.config.options().header("This configuration file affects all plugins using the Updater system (version 2+ - http://forums.bukkit.org/threads/96681/ )\nIf you wish to use your API key, read http://wiki.bukkit.org/ServerMods_API and place it below.\nSome updating systems will not adhere to the disabled value, but these may be turned off in their plugin's configuration.");
        this.config.addDefault("api-key", (Object)"PUT_API_KEY_HERE");
        this.config.addDefault("disable", (Object)false);
        if (this.config.get("api-key", null) == null) {
            this.config.options().copyDefaults(true);
            try {
                this.config.save(updaterConfigFile);
            }
            catch (IOException e) {
                plugin.getLogger().severe("The updater could not save the configuration in " + updaterFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
        if (this.config.getBoolean("disable")) {
            this.result = UpdateResult.DISABLED;
            return;
        }
        String key = this.config.getString("api-key");
        if (key.equalsIgnoreCase("PUT_API_KEY_HERE") || key.equals("")) {
            key = null;
        }
        this.apiKey = key;
        try {
            this.url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + id);
        }
        catch (MalformedURLException e) {
            plugin.getLogger().severe("The project ID provided for updating, " + id + " is invalid.");
            this.result = UpdateResult.FAIL_BADID;
            e.printStackTrace();
        }
        this.thread = new Thread(new UpdateRunnable());
        this.thread.start();
    }

    public UpdateResult getResult() {
        this.waitForThread();
        return this.result;
    }

    public String getLatestType() {
        this.waitForThread();
        return this.versionType;
    }

    public String getLatestGameVersion() {
        this.waitForThread();
        return this.versionGameVersion;
    }

    public String getLatestName() {
        this.waitForThread();
        return this.versionName;
    }

    public String getLatestFileLink() {
        this.waitForThread();
        return this.versionLink;
    }

    private void waitForThread() {
        if (this.thread != null && this.thread.isAlive()) {
            try {
                this.thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(File folder, String file, String u) {
        if (!folder.exists()) {
            folder.mkdir();
        }
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            try {
                int count;
                URL url = new URL(u);
                int fileLength = url.openConnection().getContentLength();
                in = new BufferedInputStream(url.openStream());
                fout = new FileOutputStream(String.valueOf(folder.getAbsolutePath()) + "/" + file);
                byte[] data = new byte[1024];
                if (this.announce) {
                    this.plugin.getLogger().info("About to download a new update: " + this.versionName);
                }
                long downloaded = 0L;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    fout.write(data, 0, count);
                    int percent = (int)((downloaded += (long)count) * 100L / (long)fileLength);
                    if (!this.announce || percent % 10 != 0) continue;
                    this.plugin.getLogger().info("Downloading update: " + percent + "% of " + fileLength + " bytes.");
                }
                File[] fileArray = new File(this.plugin.getDataFolder().getParent(), this.updateFolder).listFiles();
                int n = fileArray.length;
                int n2 = 0;
                while (n2 < n) {
                    File xFile = fileArray[n2];
                    if (xFile.getName().endsWith(".zip")) {
                        xFile.delete();
                    }
                    ++n2;
                }
                File dFile = new File(String.valueOf(folder.getAbsolutePath()) + "/" + file);
                if (dFile.getName().endsWith(".zip")) {
                    this.unzip(dFile.getCanonicalPath());
                }
                if (this.announce) {
                    this.plugin.getLogger().info("Finished updating.");
                }
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("The auto-updater tried to download a new update, but was unsuccessful.");
                this.result = UpdateResult.FAIL_DOWNLOAD;
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (fout != null) {
                        fout.close();
                    }
                }
                catch (Exception exception) {}
            }
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (fout != null) {
                    fout.close();
                }
            }
            catch (Exception exception) {}
        }
    }

    private void unzip(String file) {
        try {
            File fSourceZip = new File(file);
            String zipPath = file.substring(0, file.length() - 4);
            ZipFile zipFile = new ZipFile(fSourceZip);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                int b;
                ZipEntry entry = e.nextElement();
                File destinationFilePath = new File(zipPath, entry.getName());
                destinationFilePath.getParentFile().mkdirs();
                if (entry.isDirectory()) continue;
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                byte[] buffer = new byte[1024];
                FileOutputStream fos = new FileOutputStream(destinationFilePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);
                while ((b = bis.read(buffer, 0, 1024)) != -1) {
                    bos.write(buffer, 0, b);
                }
                bos.flush();
                bos.close();
                bis.close();
                String name = destinationFilePath.getName();
                if (name.endsWith(".jar") && this.pluginFile(name)) {
                    destinationFilePath.renameTo(new File(this.plugin.getDataFolder().getParent(), String.valueOf(this.updateFolder) + "/" + name));
                }
                entry = null;
                destinationFilePath = null;
            }
            e = null;
            zipFile.close();
            zipFile = null;
            File[] fileArray = new File(zipPath).listFiles();
            int n = fileArray.length;
            int n2 = 0;
            while (n2 < n) {
                File dFile = fileArray[n2];
                if (dFile.isDirectory() && this.pluginFile(dFile.getName())) {
                    File oFile = new File(this.plugin.getDataFolder().getParent(), dFile.getName());
                    File[] contents = oFile.listFiles();
                    File[] fileArray2 = dFile.listFiles();
                    int n3 = fileArray2.length;
                    int n4 = 0;
                    while (n4 < n3) {
                        File cFile = fileArray2[n4];
                        boolean found = false;
                        File[] fileArray3 = contents;
                        int n5 = contents.length;
                        int n6 = 0;
                        while (n6 < n5) {
                            File xFile = fileArray3[n6];
                            if (xFile.getName().equals(cFile.getName())) {
                                found = true;
                                break;
                            }
                            ++n6;
                        }
                        if (!found) {
                            cFile.renameTo(new File(oFile.getCanonicalFile() + "/" + cFile.getName()));
                        } else {
                            cFile.delete();
                        }
                        ++n4;
                    }
                }
                dFile.delete();
                ++n2;
            }
            new File(zipPath).delete();
            fSourceZip.delete();
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("The auto-updater tried to unzip a new update file, but was unsuccessful.");
            this.result = UpdateResult.FAIL_DOWNLOAD;
            ex.printStackTrace();
        }
        new File(file).delete();
    }

    private boolean pluginFile(String name) {
        File[] fileArray = new File("plugins").listFiles();
        int n = fileArray.length;
        int n2 = 0;
        while (n2 < n) {
            File file = fileArray[n2];
            if (file.getName().equals(name)) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    private boolean versionCheck(String title) {
        if (this.type != UpdateType.NO_VERSION_CHECK) {
            String version = this.plugin.getDescription().getVersion();
            if (title.split(" v").length == 2) {
                String remoteVersion = title.split(" v")[1].split(" ")[0];
                if (this.hasTag(version) || version.equalsIgnoreCase(remoteVersion)) {
                    this.result = UpdateResult.NO_UPDATE;
                    return false;
                }
            } else {
                String authorInfo = this.plugin.getDescription().getAuthors().size() == 0 ? "" : " (" + (String)this.plugin.getDescription().getAuthors().get(0) + ")";
                this.plugin.getLogger().warning("The author of this plugin" + authorInfo + " has misconfigured their Auto Update system");
                this.plugin.getLogger().warning("File versions should follow the format 'PluginName vVERSION'");
                this.plugin.getLogger().warning("Please notify the author of this error.");
                this.result = UpdateResult.FAIL_NOVERSION;
                return false;
            }
        }
        return true;
    }

    private boolean hasTag(String version) {
        String[] stringArray = NO_UPDATE_TAG;
        int n = NO_UPDATE_TAG.length;
        int n2 = 0;
        while (n2 < n) {
            String string = stringArray[n2];
            if (version.contains(string)) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    private boolean read() {
        JSONArray array;
        block6: {
            try {
                URLConnection conn = this.url.openConnection();
                conn.setConnectTimeout(5000);
                if (this.apiKey != null) {
                    conn.addRequestProperty("X-API-Key", this.apiKey);
                }
                conn.addRequestProperty("User-Agent", "Updater (by Gravity)");
                conn.setDoOutput(true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                array = (JSONArray)JSONValue.parse((String)response);
                if (array.size() != 0) break block6;
                this.plugin.getLogger().warning("The updater could not find any files for the project id " + this.id);
                this.result = UpdateResult.FAIL_BADID;
                return false;
            }
            catch (IOException e) {
                if (e.getMessage().contains("HTTP response code: 403")) {
                    this.plugin.getLogger().warning("dev.bukkit.org rejected the API key provided in plugins/Updater/config.yml");
                    this.plugin.getLogger().warning("Please double-check your configuration to ensure it is correct.");
                    this.result = UpdateResult.FAIL_APIKEY;
                } else {
                    this.plugin.getLogger().warning("The updater could not contact dev.bukkit.org for updating.");
                    this.plugin.getLogger().warning("If you have not recently modified your configuration and this is the first time you are seeing this message, the site may be experiencing temporary downtime.");
                    this.result = UpdateResult.FAIL_DBO;
                }
                e.printStackTrace();
                return false;
            }
        }
        this.versionName = (String)((JSONObject)array.get(array.size() - 1)).get((Object)TITLE_VALUE);
        this.versionLink = (String)((JSONObject)array.get(array.size() - 1)).get((Object)LINK_VALUE);
        this.versionType = (String)((JSONObject)array.get(array.size() - 1)).get((Object)TYPE_VALUE);
        this.versionGameVersion = (String)((JSONObject)array.get(array.size() - 1)).get((Object)VERSION_VALUE);
        return true;
    }

    public static enum UpdateResult {
        SUCCESS,
        NO_UPDATE,
        DISABLED,
        FAIL_DOWNLOAD,
        FAIL_DBO,
        FAIL_NOVERSION,
        FAIL_BADID,
        FAIL_APIKEY,
        UPDATE_AVAILABLE;

    }

    private class UpdateRunnable
    implements Runnable {
        private UpdateRunnable() {
        }

        @Override
        public void run() {
            if (Updater.this.url != null && Updater.this.read() && Updater.this.versionCheck(Updater.this.versionName)) {
                if (Updater.this.versionLink != null && Updater.this.type != UpdateType.NO_DOWNLOAD) {
                    String name = Updater.this.file.getName();
                    if (Updater.this.versionLink.endsWith(".zip")) {
                        String[] split = Updater.this.versionLink.split("/");
                        name = split[split.length - 1];
                    }
                    Updater.this.saveFile(new File(Updater.this.plugin.getDataFolder().getParent(), Updater.this.updateFolder), name, Updater.this.versionLink);
                } else {
                    Updater.this.result = UpdateResult.UPDATE_AVAILABLE;
                }
            }
        }
    }

    public static enum UpdateType {
        DEFAULT,
        NO_VERSION_CHECK,
        NO_DOWNLOAD;

    }

    public static class UpdaterHandler {
        public static boolean updateFound = false;
        public static String updateVersion = "unknown";
        public static boolean updateAlreadyDownloaded = false;
        private static Plugin plugin;
        private static int projectId;
        private static String pluginChatPrefix;
        private static File pluginFile;
        private static ChatColor primaryColor;
        private static String updateCommand;
        private static String bukkitDevSlug;

        public static void setup(Plugin plugin, int projectId, String pluginChatPrefix, File pluginFile, ChatColor primaryColor, String updateCommand, String bukkitDevSlug) {
            UpdaterHandler.plugin = plugin;
            UpdaterHandler.projectId = projectId;
            UpdaterHandler.pluginChatPrefix = pluginChatPrefix;
            UpdaterHandler.pluginFile = pluginFile;
            UpdaterHandler.primaryColor = primaryColor;
            UpdaterHandler.updateCommand = updateCommand;
            UpdaterHandler.bukkitDevSlug = bukkitDevSlug;
        }

        public static void notifyIfUpdateWasFound(final Player player, String updatePermission) {
            if (updateFound && !updateAlreadyDownloaded && player.hasPermission(updatePermission)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

                    @Override
                    public void run() {
                        player.sendMessage(String.valueOf(pluginChatPrefix) + primaryColor + "Found an update: v" + updateVersion + "   \u00a77(Your version: v" + plugin.getDescription().getVersion() + ")");
                        player.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77Type \"" + primaryColor + updateCommand + "\u00a77\" or download it from:");
                        player.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77dev.bukkit.org/bukkit-plugins/" + bukkitDevSlug);
                    }
                }, 10L);
            }
        }

        public static void startupUpdateCheck() {
            if (plugin == null) {
                try {
                    throw new Exception("The developer did not setup the updater correctly");
                }
                catch (Exception continueRuntime) {
                    continueRuntime.printStackTrace();
                    return;
                }
            }
            Updater updater = new Updater(plugin, projectId, pluginFile, UpdateType.NO_DOWNLOAD, true);
            if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                updateFound = true;
                if (updater.getLatestName().split(" v").length == 2) {
                    updateVersion = updater.getLatestName().split(" v")[1].split(" ")[0];
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

                    @Override
                    public void run() {
                        Bukkit.getConsoleSender().sendMessage(String.valueOf(pluginChatPrefix) + primaryColor + "Found an update: v" + updateVersion + "   \u00a7f(Your version: v" + plugin.getDescription().getVersion() + ")");
                        Bukkit.getConsoleSender().sendMessage(String.valueOf(pluginChatPrefix) + "\u00a7fType \"" + primaryColor + updateCommand + "\u00a7f\" or download it from:");
                        Bukkit.getConsoleSender().sendMessage(String.valueOf(pluginChatPrefix) + "\u00a7fdev.bukkit.org/bukkit-plugins/" + bukkitDevSlug);
                    }
                }, 1L);
            }
        }

        public static void manuallyCheckUpdates(CommandSender sender) {
            if (plugin == null) {
                try {
                    throw new Exception("The developer did not setup the updater correctly");
                }
                catch (Exception continueRuntime) {
                    continueRuntime.printStackTrace();
                    return;
                }
            }
            sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77Please wait while the plugin is searching for updates. If it finds one, you will see the progress on the console.");
            Updater updater = new Updater(plugin, projectId, pluginFile, UpdateType.DEFAULT, true);
            switch (updater.getResult()) {
                case SUCCESS: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The update will be loaded on the next server startup.");
                    updateAlreadyDownloaded = true;
                    break;
                }
                case DISABLED: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The updater is disabled. If you want to enable it, edit /plugins/Updater/config.yml accordingly.");
                    break;
                }
                case FAIL_APIKEY: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77You provided an invalid API key for the updater to use (/plugin/Updater/config.yml).");
                    break;
                }
                case FAIL_BADID: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The project ID didn't exist. Please contact the developer.");
                    break;
                }
                case FAIL_DBO: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The updater was unable to contact dev.bukkit.org. Please retry later.");
                    break;
                }
                case FAIL_DOWNLOAD: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The updater failed to download the update. Please download the file manually.");
                    break;
                }
                case FAIL_NOVERSION: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The latest file on dev.bukkit.org did not contain a version. Please manually check for updates and contact the developer.");
                    break;
                }
                case NO_UPDATE: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The plugin is already updated.");
                    break;
                }
                case UPDATE_AVAILABLE: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The update is ready, but has not been downloaded yet.");
                    break;
                }
                default: {
                    sender.sendMessage(String.valueOf(pluginChatPrefix) + "\u00a77The updater encountered an unexpected error. Check the console and retry later.");
                }
            }
        }
    }
}

