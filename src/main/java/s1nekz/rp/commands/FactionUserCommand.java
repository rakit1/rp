package s1nekz.rp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.FactionManager.Faction;

public class FactionUserCommand implements CommandExecutor {

    private final FactionManager factionManager;

    public FactionUserCommand(FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("accept")) {
            String factionId = FactionManager.pendingInvites.get(player.getUniqueId());
            if (factionId == null) {
                player.sendMessage("§cУ вас нет активных приглашений.");
                return true;
            }

            Faction factionToJoin = factionManager.getFaction(factionId);
            if (factionToJoin == null) {
                player.sendMessage("§cФракция, в которую вас пригласили, больше не существует.");
                return true;
            }

            factionToJoin.addMember(player.getUniqueId());
            FactionManager.pendingInvites.remove(player.getUniqueId());
            player.sendMessage("§aВы приняли приглашение и вступили во фракцию " + factionToJoin.getName() + "!");
            factionManager.saveFactions();
            return true;
        }

        player.sendMessage("§eИспользование: /faction accept");
        return true;
    }
}