package s1nekz.rp.managers;

// --- УБЕДИТЕСЬ, ЧТО ВСЕ ЭТИ ИМПОРТЫ ПРИСУТСТВУЮТ В ВАШЕМ ФАЙЛЕ ---
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType; // <-- ВОЗМОЖНО, ЭТОЙ СТРОКИ НЕ ХВАТАЕТ
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import s1nekz.rp.Rp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
// --- КОНЕЦ СПИСКА ИМПОРТОВ ---

/**
 * Управляет временными состояниями игроков, такими как наручники или следование.
 */
public class PlayerStateManager {

    // Хранит: UUID скованного игрока <- UUID того, кто его сковал
    private final Map<UUID, UUID> cuffedPlayers = new HashMap<>();

    // Хранит: UUID следующего игрока <- задача (таск), которая управляет его движением
    private final Map<UUID, BukkitTask> followingPlayers = new HashMap<>();

    /**
     * Проверяет, надеты ли на игрока наручники.
     * @param player Игрок для проверки.
     * @return true, если на игроке надеты наручники.
     */
    public boolean isCuffed(Player player) {
        return cuffedPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Надевает или снимает наручники с целевого игрока.
     * @param target Целевой игрок.
     * @param cuffer Игрок, который выполняет действие.
     */
    public void toggleCuff(Player target, Player cuffer) {
        if (isCuffed(target)) {
            cuffedPlayers.remove(target.getUniqueId());
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            cuffer.sendMessage(ChatColor.GREEN + "Вы сняли наручники с игрока " + target.getName() + ".");
            target.sendMessage(ChatColor.YELLOW + "С вас сняли наручники.");
        } else {
            cuffedPlayers.put(target.getUniqueId(), cuffer.getUniqueId());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, -1, 2, false, false));
            cuffer.sendMessage(ChatColor.GREEN + "Вы надели наручники на игрока " + target.getName() + ".");
            target.sendMessage(ChatColor.RED + "На вас надели наручники.");
        }
    }

    /**
     * Проверяет, следует ли игрок за кем-либо.
     * @param player Игрок для проверки.
     * @return true, если игрок следует за кем-то.
     */
    public boolean isFollowing(Player player) {
        return followingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Заставляет целевого игрока следовать за лидером или отменяет следование.
     * @param target Целевой игрок, который будет следовать.
     * @param leader Игрок, за которым будут следовать.
     */
    public void toggleFollow(Player target, Player leader) {
        if (isFollowing(target)) {
            followingPlayers.get(target.getUniqueId()).cancel();
            followingPlayers.remove(target.getUniqueId());
            leader.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " больше не следует за вами.");
            target.sendMessage(ChatColor.YELLOW + "Вы больше не следуете за игроком " + leader.getName() + ".");
        } else {
            BukkitTask followTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!target.isOnline() || !leader.isOnline()) {
                        this.cancel();
                        followingPlayers.remove(target.getUniqueId());
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