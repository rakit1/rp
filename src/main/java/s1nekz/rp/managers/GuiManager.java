package s1nekz.rp.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import s1nekz.rp.managers.FactionManager.Faction;

import java.util.*;
import java.util.stream.Collectors;

public class GuiManager {
    public static final Map<UUID, UUID> interactionTargets = new HashMap<>();
    private final FactionManager factionManager;

    public GuiManager(FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    public void openInteractionMenu(Player interactor, Player target) {
        interactionTargets.put(interactor.getUniqueId(), target.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 18, "§8Действия с " + target.getName());

        boolean canCuff = factionManager.playerHasPermission(interactor.getUniqueId(), "cuff");
        boolean canSearch = factionManager.playerHasPermission(interactor.getUniqueId(), "search");
        boolean canFollow = factionManager.playerHasPermission(interactor.getUniqueId(), "follow");
        if (canCuff) menu.setItem(0, createGuiItem(Material.IRON_BARS, "§eНадеть/снять наручники"));
        if (canSearch) menu.setItem(1, createGuiItem(Material.CHEST, "§bОбыскать игрока"));
        if (canFollow) menu.setItem(2, createGuiItem(Material.LEAD, "§aВести/отпустить за собой"));

        Faction leaderFaction = factionManager.getLeaderFaction(interactor.getUniqueId());
        if (leaderFaction != null) {
            Faction targetFaction = factionManager.getPlayerFaction(target.getUniqueId());
            if (targetFaction == null) {
                menu.setItem(16, createGuiItem(Material.EMERALD, "§aПригласить в организацию"));
            } else if (targetFaction.getId().equals(leaderFaction.getId())) {
                menu.setItem(17, createGuiItem(Material.NETHER_STAR, "§6Управление сотрудником"));
            }
        }
        interactor.openInventory(menu);
    }

    public void openLeaderMenu(Player player, Faction faction) {
        Inventory lmenu = Bukkit.createInventory(null, 27, "§1Меню лидера: " + faction.getName());
        lmenu.setItem(10, createGuiItem(Material.NAME_TAG, "§eУправление рангами", "§7Изменить названия и", "§7количество рангов"));
        lmenu.setItem(12, createGuiItem(Material.PLAYER_HEAD, "§aУправление составом", "§7Повысить, понизить,", "§7выдать выговор, уволить"));
        lmenu.setItem(14, createGuiItem(Material.SPYGLASS, "§bОнлайн фракции", "§7Посмотреть, кто из", "§7вашей фракции в сети"));
        lmenu.setItem(16, createGuiItem(Material.WRITABLE_BOOK, "§6Настройка прав (ФБР)", "§7Настроить доступ к", "§7командам для рангов"));
        player.openInventory(lmenu);
    }

    public void openMemberListMenu(Player player, Faction faction, int page) {
        List<OfflinePlayer> allMembers = new ArrayList<>();
        faction.getMembers().keySet().forEach(uuid -> allMembers.add(Bukkit.getOfflinePlayer(uuid)));
        if (faction.getLeader() != null) allMembers.add(Bukkit.getOfflinePlayer(faction.getLeader()));
        allMembers.sort(Comparator.comparingInt(p -> faction.getRank(((OfflinePlayer) p).getUniqueId())).reversed());

        int maxItemsPerPage = 28, startIndex = page * maxItemsPerPage;
        int totalPages = Math.max(1, (int) Math.ceil((double) allMembers.size() / maxItemsPerPage));
        Inventory menu = Bukkit.createInventory(null, 36, "§8Состав: " + faction.getName() + " (" + (page + 1) + "/" + totalPages + ")");

        for (int i = 0; i < maxItemsPerPage; i++) {
            if (startIndex + i >= allMembers.size()) break;
            OfflinePlayer member = allMembers.get(startIndex + i);
            String rankName = faction.getRankName(faction.getRank(member.getUniqueId()));
            menu.addItem(createPlayerHead(member, member.isOnline() ? "§a" + member.getName() : "§c" + member.getName(), "§7Ранг: §f" + rankName, "§7Статус: " + (member.isOnline() ? "§aВ сети" : "§cНе в сети"), "§7Выговоры: §e" + faction.getWarnings(member.getUniqueId()), "", "§aНажмите, чтобы управлять"));
        }

        if (page > 0) menu.setItem(27, createGuiItem(Material.ARROW, "§cПредыдущая страница"));
        if (page < totalPages - 1) menu.setItem(35, createGuiItem(Material.ARROW, "§aСледующая страница"));
        menu.setItem(31, createGuiItem(Material.BARRIER, "§cНазад"));
        player.openInventory(menu);
    }

    public void openPlayerActionsMenu(Player leader, OfflinePlayer target) {
        Inventory menu = Bukkit.createInventory(null, 9, "§8Действия: " + target.getName());
        menu.setItem(0, createGuiItem(Material.EMERALD, "§aПовысить"));
        menu.setItem(1, createGuiItem(Material.REDSTONE, "§cПонизить"));
        menu.setItem(4, createGuiItem(Material.PAPER, "§eВыдать выговор"));
        menu.setItem(8, createGuiItem(Material.LAVA_BUCKET, "§4Уволить из фракции"));
        leader.openInventory(menu);
    }

    public void openRankManagementMenu(Player player, Faction faction) {
        Inventory menu = Bukkit.createInventory(null, 54, "§5Управление рангами");
        menu.setItem(0, createGuiItem(Material.DIAMOND, "§bРанг Лидера: §f" + faction.getLeaderRankName(), "§aЛКМ - Переименовать"));
        for (int i = 0; i < faction.getRanks().size(); i++) {
            menu.setItem(i + 1, createGuiItem(Material.NAME_TAG, "§eРанг " + (i + 1) + ": §f" + faction.getRankName(i), "§aЛКМ - Переименовать"));
        }
        menu.setItem(45, createGuiItem(Material.ANVIL, "§aДобавить новый ранг"));
        menu.setItem(46, createGuiItem(Material.BARRIER, "§cУдалить последний ранг"));
        menu.setItem(49, createGuiItem(Material.ARROW, "§cНазад"));
        player.openInventory(menu);
    }

    public void openOnlineListMenu(Player player, Faction faction) {
        List<Player> onlineMembers = faction.getMembers().keySet().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
        Player leader = (faction.getLeader() != null) ? Bukkit.getPlayer(faction.getLeader()) : null;
        if (leader != null && !onlineMembers.contains(leader)) onlineMembers.add(leader);

        Inventory menu = Bukkit.createInventory(null, 54, "§3Онлайн: " + faction.getName());
        for (Player member : onlineMembers) {
            menu.addItem(createPlayerHead(member, "§a" + member.getName(), "§7Ранг: §f" + faction.getRankName(faction.getRank(member.getUniqueId()))));
        }
        menu.setItem(49, createGuiItem(Material.ARROW, "§cНазад"));
        player.openInventory(menu);
    }

    public void openPermissionsMenu(Player player, Faction faction) {
        Inventory menu = Bukkit.createInventory(null, 27, "§6Права (ФБР)");
        menu.setItem(11, createGuiItem(Material.IRON_BARS, "§eКоманда /cuff", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("cuff", 99)), "§aНажмите для изменения"));
        menu.setItem(13, createGuiItem(Material.CHEST, "§bКоманда /search", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("search", 99)), "§aНажмите для изменения"));
        menu.setItem(15, createGuiItem(Material.LEAD, "§aКоманда /follow", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("follow", 99)), "§aНажмите для изменения"));
        menu.setItem(22, createGuiItem(Material.ARROW, "§cНазад"));
        player.openInventory(menu);
    }

    private ItemStack createPlayerHead(OfflinePlayer player, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}