package com.daifeng.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    // 插件启动时执行 (相当于 @PostConstruct)
    @Override
    public void onEnable() {
        getLogger().info("⚡️ 李岱峰的第一个插件启动了！服务器准备起飞！");
        // 注册命令
        this.getCommand("qiandao").setExecutor(new CommandHandler(this));
        // ✅ 注册监听器 (这一步别漏了！)
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
    }

    // 插件关闭时执行
    @Override
    public void onDisable() {
        getLogger().info("😴 插件正在关闭...");
    }
}