package net.kyrptonaught.lemclienthelper.ResourcePreloader;

import blue.endless.jankson.Jankson;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import net.kyrptonaught.lemclienthelper.LEMClientHelperMod;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

public class ResourcePreloader {
    public static String MOD_ID = "resourcepreloader";
    public static AllPacks allPacks;
    private static long startTime;
    private static boolean downloadsComplete = false;

    public static void init() {
        LEMClientHelperMod.configManager.registerFile(MOD_ID, new ResourcePreloaderConfig());
    }

    public static ResourcePreloaderConfig getConfig() {
        return (ResourcePreloaderConfig) LEMClientHelperMod.configManager.getConfig(MOD_ID);

    }

    public static void getPackList() {
        try {
            URL url = new URL(getConfig().URL);
            String downloaded = IOUtils.toString(url.openStream());
            allPacks = Jankson.builder().build().fromJson(downloaded, AllPacks.class);
            allPacks.packs.forEach(rpOption -> {
                download(rpOption, true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadPacks() {
        downloadsComplete = false;
        startTime = System.currentTimeMillis();
        if (getConfig().multiDownload)
            allPacks.packs.forEach(rpOption -> download(rpOption, false));
        else downloadNextPack();
    }

    static void downloadNextPack() {
        for (AllPacks.RPOption rpOption : allPacks.packs) {
            if (!rpOption.progressListener.completed) {
                download(rpOption, false);
                return;
            }
        }
    }

    static void downloadComplete(AllPacks.RPOption rpOption) {
        if (getConfig().toastComplete)
            SystemToast.show(MinecraftClient.getInstance().getToastManager(), SystemToast.Type.TUTORIAL_HINT, rpOption.progressListener.title, new LiteralText(rpOption.packname));
        if (!getConfig().multiDownload)
            downloadNextPack();
        if (verifyFile(rpOption.hash, rpOption.downloadedFile)) {
            //todo implement this
        }
        checkIfComplete();
    }

    private static void checkIfComplete() {
        if (downloadsComplete) return;
        for (AllPacks.RPOption rpOption : allPacks.packs) {
            if (!rpOption.progressListener.completed) {
                return;
            }
        }
        downloadsComplete = true;
        SystemToast.add(MinecraftClient.getInstance().getToastManager(), SystemToast.Type.TUTORIAL_HINT, new TranslatableText("key.lemclienthelper.alldownloadcomplete"), new LiteralText("Took: " + (System.currentTimeMillis() - startTime) + " milliseconds"));
    }

    private static void download(AllPacks.RPOption rpOption, boolean previewOnly) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        String urlHash = DigestUtils.sha1Hex(rpOption.url);
        File file = new File(new File(minecraftClient.runDirectory, "server-resource-packs"), urlHash);
        rpOption.downloadedFile = file;
        if ((getConfig().allowOptifine && rpOption.packCompatibility == AllPacks.RPOption.PACKCOMPATIBILITY.VANILLA) ||
                (!getConfig().allowOptifine && rpOption.packCompatibility == AllPacks.RPOption.PACKCOMPATIBILITY.OPTIFINE)) {
            rpOption.progressListener.skip(new TranslatableText("key.lemclienthelper.wrongpackcompatibility"));
        } else if (file.exists()) {
            rpOption.progressListener.skip(new TranslatableText("key.lemclienthelper.alreadydownloaded"));
        } else if (!previewOnly) {
            Map<String, String> map = getDownloadHeaders();
            NetworkUtils.downloadResourcePack(file, rpOption.url, map, Integer.MAX_VALUE, rpOption.progressListener, minecraftClient.getNetworkProxy());
        }
    }

    private static boolean verifyFile(String expectedSha1, File file) {
        try {
            String string = com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256()).toString();
            if (expectedSha1.isEmpty()) {
                //LOGGER.info("Found file {} without verification hash", (Object)file);
                return true;
            }
            if (string.toLowerCase(Locale.ROOT).equals(expectedSha1.toLowerCase(Locale.ROOT))) {
                //LOGGER.info("Found file {} matching requested hash {}", (Object)file, (Object)expectedSha1);
                return true;
            }
            // LOGGER.warn("File {} had wrong hash (expected {}, found {}).", file, expectedSha1, string);
        } catch (IOException iOException) {
            // LOGGER.warn("File {} couldn't be hashed.", (Object)file, (Object)iOException);
        }
        return false;
    }

    private static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = Maps.newHashMap();
        map.put("X-Minecraft-Username", MinecraftClient.getInstance().getSession().getUsername());
        map.put("X-Minecraft-UUID", MinecraftClient.getInstance().getSession().getUuid());
        map.put("X-Minecraft-Version", SharedConstants.getGameVersion().getName());
        map.put("X-Minecraft-Version-ID", SharedConstants.getGameVersion().getId());
        map.put("X-Minecraft-Pack-Format", String.valueOf(ResourceType.CLIENT_RESOURCES.getPackVersion(SharedConstants.getGameVersion())));
        map.put("User-Agent", "Minecraft Java/" + SharedConstants.getGameVersion().getName());
        return map;
    }
}