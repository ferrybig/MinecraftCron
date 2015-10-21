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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Fernando
 */
public class Main extends JavaPlugin {

	Scheduler schedular;
	Map<String, List<String>> commands = new HashMap<>();

	@Override
	public void onEnable() {
		super.onEnable();
		File config = new File(this.getDataFolder(), "tasks.txt");
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdir();
		}
		if (!config.exists()) {
			try {
				try (PrintStream p = new PrintStream(new FileOutputStream(config), false, "UTF-8")) {
					p.println("# Crontab syntax :");
					p.println("# A crontab file has five fields for specifying day , date and time followed by the command to be run at that interval.");
					p.println("# *     *     *   *    * :     command to be executed");
					p.println("# -     -     -   -    -");
					p.println("# |     |     |   |    |");
					p.println("# |     |     |   |    +----- day of week (0 - 6) (Sunday=0)");
					p.println("# |     |     |   +------- month (1 - 12)");
					p.println("# |     |     +--------- day of        month (1 - 31)");
					p.println("# |     +----------- hour (0 - 23)");
					p.println("# +------------- min (0 - 59)");
					p.println("# * in the value field above means all legal values as in braces for that column.");
					p.println("# The value column can have a * or a list of elements separated by commas. An element is either a number in the ranges shown above or two numbers in the range separated by a hyphen (meaning an inclusive range).");
					p.println("# ");
					p.println("# Examples: ");
					p.println("# 0 7 * * *: say hi");
					p.println("# Says hi a 7 o clock ");
					p.println("# ");
					p.println("# 0 * * * *: backup ");
					p.println("# Hourly backup ");
					p.println("# ");
					p.println("# * * * * *: say minutes ticking... ");
					p.println("# message every minute ");
					p.println("# ");
					p.println("# Warning: command should be placed without the / in front of it");
					p.println("# ");
					p.println("# ");
				}
			} catch (IOException ex) {

				this.getLogger().log(Level.SEVERE, null, ex);
			}
			this.getLogger().log(Level.INFO, "Succesfully created datafolder, now its time to configure the plugin!");
			this.setEnabled(false);
			return;
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
		if(commands.isEmpty()) {
			this.getLogger().log(Level.WARNING, "No tasks found, plz follow the instructions inside the config file how to add a tasks");
			schedular = null;
			this.setEnabled(false);
			return;
		}
		schedular.start();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		if(schedular != null)
			schedular.stop();
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
