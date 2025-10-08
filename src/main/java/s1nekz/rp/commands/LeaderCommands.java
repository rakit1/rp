package s1nekz.rp.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.FactionManager.Faction;

public class LeaderCommands implements CommandExecutor {

    private final FactionManager factionManager;
    private final GuiManager guiManager;

    public LeaderCommands(FactionManager factionManager, GuiManager guiManager) {
        this.factionManager = factionManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("makeleader")) return handleMakeLeader(sender, args);
        if (command.getName().equalsIgnoreCase("lmenu")) return handleLeaderMenu(sender);
        return false;
    }

    private boolean handleMakeLeader(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§eИспользование: /makeleader <игрок> <фракция> [remove]"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Faction faction = factionManager.getFaction(args[1]);
        if (faction == null) { sender.sendMessage("§cФракция '" + args[1] + "' не найдена."); return true; }

        boolean remove = args.length > 2 && args[2].equalsIgnoreCase("remove");
        if (remove) {
            faction.setLeader(null);
            sender.sendMessage("§aВы сняли " + target.getName() + " с поста лидера " + faction.getName() + ".");
        } else {
            faction.setLeader(target.getUniqueId());
            sender.sendMessage("§aВы назначили " + target.getName() + " лидером фракции " + faction.getName() + ".");
        }
        factionManager.saveFactions();
        return true;
    }

    private boolean handleLeaderMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cТолько для игроков."); return true; }
        Faction faction = factionManager.getLeaderFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage("§cВы не являетесь лидером фракции."); return true; }
        guiManager.openLeaderMenu(player, faction);
        return true;
    }
}