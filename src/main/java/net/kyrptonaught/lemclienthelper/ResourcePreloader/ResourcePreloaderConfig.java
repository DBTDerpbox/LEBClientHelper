package net.kyrptonaught.lemclienthelper.ResourcePreloader;

import blue.endless.jankson.Comment;
import net.kyrptonaught.kyrptconfig.config.AbstractConfigFile;

public class ResourcePreloaderConfig implements AbstractConfigFile {
    public static final String DEFAULT_URL = "https://raw.githubusercontent.com/DBTDerpbox/Legacy-Edition-Battle/main/config/switchableresourcepacksconfig.json5";
    @Comment("URL to download the list of packs from")
    public String URL = DEFAULT_URL;

    @Comment("Should download optifine version of packs")
    public boolean allowOptifine = false;

    @Comment("Allow Simultaneous Downloads")
    public boolean multiDownload = true;

    @Comment("Should display toast's for completed downloads")
    public boolean toastComplete = true;
}
