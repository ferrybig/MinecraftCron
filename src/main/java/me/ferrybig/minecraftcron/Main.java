/* Copyright © 2000 Your Name <your@address>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */
package me.ferrybig.minecraftcron;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulingPattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Fernando
 */
public class Main extends JavaPlugin {

	private Scheduler schedular;
	private final Map<String, List<String>> commands = new HashMap<>();

	@Override
	public void onEnable() {
		super.onEnable();
		config = new File(this.getDataFolder(), "tasks.txt");
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdir();
		}
		if (!config.exists()) {
			saveResource(config.getName(), false);
			this.getLogger().log(Level.INFO, "Succesfully created datafolder, now it is time to configure the plugin!");
			this.setEnabled(false);
			return;
		}

		if (!load(config)) {
			this.setEnabled(false);
			this.getLogger().log(Level.WARNING, "No tasks found, plz follow the instructions inside the config file how to add a tasks");
		}
	}
	private File config;

	private boolean load(File config) throws IllegalStateException {
		if (this.schedular != null) {
			this.schedular.stop();
			this.schedular = null;
		}
		schedular = new Scheduler();
		try {
			try (BufferedReader p = new BufferedReader(new InputStreamReader(new FileInputStream(config), "UTF-8"))) {
				String line;
				while ((line = p.readLine()) != null) {
					if (line.length() < 10 || line.charAt(0) == '#') {
						continue;
					}
					String[] args = line.split(":", 2);
					args[0] = args[0].trim();
					args[1] = args[1].trim();
					try {
						SchedulingPattern s = new SchedulingPattern(args[0]);
						args[0] = s.toString();
						List<String> tasks = this.commands.get(args[0]);
						if (tasks == null) {
							tasks = new CopyOnWriteArrayList<>();
							this.commands.put(args[0], tasks);
							schedular.schedule(args[0], new AsyncTaskRunner(tasks));
						}
						tasks.add(args[1]);
					} catch (InvalidPatternException ex) {
						this.getLogger().log(Level.WARNING, "Invalid pattern: {0}: {1}", new Object[]{args[0], ex.toString()});
					}
				}
			}
		} catch (IOException ex) {
			this.getLogger().log(Level.SEVERE, null, ex);
		}
		if (commands.isEmpty()) {
			schedular = null;
			return true;
		}
		schedular.start();
		return false;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		if (schedular != null) {
			schedular.stop();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("minecraftcronreload")) {
			if (!command.testPermission(sender)) {
				return true;
			}
			if (this.load(config)) {
				int commands = 0;
				for (List<?> c : this.commands.values()) {
					commands += c.size();
				}
				sender.sendMessage("Tasks loaded with " + commands + " tasks");
			} else {
				sender.sendMessage("No tasks found!");
			}
			return true;
		} else {
			throw new IllegalArgumentException("Command " + command.getName() + " incorrectly implemented for this plugin");
		}
	}

	private class TaskRunner implements Runnable {

		private final List<String> t;

		public TaskRunner(List<String> t) {
			this.t = t;
		}

		@Override
		public void run() {
			getLogger().info("Running tasks...");
			for (String task : t) {
				getServer().dispatchCommand(getServer().getConsoleSender(), task);
			}
		}
	}

	private class AsyncTaskRunner implements Runnable {

		private final Runnable syncTask;

		public AsyncTaskRunner(List<String> t) {
			this.syncTask = new TaskRunner(t);
		}

		@Override
		public void run() {
			getServer().getScheduler().runTask(Main.this, syncTask);
		}
	}

}
