package me.astermc.link;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class AsterLink extends JavaPlugin implements CommandExecutor {

    private HttpClient httpClient;
    private String apiUrl;
    private String apiSecret;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        apiUrl = getConfig().getString("api-url");
        apiSecret = getConfig().getString("api-secret");

        httpClient = HttpClient.newHttpClient();

        if (getCommand("link") != null) {
            getCommand("link").setExecutor(this);
        }

        getLogger().info("Aster MC Link enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Aster MC Link disabled.");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(
                    ChatColor.RED + "Usage: /link"
            );
            return true;
        }

        int codeNumber = 100000 + new Random().nextInt(900000);
        String code = String.valueOf(codeNumber);

        String uuid = player.getUniqueId().toString();
        String username = player.getName();

        player.sendMessage("");
        player.sendMessage(
                ChatColor.AQUA + "Aster MC " +
                ChatColor.DARK_GRAY + "» " +
                ChatColor.GRAY + "Generating your linking code..."
        );

        String json = "{"
                + "\"minecraftUuid\":\"" + escapeJson(uuid) + "\","
                + "\"minecraftUsername\":\"" + escapeJson(username) + "\","
                + "\"code\":\"" + escapeJson(code) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/api/link/create"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiSecret)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(response -> {

            getServer().getScheduler().runTask(
                    this,
                    () -> {

                        if (response.statusCode() == 200) {

                            player.sendMessage("");
                            player.sendMessage(
                                    ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━"
                            );

                            player.sendMessage(
                                    ChatColor.AQUA + "      Aster MC"
                            );

                            player.sendMessage("");

                            player.sendMessage(
                                    ChatColor.GRAY +
                                    "Your Discord linking code is:"
                            );

                            player.sendMessage(
                                    ChatColor.WHITE + "          " +
                                    ChatColor.BOLD + code
                            );

                            player.sendMessage("");

                            player.sendMessage(
                                    ChatColor.GRAY +
                                    "Open Discord and use:"
                            );

                            player.sendMessage(
                                    ChatColor.AQUA +
                                    "          /link " + code
                            );

                            player.sendMessage("");

                            player.sendMessage(
                                    ChatColor.GRAY +
                                    "This code expires in 5 minutes."
                            );

                            player.sendMessage(
                                    ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━"
                            );

                            player.sendMessage("");

                        } else {

                            player.sendMessage(
                                    ChatColor.RED +
                                    "Aster MC » Failed to create linking code."
                            );

                            getLogger().warning(
                                    "Link API returned HTTP " +
                                    response.statusCode() +
                                    ": " +
                                    response.body()
                            );
                        }
                    }
            );

        }).exceptionally(error -> {

            getServer().getScheduler().runTask(
                    this,
                    () -> player.sendMessage(
                            ChatColor.RED +
                            "Aster MC » Could not connect to the linking API."
                    )
            );

            getLogger().warning(
                    "Could not contact Aster MC Link API: " +
                    error.getMessage()
            );

            return null;
        });

        return true;
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}