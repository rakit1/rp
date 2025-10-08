package s1nekz.rp.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import s1nekz.rp.Rp;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.FactionManager.Faction;
import s1nekz.rp.managers.GuiManager;
import s1nekz.rp.managers.PlayerStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private final Map<UUID, Integer> renamingRank = new HashMap<>();
    private final Map<UUID, String> settingPermission = new HashMap<>();
    private final Map<UUID, UUID> playerActionTarget = new HashMap<>();

    private final FactionManager fManager;
    private final PlayerStateManager sManager;
    private final GuiManager gManager;

    public GuiListener(FactionManager fm, PlayerStateManager sm, GuiManager gm) {
        this.fManager = fm;
        this.sManager = sm;
        this.gManager = gm;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        Faction f = fManager.getLeaderFaction(uuid);
        if (f == null) return;

        if (renamingRank.containsKey(uuid)) {
            event.setCancelled(true);
            int rankIndex = renamingRank.remove(uuid);
            if (event.getMessage().equalsIgnoreCase("отмена")) {
                p.sendMessage("§cПереименование отменено.");
                return;
            }
            if (rankIndex == -1) {
                f.setLeaderRankName(event.getMessage());
            } else {
                f.renameRank(rankIndex, event.getMessage());
            }
            fManager.saveFactions();
            p.sendMessage("§aРанг переименован.");
            Bukkit.getScheduler().runTask(Rp.getInstance(), () -> gManager.openRankManagementMenu(p, f));
        }

        if (settingPermission.containsKey(uuid)) {
            event.setCancelled(true);
            String cmd = settingPermission.remove(uuid);
            if(event.getMessage().equalsIgnoreCase("отмена")) { p.sendMessage("§cНастройка прав отменена."); return; }
            try {
                int rankIndex = Integer.parseInt(event.getMessage()) - 1;
                if (rankIndex < 0 || rankIndex >= f.getRanks().size()) throw new NumberFormatException();
                f.setPermission(cmd, rankIndex);
                fManager.saveFactions();
                p.sendMessage("§aПрава для /" + cmd + " установлены на ранг '" + f.getRankName(rankIndex) + "'.");
                Bukkit.getScheduler().runTask(Rp.getInstance(), () -> gManager.openPermissionsMenu(p, f));
            } catch (NumberFormatException e) {
                p.sendMessage("§cНеверный номер ранга. Введите число от 1 до " + f.getRanks().size());
                settingPermission.put(uuid, cmd);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p) || event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        String item = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        if (!title.matches("^(Меню лидера:|Состав:|Действия:|Управление рангами|Онлайн:|Права \\(ФБР\\)|Действия с |Инвентарь: ).*")) return;
        event.setCancelled(true);

        Faction f = fManager.getLeaderFaction(p.getUniqueId());

        if (title.startsWith("Меню лидера:")) {
            if (f == null) return;
            if (item.equals("Управление рангами")) gManager.openRankManagementMenu(p, f);
            else if (item.equals("Управление составом")) gManager.openMemberListMenu(p, f, 0);
            else if (item.equals("Онлайн фракции")) gManager.openOnlineListMenu(p, f);
            else if (item.equals("Настройка прав (ФБР)")) {
                if (f.getId().equalsIgnoreCase("fbi")) gManager.openPermissionsMenu(p, f);
                else p.sendMessage("§cЭтот раздел доступен только для лидера ФБР.");
            }
        } else if (title.startsWith("Состав:")) {
            if (f == null) return;
            int page = Integer.parseInt(title.substring(title.indexOf('(') + 1, title.indexOf('/'))) - 1;
            if (item.equals("Следующая страница")) gManager.openMemberListMenu(p, f, page + 1);
            else if (item.equals("Предыдущая страница")) gManager.openMemberListMenu(p, f, page - 1);
            else if (item.equals("Назад")) gManager.openLeaderMenu(p, f);
            else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(item);
                playerActionTarget.put(p.getUniqueId(), target.getUniqueId());
                gManager.openPlayerActionsMenu(p, target);
            }
        } else if (title.startsWith("Действия: ")) {
            if (f == null) return;
            OfflinePlayer target = Bukkit.getOfflinePlayer(title.substring(10));
            if (target.getUniqueId().equals(f.getLeader())) { p.sendMessage("§cВы не можете управлять собой."); return; }

            if (item.equals("Повысить")) { f.promote(target.getUniqueId()); p.sendMessage("§aВы повысили " + target.getName()); }
            else if (item.equals("Понизить")) { f.demote(target.getUniqueId()); p.sendMessage("§aВы понизили " + target.getName()); }
            else if (item.equals("Выдать выговор")) { f.addWarning(target.getUniqueId()); p.sendMessage("§aВы выдали выговор " + target.getName()); }
            else if (item.equals("Уволить из фракции")) { f.removeMember(target.getUniqueId()); p.sendMessage("§aВы уволили " + target.getName()); }

            fManager.saveFactions();
            p.closeInventory();
        } else if (title.startsWith("Управление рангами")) {
            if (f == null) return;
            if (item.equals("Назад")) gManager.openLeaderMenu(p, f);
            else if (item.equals("Добавить новый ранг")) { f.addRank("Новый ранг"); gManager.openRankManagementMenu(p, f); }
            else if (item.equals("Удалить последний ранг")) { f.removeLastRank(); gManager.openRankManagementMenu(p, f); }
            else if (item.startsWith("Ранг Лидера:")) {
                renamingRank.put(p.getUniqueId(), -1);
                p.closeInventory();
                p.sendMessage("§eВведите новое название для ранга Лидера. Напишите 'отмена', чтобы отменить.");
            } else if (item.startsWith("Ранг ")) {
                int index = Integer.parseInt(item.split(" ")[1].replace(":", "")) - 1;
                renamingRank.put(p.getUniqueId(), index);
                p.closeInventory();
                p.sendMessage("§eВведите новое название для ранга. Напишите 'отмена', чтобы отменить.");
            }
        } else if (title.startsWith("Онлайн:") || title.startsWith("Права (ФБР)")) {
            if (f == null) return;
            if (item.equals("Назад")) gManager.openLeaderMenu(p, f);
            if (title.startsWith("Права (ФБР)")) {
                String cmd = null;
                if (item.contains("/cuff")) cmd = "cuff"; else if (item.contains("/search")) cmd = "search"; else if (item.contains("/follow")) cmd = "follow";
                if (cmd != null) {
                    settingPermission.put(p.getUniqueId(), cmd);
                    p.closeInventory();
                    p.sendMessage("§eВведите в чат номер ранга (например, 3) для команды /" + cmd + ". Напишите 'отмена', чтобы отменить.");
                }
            }
        } else if (title.startsWith("Действия с ")) {
            Player target = Bukkit.getPlayer(GuiManager.interactionTargets.get(p.getUniqueId()));
            if (target == null) { p.sendMessage("§cИгрок вышел из сети."); return; }

            if (item.equals("Надеть/снять наручники")) {
                sManager.toggleCuff(target, p);
                p.closeInventory();
            } else if (item.equals("Обыскать игрока")) {
                Inventory targetInv = Bukkit.createInventory(null, 45, "§8Инвентарь: " + target.getName());
                targetInv.setContents(target.getInventory().getContents());
                p.openInventory(targetInv);
            }
            else if (item.equals("Вести/отпустить за собой")) {
                sManager.toggleFollow(target, p);
                p.closeInventory();
            } else if (item.equals("Пригласить в организацию")) {
                Faction leaderFaction = fManager.getLeaderFaction(p.getUniqueId());
                if (leaderFaction != null) {
                    FactionManager.pendingInvites.put(target.getUniqueId(), leaderFaction.getId());
                    p.sendMessage("§aВы пригласили игрока " + target.getName() + " во фракцию.");
                    target.sendMessage("§eЛидер фракции " + leaderFaction.getName() + " пригласил вас вступить. Напишите §a/faction accept§e, чтобы принять.");
                }
                p.closeInventory();
            } else if (item.equals("Управление сотрудником")) {
                playerActionTarget.put(p.getUniqueId(), target.getUniqueId());
                gManager.openPlayerActionsMenu(p, target);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.startsWith("Действия с ")) GuiManager.interactionTargets.remove(uuid);
        else if (title.startsWith("Действия: ")) playerActionTarget.remove(uuid);
    }
}