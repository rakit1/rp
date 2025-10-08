package s1nekz.rp.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.PlayerStateManager;

public class PlayerInteractionListener implements Listener {

    private final FactionManager factionManager;
    private final PlayerStateManager stateManager;
    private final GuiManager guiManager;

    public PlayerInteractionListener(FactionManager fm, PlayerStateManager sm, GuiManager gm) {
        this.factionManager = fm;
        this.stateManager = sm;
        this.guiManager = gm;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player interactor = event.getPlayer();
        if (interactor.isSneaking()) {
            event.setCancelled(true);
            Player target = (Player) event.getRightClicked();
            // Вся логика открытия меню теперь в GuiManager
            guiManager.openInteractionMenu(interactor, target);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Если игрок в наручниках, отменяем его движение
        if (stateManager.isCuffed(event.getPlayer())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }
}