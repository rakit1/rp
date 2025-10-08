package s1nekz.rp;

import org.bukkit.plugin.java.JavaPlugin;
import s1nekz.rp.commands.LeaderCommands;
import s1nekz.rp.listeners.GuiListener;
import s1nekz.rp.listeners.PlayerInteractionListener;
import s1nekz.rp.managers.FactionManager;
import s1nekz.rp.managers.GuiManager;
import s1nekz.rp.managers.PlayerStateManager;

public final class Rp extends JavaPlugin {

    private static Rp instance;
    private FactionManager factionManager;
    private PlayerStateManager stateManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        this.factionManager = new FactionManager(this);
        this.stateManager = new PlayerStateManager();
        this.guiManager = new GuiManager(factionManager);

        // Регистрация команд
        LeaderCommands leaderCommands = new LeaderCommands(factionManager, guiManager);
        getCommand("makeleader").setExecutor(leaderCommands);
        getCommand("lmenu").setExecutor(leaderCommands);

        // Регистрация слушателей событий
        GuiListener guiListener = new GuiListener(factionManager, stateManager, guiManager);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new PlayerInteractionListener(factionManager, stateManager, guiManager), this);

        getLogger().info("RP Plugin by s1nekz has been enabled!");
    }

    @Override
    public void onDisable() {
        if (factionManager != null) factionManager.saveFactions();
        getLogger().info("RP Plugin by s1nekz has been disabled!");
    }

    public static Rp getInstance() { return instance; }
    public FactionManager getFactionManager() { return factionManager; }
    public PlayerStateManager getStateManager() { return stateManager; }
    public GuiManager getGuiManager() { return guiManager; }
}