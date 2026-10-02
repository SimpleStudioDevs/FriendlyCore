package com.friendlysmp.core.features.chatgames;

import com.friendlysmp.core.util.ConfirmationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatgamesCommand implements CommandExecutor, TabCompleter {
    private final ChatgamesFeature feature;
    private Map<UUID, Long> cooldowns = new HashMap<>();
    private long lastSent = 0;

    public ChatgamesCommand(ChatgamesFeature feature) {
        this.feature = feature;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("friendlycore.chatgames")) {
            sender.sendMessage(Component.text("You do not have permission to use that command!", NamedTextColor.RED));
            return true;
        }

        int price = feature.getConfig().getInt("price");

        if (args.length < 2) {
            sender.sendMessage(Component.text("(Price to use: " + price + " diamonds) Usage: /pcg <question in quotes> <answer in quotes>", NamedTextColor.YELLOW));
            return true;
        }

        Economy econ = feature.getEconomy();
        long cooldown = feature.getConfig().getLong("cooldown");
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeSince =  now - cooldowns.get(player.getUniqueId());
            if (timeSince <= cooldown * 60L * 1000L) {
                sender.sendMessage(Component.text("You can use this command again in " + (cooldown - timeSince / 1000L / 60L) + " minutes", NamedTextColor.RED));
                return true;
            }
        }

        if (now - lastSent < 25L * 1000L) {
            sender.sendMessage(Component.text("Please wait until the current chatgames has finished!", NamedTextColor.YELLOW));
            return true;
        }

        if (econ.getBalance(player) < price) {
            sender.sendMessage(Component.text("You need at least " + price + " diamonds to use this!", NamedTextColor.RED));
            return true;
        }

        String joinedArgs = String.join(" ", args);
        Matcher matcher = Pattern.compile("\"([^\"]*)\"\\s+\"([^\"]*)\"").matcher(joinedArgs);
        if (!matcher.matches()) {
            sender.sendMessage(Component.text("(Price to use: " + price + " diamonds) Usage: /pcg <question in quotes> <answer in quotes>", NamedTextColor.YELLOW));
            return true;
        }

        String question = matcher.group(1);
        String answer = matcher.group(2);

        String finalCommand = "cg custom trivia " + joinedArgs;
        List<Component> preview = new ArrayList<>();
        preview.add(Component.text("Question: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(question, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        preview.add(Component.text("Answer: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(answer, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));


        ConfirmationMenu menu = new ConfirmationMenu(price, preview,
                () -> {
                    econ.withdrawPlayer(player, price);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(Component.text(player.getName() + " has started a custom chat game!", NamedTextColor.GREEN));
                    }
                    cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    feature.getSchedulers().global(() ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                    player.sendMessage(Component.text("You sent a custom chat game for " + price + " diamonds", NamedTextColor.GREEN));
                    lastSent = System.currentTimeMillis();
                },
                () -> player.sendMessage(Component.text("Cancelled the custom chat game.", NamedTextColor.RED))
        );
        menu.openMenu(player);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("friendlycore.chatgames")) return List.of();

        String joinedArgs = String.join(" ", args);
        long quoteCount = joinedArgs.chars().filter(c -> c == '"').count();

        if (quoteCount % 2 != 0) {
            // Inside an open quote
            return List.of();
        }

        if (quoteCount == 0) {
            return List.of("\"question in quotes\"");
        }

        if (quoteCount == 2) {
            return List.of("\"answer in quotes\"");
        }

        return List.of();
    }
}
