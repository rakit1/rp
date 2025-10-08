package s1nekz.rp.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import s1nekz.rp.Rp;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionManager {

    public static final Map<UUID, String> pendingInvites = new HashMap<>();
    private static final List<String> governmentFactionIds = Arrays.asList("police", "government");

    private final Rp plugin;
    private final Map<String, Faction> factions = new HashMap<>();
    private File factionsFile;
    private FileConfiguration factionsConfig;

    public FactionManager(Rp plugin) {
        this.plugin = plugin;
        loadFactions();
    }

    private void createDefaultFactions() {
        List<String> ranks = Arrays.asList("Стажёр", "Агент", "Спецагент", "Зам. Главы", "Глава Отдела");
        int leaderRankIndex = ranks.size();

        Map<String, Integer> defaultPerms = new HashMap<>();
        defaultPerms.put("invite", leaderRankIndex);
        defaultPerms.put("warn_own", leaderRankIndex);

        Map<String, Integer> fbiPerms = new HashMap<>(defaultPerms);
        fbiPerms.put("cuff", 2);
        fbiPerms.put("search", 2);
        fbiPerms.put("follow", 3);
        fbiPerms.put("fire_gov", 4);
        fbiPerms.put("warn_gov", 3);

        createFactionSection("fbi", "ФБР", ranks, fbiPerms);
        createFactionSection("police", "Полиция", ranks, defaultPerms);
        createFactionSection("mafia", "Мафия", ranks, defaultPerms);
        createFactionSection("government", "Правительство", ranks, defaultPerms);
        saveConfig();
    }

    public boolean canPerformAction(UUID uuid, String action) {
        Faction f = getPlayerFaction(uuid);
        if (f == null) return false;

        boolean isLeader = f.getLeader() != null && f.getLeader().equals(uuid);
        if (isLeader && (action.equals("promote") || action.equals("demote") || action.equals("fire_own"))) {
            return true;
        }

        // Права, эксклюзивные для ФБР
        if (!f.getId().equalsIgnoreCase("fbi") && (action.equals("cuff") || action.equals("search") || action.equals("follow") || action.equals("warn_gov") || action.equals("fire_gov"))) {
            return false;
        }

        int requiredRank = f.getPermissions().getOrDefault(action, f.getRanks().size());
        return f.getRank(uuid) >= requiredRank;
    }

    public boolean isGovernmentFaction(Faction faction) {
        return faction != null && governmentFactionIds.contains(faction.getId().toLowerCase());
    }

    public Collection<Faction> getAllFactions() { return factions.values(); }
    public void loadFactions() { factionsFile = new File(plugin.getDataFolder(), "factions.yml"); if (!factionsFile.exists()) plugin.saveResource("factions.yml", false); factionsConfig = YamlConfiguration.loadConfiguration(factionsFile); factions.clear(); ConfigurationSection factionsSection = factionsConfig.getConfigurationSection("factions"); if (factionsSection == null) { createDefaultFactions(); factionsSection = factionsConfig.getConfigurationSection("factions"); } for (String factionId : factionsSection.getKeys(false)) { ConfigurationSection data = factionsSection.getConfigurationSection(factionId); if (data != null) { Map<UUID, Integer> members = new HashMap<>(), warnings = new HashMap<>(); Map<String, Integer> permissions = new HashMap<>(); ConfigurationSection membersSection = data.getConfigurationSection("members"); if (membersSection != null) membersSection.getValues(false).forEach((k, v) -> members.put(UUID.fromString(k), (Integer) v)); ConfigurationSection warningsSection = data.getConfigurationSection("warnings"); if (warningsSection != null) warningsSection.getValues(false).forEach((k, v) -> warnings.put(UUID.fromString(k), (Integer) v)); ConfigurationSection permissionsSection = data.getConfigurationSection("permissions"); if (permissionsSection != null) permissionsSection.getValues(false).forEach((k, v) -> permissions.put(k, (Integer) v)); factions.put(factionId.toLowerCase(), new Faction(factionId, data.getString("name"), data.getString("leader") != null ? UUID.fromString(data.getString("leader")) : null, data.getString("leaderRankName", "Лидер"), members, data.getStringList("ranks"), permissions, warnings)); } } }
    public void saveFactions() { for (Map.Entry<String, Faction> entry : factions.entrySet()) { String path = "factions." + entry.getKey() + "."; Faction f = entry.getValue(); factionsConfig.set(path + "name", f.getName()); factionsConfig.set(path + "leader", f.getLeader() != null ? f.getLeader().toString() : null); factionsConfig.set(path + "leaderRankName", f.getLeaderRankName()); factionsConfig.set(path + "ranks", f.getRanks()); Map<String, Integer> members = new HashMap<>(), warnings = new HashMap<>(); f.getMembers().forEach((k, v) -> members.put(k.toString(), v)); f.getWarningsMap().forEach((k, v) -> warnings.put(k.toString(), v)); factionsConfig.set(path + "members", members); factionsConfig.set(path + "warnings", warnings); factionsConfig.set(path + "permissions", f.getPermissions()); } saveConfig(); }
    private void createFactionSection(String id, String name, List<String> ranks, Map<String, Integer> perms) { String path = "factions." + id + "."; factionsConfig.set(path + "name", name); factionsConfig.set(path + "leader", null); factionsConfig.set(path + "leaderRankName", "Лидер"); factionsConfig.set(path + "ranks", ranks); factionsConfig.set(path + "members", new HashMap<>()); factionsConfig.set(path + "warnings", new HashMap<>()); factionsConfig.set(path + "permissions", perms); }
    private void saveConfig() { try { factionsConfig.save(factionsFile); } catch (IOException e) { e.printStackTrace(); } }
    public Faction getFaction(String id) { return factions.get(id.toLowerCase()); }
    public Faction getPlayerFaction(UUID uuid) { return factions.values().stream().filter(f -> f.isMember(uuid)).findFirst().orElse(null); }
    public Faction getLeaderFaction(UUID uuid) { return factions.values().stream().filter(f -> f.getLeader() != null && f.getLeader().equals(uuid)).findFirst().orElse(null); }

    public static class Faction {
        private final String id; private String name; private UUID leader; private String leaderRankName; private Map<UUID, Integer> members; private List<String> ranks; private Map<String, Integer> permissions; private Map<UUID, Integer> warnings;
        public Faction(String id, String name, UUID leader, String leaderRankName, Map<UUID, Integer> members, List<String> ranks, Map<String, Integer> permissions, Map<UUID, Integer> warnings) { this.id = id; this.name = name; this.leader = leader; this.leaderRankName = leaderRankName; this.members = members; this.ranks = ranks; this.permissions = permissions; this.warnings = warnings; }
        public String getId() { return id; } public String getName() { return name; } public UUID getLeader() { return leader; } public String getLeaderRankName() { return leaderRankName; } public Map<UUID, Integer> getMembers() { return members; } public List<String> getRanks() { return ranks; } public Map<String, Integer> getPermissions() { return permissions; } public Map<UUID, Integer> getWarningsMap() { return warnings; }
        public void setLeader(UUID leader) { this.leader = leader; } public void setLeaderRankName(String name) { this.leaderRankName = name; }
        public boolean isMember(UUID uuid) { return members.containsKey(uuid) || (leader != null && leader.equals(uuid)); }
        public int getRank(UUID uuid) { if (leader != null && leader.equals(uuid)) return ranks.size(); return members.getOrDefault(uuid, -1); }
        public String getRankName(int i) { if (i == ranks.size()) return leaderRankName; if (i > ranks.size()) return "§cТолько Лидер"; if (i < 0) return "§cНеизвестно"; return ranks.get(i); }
        public void addMember(UUID uuid) { members.put(uuid, 0); }
        public void promote(UUID uuid) { int currentRank = getRank(uuid); if (members.containsKey(uuid) && currentRank < ranks.size() - 1) { members.put(uuid, currentRank + 1); OfflinePlayer member = Bukkit.getOfflinePlayer(uuid); if (member.isOnline()) { ((Player) member).sendMessage(ChatColor.GREEN + "Вас повысили до ранга: " + getRankName(currentRank + 1)); } } }
        public void demote(UUID uuid) { int currentRank = getRank(uuid); if (members.containsKey(uuid) && currentRank > 0) { members.put(uuid, currentRank - 1); OfflinePlayer member = Bukkit.getOfflinePlayer(uuid); if (member.isOnline()) { ((Player) member).sendMessage(ChatColor.RED + "Вас понизили до ранга: " + getRankName(currentRank - 1)); } } }
        public void removeMember(UUID uuid) { OfflinePlayer member = Bukkit.getOfflinePlayer(uuid); if (member.isOnline()) { ((Player) member).sendMessage(ChatColor.DARK_RED + "Вы были уволены из фракции " + getName()); } members.remove(uuid); warnings.remove(uuid); }
        public int getWarnings(UUID uuid) { return warnings.getOrDefault(uuid, 0); }
        public boolean addWarning(UUID uuid) { int newWarnings = getWarnings(uuid) + 1; warnings.put(uuid, newWarnings); OfflinePlayer member = Bukkit.getOfflinePlayer(uuid); if (member.isOnline()) { ((Player) member).sendMessage(ChatColor.RED + "Вы получили выговор! Теперь у вас " + newWarnings + "/3."); } if (newWarnings >= 3) { removeMember(uuid); return true; } return false; }
        public void removeWarning(UUID uuid) { int currentWarnings = getWarnings(uuid); if (currentWarnings > 0) { warnings.put(uuid, currentWarnings - 1); OfflinePlayer member = Bukkit.getOfflinePlayer(uuid); if (member.isOnline()) { ((Player) member).sendMessage(ChatColor.GREEN + "С вас сняли один выговор! Теперь у вас " + (currentWarnings - 1) + "/3."); } } }
        public void renameRank(int i, String n) { if (i >= 0 && i < ranks.size()) ranks.set(i, n); }
        public void addRank(String n) { ranks.add(n); }
        public void removeLastRank() { if (!ranks.isEmpty()) ranks.remove(ranks.size() - 1); }
        public void setPermission(String c, int r) { permissions.put(c.toLowerCase(), r); }
    }
}