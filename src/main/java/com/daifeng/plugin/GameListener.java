package com.daifeng.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class GameListener implements Listener {

    private final Main plugin;
    private final DatabaseManager dbManager;

    public GameListener(Main plugin) {
        this.plugin = plugin;
        this.dbManager = new DatabaseManager();
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        // 1. 判断死的是不是僵尸
        if (event.getEntityType() != EntityType.ZOMBIE) {
            return;
        }

        // 2. 判断凶手是不是玩家 (僵尸可能被骷髅射死，那种不算)
        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = event.getEntity().getKiller();
            String playerName = killer.getName();

            // 3. 核心知识点：异步操作！(Asynchronous)
            // 数据库操作是 IO 操作，很慢。如果在主线程做，服务器会卡顿。
            // 必须开一个新线程去做。
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                // --- 这里是子线程 ---
                double newBalance = dbManager.addMoney(playerName, 10.0);

                // 4. 发消息 (涉及UI或游戏逻辑，建议切回主线程，虽然发消息是线程安全的，但养成好习惯)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    killer.sendMessage("§6[奖励] §f你杀死了僵尸！获得 10 金币。余额: " + newBalance);
                });
            });
        }
    }
}