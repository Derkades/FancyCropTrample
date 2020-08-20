package xyz.derkades.trample;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Trample extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		super.saveDefaultConfig();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onTrample(final PlayerInteractEvent event){
	    if (event.getAction() != Action.PHYSICAL){
	    	return;
	    }
	    
		final Block soil = event.getClickedBlock();
		if (soil == null || soil.getType() != Material.FARMLAND) {
			return;
		}

		event.setUseInteractedBlock(PlayerInteractEvent.Result.DENY);
		event.setCancelled(true);

		// Dehydrate soil and hydrate after a while
		soil.setType(Material.FARMLAND);
		final Farmland farmland = (Farmland) soil.getBlockData();
		farmland.setMoisture(0);
		Bukkit.getScheduler().runTaskLater(this, () -> farmland.setMoisture(farmland.getMaximumMoisture()), 40);
	
		final String mode = getConfig().getString("mode");
		
		if (mode.equals("hydrate")) {
			return;
		}
		
		final Block crop = soil.getRelative(BlockFace.UP);
		
		if (!(crop.getBlockData() instanceof Ageable)) {
			return;
		}
		
		final Ageable ageable = (Ageable) crop.getBlockData();
		
		final int targetAge;
			
		if (mode.equals("original")) {
			targetAge = ageable.getAge();
		} else if (mode.equals("full")) {
			targetAge = ageable.getMaximumAge();
		} else if (mode.equals("random")) {
			targetAge = ThreadLocalRandom.current().nextInt(0, ageable.getMaximumAge());
		} else {
			targetAge = 0;
			event.getPlayer().sendMessage("FancyCropTrample: Invalid mode");
		}

		ageable.setAge(0);
		//apparently since 1.16 it needs get/set
		crop.setBlockData(ageable);
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!(crop.getBlockData() instanceof Ageable)) {
					this.cancel();
					return;
				}
				
				final Ageable ageable2 = (Ageable) crop.getBlockData();
				final int age = ageable2.getAge();

				if (age >= targetAge) {
					this.cancel();
					return;
				}

				ageable2.setAge(age + 1);
				crop.setBlockData(ageable2);
			}
		}.runTaskTimer(this, 0, 20);
	}

}
