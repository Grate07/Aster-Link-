package me.astermc.link;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

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

        getLogger().info("Aster MC Link enabled.");
        getLogger().info("Geyser/Floodgate linking support enabled.");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(
                    ChatColor.RED + "Usage: /link"
            );
            return true;
        }

        FloodgateApi floodgate = FloodgateApi.getInstance();

        if (!floodgate.isFloodgatePlayer(player.getUniqueId())) {
            player.sendMessage("");
            player.sendMessage(
                    ChatColor.RED +
                    "Aster MC » You must be connected through Bedrock/Floodgate."
            );
            player.sendMessage("");
            return true;
        }

        String uuid = player.getUniqueId().toString();
        String username = player.getName();

        int codeNumber =
                ThreadLocalRandom.current().nextInt(100000, 1000000);

        String code = String.valueOf(codeNumber);

        player.sendMessage("");
        player.sendMessage(
                ChatColor.DARK_AQUA +
                "━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.AQUA +
                "        ASTER MC"
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.GRAY +
                "Your Discord linking code:"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "          " +
                ChatColor.BOLD +
                code
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.GRAY +
                "Use this code in Discord:"
        );

        player.sendMessage(
                ChatColor.AQUA +
                "          /link " +
                code
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.GRAY +
                "Expires in 5 minutes."
        );

        player.sendMessage(
                ChatColor.DARK_AQUA +
                "━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage("");

        createLinkCode(player, uuid, username, code);

        return true;
    }

    private void createLinkCode(
            Player player,
            String uuid,
            String username,
            String code
    ) {

        String json =
                "{"
                + "\"minecraftUuid\":\""
                + escapeJson(uuid)
                + "\","
                + "\"minecraftUsername\":\""
                + escapeJson(username)
                + "\","
                + "\"code\":\""
                + escapeJson(code)
                + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                apiUrl + "/api/link/create"
                        )
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .header(
                        "Authorization",
                        "Bearer " + apiSecret
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(json)
                )
                .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(response -> {

            if (response.statusCode() != 200) {

                getLogger().warning(
                        "Link API returned HTTP "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

        }).exceptionally(error -> {

            getLogger().warning(
                    "Could not connect to Aster Link API: "
                    + error.getMessage()
            );

            getServer().getScheduler().runTask(
                    this,
                    () -> player.sendMessage(
                            ChatColor.RED +
                            "Aster MC » Unable to contact linking service."
                    )
            );

            return null;
        });
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}