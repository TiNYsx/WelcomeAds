package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import com.tinysx.welcomeads.utils.ColorUtil;
import com.tinysx.welcomeads.utils.CommandConverter;
import com.tinysx.welcomeads.utils.HeadsUtil;

public final class Screen {

    public static final NamespacedKey ADS_ITEM_KEY = new NamespacedKey(WelcomeAds.getInstance(), "adsid");

    private final Map<Integer, String> slotToKeyMap = new HashMap<>();
    private final Map<String, List<String>> commandMap = new HashMap<>();

    public Inventory screenInventory;
    public String index;
    public String container;
    public Integer containerIndex;
    public String title;
    public String background;
    public Integer backgroundStay = 0;
    public Integer backgroundFadeout = 10;
    public Integer backgroundFadein = 0;
    public ConfigurationSection itemsection;
    private final Config config;
    private final WelcomeInventoryHolder holder;
    private final WelcomeAds welcomeads;

    public Screen(String container, Integer containerIndex, Player player) {
        this.welcomeads = WelcomeAds.getInstance();
        this.config = this.welcomeads.config;
        this.container = container;

        List<String> invList = config.loadContainer().getStringList("container." + container + ".inventory");
        if (invList.isEmpty()) {
            this.containerIndex = 0;
            this.index = container;
        } else if (containerIndex == null || containerIndex > invList.size() - 1) {
            if (config.loadContainer().getBoolean("container." + container + ".loop", false)) {
                this.containerIndex = 0;
                this.index = invList.get(0);
            } else {
                this.containerIndex = Math.max(0, invList.size() - 1);
                this.index = invList.get(this.containerIndex);
            }
        } else if (containerIndex < 0) {
            this.containerIndex = 0;
            this.index = invList.get(0);
        } else {
            this.containerIndex = containerIndex;
            this.index = invList.get(this.containerIndex);
        }

        // Background settings
        ConfigurationSection invSec = config.loadInventory().getConfigurationSection("inventory." + this.index);
        if (invSec != null && invSec.contains("background.text")) {
            this.background = invSec.getString("background.text",
                    welcomeads.getConfig().getString("global-background.text", ""));
            this.backgroundStay = invSec.getInt("background.stay",
                    welcomeads.getConfig().getInt("global-background.stay", 0));
            this.backgroundFadeout = invSec.getInt("background.fadeout",
                    welcomeads.getConfig().getInt("global-background.fadeout", 10));
            this.backgroundFadein = invSec.getInt("background.fadein",
                    welcomeads.getConfig().getInt("global-background.fadein", 0));
        } else {
            this.background = welcomeads.getConfig().getString("global-background.text", "");
            this.backgroundStay = welcomeads.getConfig().getInt("global-background.stay", 0);
            this.backgroundFadeout = welcomeads.getConfig().getInt("global-background.fadeout", 10);
            this.backgroundFadein = welcomeads.getConfig().getInt("global-background.fadein", 0);
        }

        String rawTitle = invSec != null ? invSec.getString("title", "") : "";
        this.title = ColorUtil.format(player, "&f" + (this.background != null ? this.background : "") + "&f" + rawTitle);

        this.itemsection = invSec != null ? invSec.getConfigurationSection("items") : null;
        this.holder = new WelcomeInventoryHolder(this);
        this.screenInventory = Bukkit.createInventory(this.holder, 54, this.title);

        buildItems(player);
    }

    private void buildItems(Player player) {
        if (this.itemsection == null) {
            return;
        }

        for (String key : itemsection.getKeys(false)) {
            int slot = itemsection.getInt(key + ".slot");
            if (slot < 0 || slot >= 54) {
                continue;
            }

            slotToKeyMap.put(slot, key);
            List<String> commands = itemsection.getStringList(key + ".commands");
            if (!commands.isEmpty()) {
                commandMap.put(key, commands);
            }

            String itemMaterial = itemsection.getString(key + ".material", "STONE");

            if (itemMaterial.contains("PLAYER_HEAD")) {
                String headOwner = player.getName();
                if (itemMaterial.contains(":")) {
                    headOwner = itemMaterial.split(":")[1];
                }
                final String finalHeadOwner = headOwner;

                // 1. Create a temporary fallback head immediately (zero main thread stall)
                ItemStack tempHead = HeadsUtil.createDefaultHead();
                setupItemMeta(tempHead, key, player);
                this.screenInventory.setItem(slot, tempHead);

                // 2. Fetch the texture asynchronously and update slot safely on the main thread
                HeadsUtil.getPlayerHead(finalHeadOwner, fetchedHead -> {
                    if (fetchedHead != null) {
                        setupItemMeta(fetchedHead, key, player);
                        Bukkit.getScheduler().runTask(this.welcomeads, () -> {
                            if (player.isOnline()
                                    && player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder currentHolder
                                    && currentHolder.getScreen() == this) {
                                this.screenInventory.setItem(slot, fetchedHead);
                            }
                        });
                    }
                });
            } else {
                ItemStack item = createItemSync(key, player);
                if (item != null) {
                    this.screenInventory.setItem(slot, item);
                }
            }
        }
    }

    public boolean handleClick(int slot, Player player) {
        String key = slotToKeyMap.get(slot);
        if (key != null && commandMap.containsKey(key)) {
            List<String> commands = commandMap.get(key);
            if (!commands.isEmpty()) {
                CommandConverter.runStringListCommands(commands, player);
                return true;
            }
        }
        return false;
    }

    private ItemStack createItemSync(String key, Player player) {
        String itemMaterial = itemsection.getString(key + ".material", "STONE");
        Material material = Material.getMaterial(itemMaterial.toUpperCase());
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        return setupItemMeta(item, key, player);
    }

    private ItemStack setupItemMeta(ItemStack item, String key, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String itemName = itemsection.getString(key + ".name");
        if (itemName != null) {
            meta.setDisplayName(ColorUtil.format(player, itemName));
        }

        List<String> itemLore = itemsection.getStringList(key + ".lore");
        if (!itemLore.isEmpty()) {
            List<String> processedLore = new ArrayList<>(itemLore.size());
            for (String line : itemLore) {
                processedLore.add(ColorUtil.format(player, line));
            }
            meta.setLore(processedLore);
        }

        int itemModelData = itemsection.getInt(key + ".modeldata", -1);
        if (itemModelData >= 0) {
            meta.setCustomModelData(itemModelData);
        }

        List<String> flagList = itemsection.getStringList(key + ".flags");
        for (String flagkey : flagList) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagkey));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<String> enchantList = itemsection.getStringList(key + ".enchantments");
        for (String enchantkey : enchantList) {
            String[] eKey = enchantkey.split(":");
            if (eKey.length >= 2) {
                try {
                    Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(eKey[0].toLowerCase()));
                    if (ench != null) {
                        meta.addEnchant(ench, Integer.parseInt(eKey[1]), true);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            Color color = parseDyeColor(key);
            if (color != null) {
                leatherMeta.setColor(color);
            }
        } else if (meta instanceof PotionMeta potionMeta) {
            Color color = parseDyeColor(key);
            if (color != null) {
                potionMeta.setColor(color);
            }
        }

        // Tag item using Spigot native PersistentDataContainer (PDC)
        meta.getPersistentDataContainer().set(ADS_ITEM_KEY, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    private Color parseDyeColor(String key) {
        List<String> dyeList = itemsection.getStringList(key + ".dyes");
        if (dyeList.isEmpty()) {
            return null;
        }
        Color mixedColor = Color.fromRGB(255, 255, 255);
        for (String dyekey : dyeList) {
            String[] dKey = dyekey.split(":");
            if (dKey.length >= 3) {
                try {
                    Color cKey = Color.fromRGB(
                            Integer.parseInt(dKey[0]),
                            Integer.parseInt(dKey[1]),
                            Integer.parseInt(dKey[2]));
                    mixedColor = mixedColor.mixColors(cKey);
                } catch (Exception ignored) {
                }
            }
        }
        return mixedColor;
    }

    public Inventory getInventory() {
        return this.screenInventory;
    }

    public Inventory getScreenInventory(@Nullable Player player) {
        return this.screenInventory;
    }

    public void openTo(Player player) {
        if (this.itemsection == null) {
            player.sendMessage(config.loadLang("screen-config-none", player).replace("<index>", this.index));
            return;
        }

        if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            player.closeInventory();
        }

        String perm = config.loadContainer().getString("container." + this.container + ".permission");
        boolean isPublic = (perm == null || perm.isEmpty() || perm.equalsIgnoreCase("none") || perm.equalsIgnoreCase("default"));
        boolean hasAccess = isPublic
                || player.hasPermission(perm)
                || player.hasPermission("welcomeads.open." + this.index)
                || player.hasPermission("welcomeads.open." + this.container)
                || player.hasPermission("welcomeads.admin")
                || player.hasPermission("welcomeads.open");

        if (hasAccess) {
            // Hide player inventory so no items show on the welcome screen
            SafeInventoryManager.hidePlayerInventory(player);

            // Send visual background overlay if configured
            if (this.background != null && !this.background.isEmpty()) {
                player.sendTitle(ColorUtil.format(player, this.background), "",
                        this.backgroundFadein, this.backgroundStay, this.backgroundFadeout);
            }
            player.openInventory(this.screenInventory);
        } else {
            player.sendMessage(config.loadLang("cmd-perm-none", player));
        }
    }

    public WelcomeInventoryHolder getHolder() {
        return this.holder;
    }

    public String getIndex() {
        return this.index;
    }

    public String getContainer() {
        return this.container;
    }

    public Integer getContainerIndex() {
        return this.containerIndex;
    }

    public String getBackground() {
        return this.background;
    }

    public Integer getBackgroundStay() {
        return this.backgroundStay;
    }

    public Integer getBackgroundFadeout() {
        return this.backgroundFadeout;
    }

    public Integer getBackgroundFadein() {
        return this.backgroundFadein;
    }

    public String getTitle() {
        return this.title;
    }

    public ConfigurationSection getItemSection() {
        return this.itemsection;
    }
}
