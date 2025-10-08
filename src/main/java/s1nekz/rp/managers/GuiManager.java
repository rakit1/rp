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
import java.util.stream.Collectors; // <-- ИСПРАВЛЕНО: Добавлен этот импорт

public class GuiManager {
    public static final Map<UUID, UUID> interactionTargets = new HashMap<>();
    private final FactionManager factionManager;

    public GuiManager(FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    public void openInteractionMenu(Player interactor, Player target) {
        Faction interactorFaction = factionManager.getPlayerFaction(interactor.getUniqueId());

        boolean isLeader = interactorFaction != null && interactorFaction.getLeader() != null && interactorFaction.getLeader().equals(interactor.getUniqueId());
        boolean canCuff = factionManager.canPerformAction(interactor.getUniqueId(), "cuff");
        boolean canSearch = factionManager.canPerformAction(interactor.getUniqueId(), "search");
        boolean canFollow = factionManager.canPerformAction(interactor.getUniqueId(), "follow");
        boolean canInvite = factionManager.canPerformAction(interactor.getUniqueId(), "invite");
        boolean canFireGov = factionManager.canPerformAction(interactor.getUniqueId(), "fire_gov");
        boolean canWarnGov = factionManager.canPerformAction(interactor.getUniqueId(), "warn_gov");

        Faction targetFaction = factionManager.getPlayerFaction(target.getUniqueId());
        boolean isTargetGov = factionManager.isGovernmentFaction(targetFaction);
        boolean canManageOwn = isLeader && targetFaction != null && targetFaction.getId().equals(interactorFaction.getId());

        if (!canCuff && !canSearch && !canFollow && !canInvite && !canFireGov && !canWarnGov && !canManageOwn) {
            interactor.sendMessage(ChatColor.RED + "У вас нет доступных взаимодействий с этим игроком.");
            return;
        }

        interactionTargets.put(interactor.getUniqueId(), target.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 18, "§8Действия с " + target.getName());

        if (canCuff) menu.setItem(0, createGuiItem(Material.IRON_BARS, "§eНадеть/снять наручники"));
        if (canSearch) menu.setItem(1, createGuiItem(Material.CHEST, "§bОбыскать игрока"));
        if (canFollow) menu.setItem(2, createGuiItem(Material.LEAD, "§aВести/отпустить за собой"));

        if (canInvite && targetFaction == null) {
            menu.setItem(16, createGuiItem(Material.EMERALD, "§aПригласить в " + interactorFaction.getName()));
        }

        if (canManageOwn) {
            menu.setItem(17, createGuiItem(Material.NETHER_STAR, "§6Управление сотрудником"));
        }

        if (isTargetGov && (targetFaction.getLeader() == null || !targetFaction.getLeader().equals(target.getUniqueId()))) {
            if (canWarnGov) menu.setItem(7, createGuiItem(Material.PAPER, "§6Выдать выговор (Гос.)"));
            if (canFireGov) menu.setItem(8, createGuiItem(Material.BARRIER, "§4Уволить (Гос.)"));
        }

        interactor.openInventory(menu);
    }

    public void openPermissionsMenu(Player player, Faction faction) {
        Inventory menu = Bukkit.createInventory(null, 36, "§6Права (ФБР)");

        menu.setItem(10, createGuiItem(Material.EMERALD, "§aПриглашение в фракцию", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("invite", 999))));
        menu.setItem(11, createGuiItem(Material.PAPER, "§6Выговоры своим", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("warn", 999))));
        menu.setItem(13, createGuiItem(Material.IRON_BARS, "§eНаручники", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("cuff", 999))));
        menu.setItem(14, createGuiItem(Material.CHEST, "§bОбыск", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("search", 999))));
        menu.setItem(15, createGuiItem(Material.LEAD, "§aСледование", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("follow", 999))));
        menu.setItem(16, createGuiItem(Material.BOOK, "§6Выговоры (Гос.)", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("warn_gov", 999))));
        menu.setItem(22, createGuiItem(Material.BARRIER, "§4Увольнение (Гос.)", "§7Треб. ранг: §f" + faction.getRankName(faction.getPermissions().getOrDefault("fire_gov", 999))));

        menu.setItem(31, createGuiItem(Material.ARROW, "§cНазад"));
        player.openInventory(menu);
    }

    public void openLeaderMenu(Player player, Faction faction) {
        Inventory lmenu = Bukkit.createInventory(null, 27, "§1Меню лидера: " + faction.getName());
        lmenu.setItem(10, createGuiItem(Material.NAME_TAG, "§eУправление рангами"));
        lmenu.setItem(12, createGuiItem(Material.PLAYER_HEAD, "§aУправление составом"));
        lmenu.setItem(14, createGuiItem(Material.SPYGLASS, "§bОнлайн фракции"));
        lmenu.setItem(16, createGuiItem(Material.WRITABLE_BOOK, "§6Настройка прав (ФБР)"));
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
        Inventory menu = Bukkit.createInventory(null, 27, "§8Действия: " + target.getName());
        menu.setItem(10, createGuiItem(Material.EMERALD, "§aПовысить"));
        menu.setItem(11, createGuiItem(Material.REDSTONE, "§cПонизить"));
        menu.setItem(13, createGuiItem(Material.PAPER, "§eВыдать выговор"));
        menu.setItem(14, createGuiItem(Material.BUCKET, "§bСнять выговор"));
        menu.setItem(16, createGuiItem(Material.LAVA_BUCKET, "§4Уволить из фракции"));
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

    public void openInviteConfirmMenu(Player target, Faction faction) {
        Inventory menu = Bukkit.createInventory(null, 27, "§0Приглашение во фракцию");
        ItemStack info = createGuiItem(Material.PAPER, "§b" + faction.getName(), "§7Вас приглашают вступить", "§7в эту организацию.");
        ItemStack accept = createGuiItem(Material.GREEN_WOOL, "§aПринять");
        ItemStack decline = createGuiItem(Material.RED_WOOL, "§cОтклонить");
        menu.setItem(13, info);
        menu.setItem(11, accept);
        menu.setItem(15, decline);
        target.openInventory(menu);
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