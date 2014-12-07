/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcmiddleearth.enforcersuite;

import com.mcmiddleearth.enforcersuite.DBmanager.DBmanager;
import com.mcmiddleearth.enforcersuite.DBmanager.LocationIndex;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
//Oathbreaker, Thrall, Commoner, Ranger, Artist, Foreman, Artisan, Steward, Enforcer, Valar
/**
 *
 * @author Donovan, aaldim
 */
public class EnforcerSuite extends JavaPlugin{
    @Getter
    private static EnforcerSuite plugin;
    
    @Getter
    private static String prefix = ChatColor.GOLD +"["+ ChatColor.YELLOW +"EnforcerSuite"+ ChatColor.GOLD +"] "+ ChatColor.YELLOW;
    
    @Getter
    private String FileSep = System.getProperty("file.separator");
    
    @Getter @Setter
    private World MainWorld;
            
    @Override
    public void onEnable(){
        this.saveDefaultConfig();
        this.reloadConfig();
        plugin = this;
        getCommand("ob").setExecutor(new Commands());
        getCommand("done").setExecutor(new Commands());
        LocationIndex.loadLocs();
        MainWorld = Bukkit.getWorld(this.getConfig().getString("MainWorld"));

    }
    @Override
    public void onDisable(){
        
    }
}