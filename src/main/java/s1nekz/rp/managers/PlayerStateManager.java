package s1nekz.rp.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import s1nekz.rp.Rp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateManager {

    private final Map<UUID, UUID> cuffedPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> followingPlayers = new HashMap<>();

    public boolean isCuffed(Player player) {
        return cuffedPlayers.containsKey(player.getUniqueId());
    }

    public void toggleCuff(Player target, Player cuffer) {
        if (isCuffed(target)) {
            UUID leaderUUID = cuffedPlayers.get(target.getUniqueId());
            cuffedPlayers.remove(target.getUniqueId());
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            cuffer.sendMessage(ChatColor.GREEN + "Вы сняли наручники с игрока " + target.getName() + ".");
            target.sendMessage(ChatColor.YELLOW + "С вас сняли наручники.");

            if (isFollowing(target)) {
                Player leader = (leaderUUID != null) ? Bukkit.getPlayer(leaderUUID) : cuffer;
                if (leader != null) {
                    toggleFollow(target, leader);
                }
            }
        } else {
            cuffedPlayers.put(target.getUniqueId(), cuffer.getUniqueId());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, -1, 2, false, false));
            cuffer.sendMessage(ChatColor.GREEN + "Вы надели наручники на игрока " + target.getName() + ".");
            target.sendMessage(ChatColor.RED + "На вас надели наручники.");
        }
    }

    public boolean isFollowing(Player player) {
        return followingPlayers.containsKey(player.getUniqueId());
    }

    public void toggleFollow(Player target, Player leader) {
        if (!isFollowing(target) && !isCuffed(target)) {
            leader.sendMessage(ChatColor.RED + "Вы должны сначала надеть на игрока наручники!");
            return;
        }

        if (isFollowing(target)) {
            followingPlayers.get(target.getUniqueId()).cancel();
            followingPlayers.remove(target.getUniqueId());
            leader.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " больше не следует за вами.");
            target.sendMessage(ChatColor.YELLOW + "Вы больше не следуете за игроком " + leader.getName() + ".");
        } else {
            BukkitTask followTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!target.isOnline() || !leader.isOnline() || !isCuffed(target)) {
                        this.cancel();
                        followingPlayers.remove(target.getUniqueId());
                        if (leader.isOnline()) {
                            leader.sendMessage(ChatColor.YELLOW + "Следование за " + target.getName() + " автоматически прекращено.");
                        }
                        return;
                    }
                    if (!target.getWorld().equals(leader.getWorld())) {
                        this.cancel();
                        followingPlayers.remove(target.getUniqueId());
                        if (leader.isOnline()) {
                            leader.sendMessage(ChatColor.RED + "Следование отменено: игрок в другом мире.");
                        }
                        return;
                    }
                    double distance = target.getLocation().distance(leader.getLocation());
                    if (distance > 4.0) {
                        Location behindLeader = leader.getLocation().subtract(leader.getLocation().getDirection().multiply(1.5));
                        target.teleport(behindLeader);
                    }
                }
            }.runTaskTimer(Rp.getInstance(), 0L, 20L);

            followingPlayers.put(target.getUniqueId(), followTask);
            leader.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " теперь следует за вами.");
            target.sendMessage(ChatColor.YELLOW + "Вы теперь следуете за игроком " + leader.getName() + ".");
        }
    }
}