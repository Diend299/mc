package com.daifeng.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;

public class CommandHandler implements CommandExecutor {
    // ✅ 定义在类成员位置，加上 static final 更好（或者在构造函数初始化）
    // 用来记录“正在签到中”的玩家名字
    private final HashSet<String> signingPlayers = new HashSet<>();

    // 1. 定义一个成员变量来存这个插件实例
    private final Main plugin;

    // 2. 构造函数：接收 Main 实例
    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("qiandao")) {
            String playerName = player.getName();

            // ✅ 2. 【防并发第一关】检查 Set
            // 这一步是在主线程，是线程安全的
            if (signingPlayers.contains(playerName)) {
                player.sendMessage("§c别急！系统正在处理你的上一次请求...");
                return true; // 直接阻断，不往下走了
            }

            // ✅ 3. 【加锁】把玩家扔进 Set
            signingPlayers.add(playerName);
            player.sendMessage("§7正在连接签到中心...");

            // ✅ 1. 切换到【异步线程】查库
            // 此时主线程继续跑，玩家可以自由移动，不会卡
            Plugin plugin = null;
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {

                // 模拟网络延迟 (睡 2 秒) —— 让你体会异步的威力
                try { Thread.sleep(2000); } catch (InterruptedException e) {}

                DatabaseManager db = new DatabaseManager();
                boolean signed = db.hasSignedInToday(player.getName());

                if (signed) {
                    // ❌ 错误写法：直接在这里 player.sendMessage(...)
                    // 虽然发消息通常没事，但如果这里涉及给物品，必须回主线程

                    // ✅ 2. 切换回【主线程】反馈结果
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        player.sendMessage("§c你今天已经签到过了！");
                        signingPlayers.remove(playerName); // 移出名单
                    });
                } else {
                    // 执行签到 (写库，依然在异步线程)
                    db.doSign(player.getName());

                    // ✅ 3. 切换回【主线程】发奖励
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        player.sendMessage("§a签到成功！获得 100 金币！");
                        signingPlayers.remove(playerName); // 移出名单
                        // 播放个音效
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    });
                }
            });
            return true;
        }
        return false;
    }
}