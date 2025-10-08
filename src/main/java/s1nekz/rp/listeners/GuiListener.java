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
            if (event.getMessage().equalsIgnoreCase("отмена")) { p.sendMessage("§cПереименование отменено."); return; }
            if (rankIndex == -1) { f.setLeaderRankName(event.getMessage()); }
            else { f.renameRank(rankIndex, event.getMessage()); }
            fManager.saveFactions();
            p.sendMessage("§aРанг переименован.");
            Bukkit.getScheduler().runTask(Rp.getInstance(), () -> gManager.openRankManagementMenu(p, f));
        }

        if (settingPermission.containsKey(uuid)) {
            event.setCancelled(true);
            String cmd = settingPermission.remove(uuid);
            if(event.getMessage().equalsIgnoreCase("отмена")) { p.sendMessage("§cНастройка прав отменена."); return; }

            try {
                int rankNumber = Integer.parseInt(event.getMessage());
                int leaderRankNumber = f.getRanks().size() + 1;

                if (rankNumber < 1 || rankNumber > leaderRankNumber) {
                    p.sendMessage("§cНеверный номер. Введите число от 1 до " + f.getRanks().size() + " (или " + leaderRankNumber + " для Лидера).");
                    settingPermission.put(uuid, cmd);
                    return;
                }

                int requiredRankIndex = (rankNumber == leaderRankNumber) ? f.getRanks().size() : rankNumber - 1;

                f.setPermission(cmd, requiredRankIndex);
                fManager.saveFactions();
                p.sendMessage("§aПрава для действия установлены на ранг '" + f.getRankName(requiredRankIndex) + "'.");
                Bukkit.getScheduler().runTask(Rp.getInstance(), () -> gManager.openPermissionsMenu(p, f));
            } catch (NumberFormatException e) {
                p.sendMessage("§cЭто не число. Пожалуйста, введите номер ранга.");
                settingPermission.put(uuid, cmd);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p) || event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        String item = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        String validTitlesRegex = "^(Меню лидера:|Состав:|Действия:|Управление рангами|Онлайн:|Права \\(ФБР\\)|Действия с |Инвентарь: |Приглашение во фракцию).*";
        if (!title.matches(validTitlesRegex)) return;
        event.setCancelled(true);

        Faction f = fManager.getLeaderFaction(p.getUniqueId());

        if (title.startsWith("Действия с ")) {
            Player target = Bukkit.getPlayer(GuiManager.interactionTargets.get(p.getUniqueId()));
            if (target == null) { p.sendMessage("§cИгрок вышел из сети."); return; }

            if (item.equals("Надеть/снять наручники")) sManager.toggleCuff(target, p);
            else if (item.equals("Обыскать игрока")) { /* ... */ }
            else if (item.equals("Вести/отпустить за собой")) sManager.toggleFollow(target, p);
            else if (item.startsWith("Пригласить в")) {
                Faction myFaction = fManager.getPlayerFaction(p.getUniqueId());
                if (myFaction != null) {
                    FactionManager.pendingInvites.put(target.getUniqueId(), myFaction.getId());
                    p.sendMessage("§aВы пригласили игрока " + target.getName() + " во фракцию.");
                    gManager.openInviteConfirmMenu(target, myFaction);
                }
            } else if (item.equals("Управление сотрудником")) {
                playerActionTarget.put(p.getUniqueId(), target.getUniqueId());
                gManager.openPlayerActionsMenu(p, target);
                return;
            } else if (item.startsWith("Выдать выговор (Гос.)")) {
                Faction targetFaction = fManager.getPlayerFaction(target.getUniqueId());
                if (targetFaction != null) {
                    boolean wasFired = targetFaction.addWarning(target.getUniqueId());
                    p.sendMessage("§aВы выдали выговор сотруднику " + target.getName() + " из фракции " + targetFaction.getName() + ".");
                    if (wasFired) p.sendMessage(ChatColor.RED + "Сотрудник " + target.getName() + " был автоматически уволен за 3/3 выговора!");
                    fManager.saveFactions();
                }
            } else if (item.startsWith("Уволить (Гос.)")) {
                Faction targetFaction = fManager.getPlayerFaction(target.getUniqueId());
                if (targetFaction != null) {
                    targetFaction.removeMember(target.getUniqueId());
                    p.sendMessage("§aВы уволили " + target.getName() + " из фракции " + targetFaction.getName() + " по полномочиям ФБР.");
                    fManager.saveFactions();
                }
            }
            p.closeInventory();
        } else if (title.startsWith("Права (ФБР)")) {
            if (f == null) return;
            if (item.equals("Назад")) gManager.openLeaderMenu(p, f);
            else {
                String cmd = null;
                if (item.contains("Приглашение")) cmd = "invite";
                else if (item.contains("Выговоры своим")) cmd = "warn";
                else if (item.contains("Наручники")) cmd = "cuff";
                else if (item.contains("Обыск")) cmd = "search";
                else if (item.contains("Следование")) cmd = "follow";
                else if (item.contains("Выговоры (Гос.)")) cmd = "warn_gov";
                else if (item.contains("Увольнение (Гос.)")) cmd = "fire_gov";

                if (cmd != null) {
                    settingPermission.put(p.getUniqueId(), cmd);
                    p.closeInventory();
                    int leaderRankNum = f.getRanks().size() + 1;
                    p.sendMessage("§eВведите номер ранга (1-" + f.getRanks().size() + ", или " + leaderRankNum + " для Лидера). 'отмена' - отменить.");
                }
            }
        }
        else if (title.startsWith("Действия: ")) {
            if (f == null) return;
            OfflinePlayer target = Bukkit.getOfflinePlayer(title.substring(10));
            if (target.getUniqueId().equals(f.getLeader())) { p.sendMessage("§cВы не можете управлять собой."); return; }

            if (item.equals("Выдать выговор")) {
                // ПРОВЕРЯЕМ ПРАВА НА ВЫДАЧУ ВЫГОВОРА СВОИМ
                if (!fManager.canPerformAction(p.getUniqueId(), "warn")) {
                    p.sendMessage(ChatColor.RED + "У вас недостаточно прав, чтобы выдавать выговоры своим сотрудникам.");
                    p.closeInventory();
                    return;
                }
                boolean wasFired = f.addWarning(target.getUniqueId());
                p.sendMessage("§aВы выдали выговор " + target.getName());
                if (wasFired) { p.sendMessage(ChatColor.RED + "Игрок " + target.getName() + " был автоматически уволен за 3/3 выговора!"); }
            }
            else if (item.equals("Повысить")) f.promote(target.getUniqueId());
            else if (item.equals("Понизить")) f.demote(target.getUniqueId());
            else if (item.equals("Снять выговор")) f.removeWarning(target.getUniqueId());
            else if (item.equals("Уволить из фракции")) f.removeMember(target.getUniqueId());

            fManager.saveFactions();
            p.closeInventory();
        }
        // (Остальные блоки без изменений)
        else if (title.startsWith("Меню лидера:")) { if (f == null) return; if (item.equals("Управление рангами")) gManager.openRankManagementMenu(p, f); else if (item.equals("Управление составом")) gManager.openMemberListMenu(p, f, 0); else if (item.equals("Онлайн фракции")) gManager.openOnlineListMenu(p, f); else if (item.equals("Настройка прав (ФБР)")) { if (f.getId().equalsIgnoreCase("fbi")) gManager.openPermissionsMenu(p, f); else p.sendMessage("§cЭтот раздел доступен только для лидера ФБР."); } }
        else if (title.startsWith("Состав:")) { if (f == null) return; int page = Integer.parseInt(title.substring(title.indexOf('(') + 1, title.indexOf('/'))) - 1; if (item.equals("Следующая страница")) gManager.openMemberListMenu(p, f, page + 1); else if (item.equals("Предыдущая страница")) gManager.openMemberListMenu(p, f, page - 1); else if (item.equals("Назад")) gManager.openLeaderMenu(p, f); else { OfflinePlayer target = Bukkit.getOfflinePlayer(item); playerActionTarget.put(p.getUniqueId(), target.getUniqueId()); gManager.openPlayerActionsMenu(p, target); } }
        else if (title.startsWith("Управление рангами")) { if (f == null) return; if (item.equals("Назад")) gManager.openLeaderMenu(p, f); else if (item.equals("Добавить новый ранг")) { f.addRank("Новый ранг"); gManager.openRankManagementMenu(p, f); } else if (item.equals("Удалить последний ранг")) { f.removeLastRank(); gManager.openRankManagementMenu(p, f); } else if (item.startsWith("Ранг Лидера:")) { renamingRank.put(p.getUniqueId(), -1); p.closeInventory(); p.sendMessage("§eВведите новое название для ранга Лидера. Напишите 'отмена', чтобы отменить."); } else if (item.startsWith("Ранг ")) { int index = Integer.parseInt(item.split(" ")[1].replace(":", "")) - 1; renamingRank.put(p.getUniqueId(), index); p.closeInventory(); p.sendMessage("§eВведите новое название для ранга. Напишите 'отмена', чтобы отменить."); } }
        else if (title.startsWith("Приглашение во фракцию")) { String factionId = FactionManager.pendingInvites.get(p.getUniqueId()); if (factionId == null) { p.sendMessage("§cЭто приглашение больше недействительно."); p.closeInventory(); return; } Faction factionToJoin = fManager.getFaction(factionId); if (factionToJoin == null) { p.sendMessage("§cЭта фракция больше не существует."); p.closeInventory(); return; } if (item.equals("Принять")) { factionToJoin.addMember(p.getUniqueId()); p.sendMessage("§aВы вступили во фракцию " + factionToJoin.getName() + "!"); fManager.saveFactions(); } else if (item.equals("Отклонить")) { p.sendMessage("§eВы отклонили приглашение."); } FactionManager.pendingInvites.remove(p.getUniqueId()); p.closeInventory(); }
    }

    @EventHandler public void onInventoryClose(InventoryCloseEvent event) { UUID uuid = event.getPlayer().getUniqueId(); String title = ChatColor.stripColor(event.getView().getTitle()); if (title.startsWith("Действия с ")) GuiManager.interactionTargets.remove(uuid); else if (title.startsWith("Действия: ")) playerActionTarget.remove(uuid); else if (title.startsWith("Приглашение во фракцию")) { FactionManager.pendingInvites.remove(uuid); } }
}