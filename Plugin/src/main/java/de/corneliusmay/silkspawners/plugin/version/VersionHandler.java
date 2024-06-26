package de.corneliusmay.silkspawners.plugin.version;

import de.corneliusmay.silkspawners.api.NMS;
import de.corneliusmay.silkspawners.plugin.SilkSpawners;
import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionHandler {

    private final SilkSpawners plugin;

    @Getter
    private final String version;

    @Getter
    private NMS nmsHandler;

    public VersionHandler(SilkSpawners plugin) {
        this.plugin = plugin;
        this.version = getServerVersion();
    }

    public boolean load() {
        try {
            this.nmsHandler = getNMS(this.version);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
            plugin.getLog().error("The detected Server Version (" + this.version + ") is not supported by the currently installed version of SilkSpawners");

            plugin.getLog().info("Currently supported Versions are: " + Arrays.toString(getSupportedVersions()));
            plugin.getLog().info("You can check for updates at https://www.spigotmc.org/resources/silkspawners-versions-1-8-8-1-18-2.60063/");


            plugin.getLog().warn("Disabling plugin due to version incompatibility");
            plugin.getPluginLoader().disablePlugin(plugin);
            return false;
        }

        plugin.getLog().info("Loading support for NMS-Version " + this.version);
        return true;
    }

    private String getServerVersion() {
        // As of Minecraft version 1.20.5, Paper ships with a Mojang-mapped runtime instead of reobfuscating the server
        // to Spigot mappings. This means that the package name of the server implementation is no longer a reliable
        // way to determine the server version. Instead, we can use the Bukkit version string.
        //
        // The following code also means that we don't have to update the plugin for every new Minecraft version
        // unless the Bukkit API changes in a way that explicitly breaks it.
        if(MinecraftVersion.versionIsNewerOrEqualTo(1, 20, 5)) {
            return "v1_20_R4";
        }
        String packageName = plugin.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }


    private NMS getNMS(String version) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Class<?> clazz = Class.forName("de.corneliusmay.silkspawners.nms." + version + ".NMSHandler");
        if(NMS.class.isAssignableFrom(clazz)) {
            return (NMS) clazz.getConstructor().newInstance();
        }

        return null;
    }

    private String[] getSupportedVersions() {
        List<String> versions = new ArrayList<>();
        try {
            CodeSource src = SilkSpawners.class.getProtectionDomain().getCodeSource();
            if(src == null) return new String[]{"Cannot get supported versions"};

            URL jar = src.getLocation();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            zipIterator(zip, (zipEntry -> addPackageName(zipEntry.getName(), versions)));
        } catch (IOException ex) {
            return new String[]{"Cannot get supported versions: " + ex.getMessage()};
        }

        return versions.toArray(new String[0]);
    }

    private void zipIterator(ZipInputStream zip, Consumer<ZipEntry> consumer) throws IOException {
        while (true) {
            ZipEntry e = zip.getNextEntry();
            if (e == null) break;
            consumer.accept(e);
        }
    }

    private void addPackageName(String name, List<String> names) {
        if(!name.startsWith("de/corneliusmay/silkspawners/nms/v") || name.contains(".class")) return;

        String[] nameSplit = name.split("/");
        names.add(nameSplit[nameSplit.length - 1]);
    }
}
