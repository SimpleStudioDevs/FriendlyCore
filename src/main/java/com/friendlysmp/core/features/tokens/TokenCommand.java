package com.friendlysmp.core.features.tokens;

import com.friendlysmp.core.FriendlyCorePlugin;
import com.friendlysmp.core.features.tokens.TokenService.GiveResult;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TokenCommand implements TabExecutor {
    private final FriendlyCorePlugin plugin;
    private final TokenService service;

    public TokenCommand(FriendlyCorePlugin plugin, TokenService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use /token claim.");
                return true;
            }
            service.handleClaim(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("friendlycore.tokens.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadFriendlyCore();
            sender.sendMessage("§aFriendlyCore reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("lastcollection")) {
            if (!sender.hasPermission("friendlycore.tokens.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }

            if (args.length != 3 && args.length != 4) {
                sender.sendMessage("§eUsage: /token lastcollection <player> <month>");
                sender.sendMessage("§eUsage: /token lastcollection <player> <year> <month>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }

            try {
                int year;
                int month;

                if (args.length == 3) {
                    year = LocalDate.now().getYear();
                    month = Integer.parseInt(args[2]);
                } else {
                    year = Integer.parseInt(args[2]);
                    month = Integer.parseInt(args[3]);
                }

                if (month < 1 || month > 12) {
                    sender.sendMessage("§cMonth must be between 1 and 12.");
                    return true;
                }

                service.setLastCollection(target, year, month);
                sender.sendMessage("§aSet last collection for " + target.getName() + " to " + year + "-" + String.format("%02d", month) + ".");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            } catch (Exception e) {
                sender.sendMessage("§cFailed to set last collection.");
                plugin.getLogger().warning("Failed to set last collection: " + e.getMessage());
            }

            return true;
        }

        if (!sender.hasPermission("friendlycore.tokens.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    sender.sendMessage("§cAmount must be greater than 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return true;
            }
        }

        TokenService.GiveResult result = service.handleAdminGive(target, amount);

        if (result == TokenService.GiveResult.GIVEN_NOW) {
            sender.sendMessage("§aGave " + amount + " Shop Token(s) to " + target.getName() + ".");
            return true;
        }

        if (result == TokenService.GiveResult.STORED_FOR_CLAIM) {
            sender.sendMessage("§e" + target.getName() + " could not receive the token(s) right now, so they were stored for /token claim.");
            if (target.isOnline()) {
                Player player = (Player) target;
                player.sendMessage("§eYou received " + amount + " token(s), but they were stored for claim because your inventory was full or you were in an excluded world. /token claim");
            }
            return true;
        }

        sender.sendMessage("§cFailed to give tokens to " + target.getName() + ".");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/token claim");
        sender.sendMessage("§e/token <player> [amount]");
        sender.sendMessage("§e/token lastcollection <player> <month>");
        sender.sendMessage("§e/token lastcollection <player> <year> <month>");
        sender.sendMessage("§e/token reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            out.add("claim");
            if (sender.hasPermission("friendlycore.tokens.admin")) {
                out.add("reload");
                out.add("lastcollection");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    out.add(p.getName());
                }
            }
            return filter(out, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("lastcollection")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    out.add(p.getName());
                }
                return filter(out, args[1]);
            }

            if (!args[0].equalsIgnoreCase("claim") && !args[0].equalsIgnoreCase("reload")) {
                out.add("<amount>");
                return filter(out, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("lastcollection")) {
            for (int i = 1; i <= 12; i++) {
                out.add(String.valueOf(i));
            }
            return filter(out, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("lastcollection")) {
            for (int i = 1; i <= 12; i++) {
                out.add(String.valueOf(i));
            }
            return filter(out, args[3]);
        }

        return List.of();
    }

    private List<String> filter(List<String> input, String startsWith) {
        String lower = startsWith.toLowerCase();
        return input.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .distinct()
                .sorted()
                .toList();
    }
}