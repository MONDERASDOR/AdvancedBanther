package com.sunpowder.banther;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AdvancedBanther extends JavaPlugin implements Listener {
    
    private Set<String> authorizedUsers;
    private NamespacedKey doomAxeKey;
    
    @Override
    public void onEnable() {
        doomAxeKey = new NamespacedKey(this, "doom_axe_identifier");
        authorizedUsers = new HashSet<>(getConfig().getStringList("authorized-users"));
        saveDefaultConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(ChatColor.DARK_RED + "DoomAxe system activated!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("doomaxe")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can wield this power!");
                return true;
            }
            
            if (!checkAuthorization(player)) return true;
            
            player.getInventory().addItem(createDoomAxe());
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You feel an ominous power in your hands...");
            return true;
        }
        
        if (cmd.getName().equalsIgnoreCase("doomaxe-admin")) {
            return handleAdminCommand(sender, args);
        }
        
        return false;
    }
    
    private boolean checkAuthorization(Player player) {
        if (!authorizedUsers.contains(player.getName()) && !player.hasPermission("advancedbanther.bypass")) {
            punishUnauthorized(player);
            return false;
        }
        return true;
    }
    
    private ItemStack createDoomAxe() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        
        meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "The DoomAxe");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Instrument of ultimate punishment",
            ChatColor.DARK_RED + "WARNING: Unauthorized use will backfire"
        ));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
        meta.getPersistentDataContainer().set(doomAxeKey, PersistentDataType.BYTE, (byte) 1);
        
        axe.setItemMeta(meta);
        return axe;
    }
    
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("advancedbanther.admin")) {
            sender.sendMessage(ChatColor.RED + "You lack the authority!");
            return true;
        }
        
        if (args.length < 2 && !args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.RED + "Usage: /doomaxe-admin <add|remove|list> <player>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "add":
                authorizedUsers.add(args[1]);
                updateConfig();
                sender.sendMessage(ChatColor.GREEN + "Added " + args[1] + " to authorized users");
                break;
                
            case "remove":
                authorizedUsers.remove(args[1]);
                updateConfig();
                sender.sendMessage(ChatColor.GREEN + "Removed " + args[1] + " from authorized users");
                break;
                
            case "list":
                sender.sendMessage(ChatColor.GOLD + "Authorized Users:");
                authorizedUsers.forEach(user -> sender.sendMessage(ChatColor.YELLOW + "- " + user));
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand");
        }
        
        return true;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!isDoomAxe(weapon)) return;
        
        if (!checkAuthorization(attacker)) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getEntity() instanceof Player target) {
            executeDoom(target, attacker.getName());
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + 
                target.getName() + " was eradicated by " + attacker.getName() + "'s DoomAxe!");
        }
    }
    
    private boolean isDoomAxe(ItemStack item) {
        return item != null && 
               item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(doomAxeKey, PersistentDataType.BYTE);
    }
    
    private void executeDoom(Player target, String punisher) {
        World world = target.getWorld();
        Location loc = target.getLocation();
        
        world.strikeLightningEffect(loc);
        world.createExplosion(loc, 2F, false, false);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0F, 0.5F);
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(
            target.getName(), 
            "Smote by the DoomAxe", 
            null, 
            punisher
        );
        Bukkit.banIP(target.getAddress().getAddress().getHostAddress());
        
        target.kickPlayer(ChatColor.DARK_RED + "" + ChatColor.BOLD + 
            "YOU HAVE BEEN DOOMED!\n" + 
            ChatColor.RED + "IP banned permanently by the DoomAxe");
    }
    
    private void punishUnauthorized(Player player) {
        player.sendTitle(
            ChatColor.DARK_RED + "" + ChatColor.BOLD + "JUDGMENT", 
            ChatColor.RED + "You wielded power beyond your station", 
            10, 70, 20
        );
        executeDoom(player, "DoomAxe Auto-Justice");
    }
    
    private void updateConfig() {
        getConfig().set("authorized-users", new ArrayList<>(authorizedUsers));
        saveConfig();
    }
}
