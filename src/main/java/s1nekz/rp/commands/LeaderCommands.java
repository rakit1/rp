package s1nekz.rp.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.GuiManager;

public class LeaderCommands implements CommandExecutor {

    private final FactionManager factionManager;
    private final GuiManager guiManager;

    public LeaderCommands(FactionManager factionManager, GuiManager guiManager) {
        this.factionManager = factionManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("makeleader")) {
            if (!player.hasPermission("rp.admin.makeleader")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав.");
                return true;
            }
            if (args.length < 1) {
                player.sendMessage("§eИспользование: /makeleader <игрок>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Игрок '" + args[0] + "' никогда не играл на сервере.");
                return true;
            }
            guiManager.openMakeLeaderMenu(player, target);
            return true;
        }

        if (command.getName().equalsIgnoreCase("lmenu")) {
            FactionManager.Faction faction = factionManager.getLeaderFaction(player.getUniqueId());
            if (faction == null) {
                player.sendMessage("§cВы не являетесь лидером фракции.");
                return true;
            }
            guiManager.openLeaderMenu(player, faction);
            return true;
        }
        return false;
    }
}