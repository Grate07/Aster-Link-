package me.astermc.link;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class AsterLink extends JavaPlugin {

    private String apiUrl;
    private String apiSecret;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        apiUrl = getConfig().getString("api-url");
        apiSecret = getConfig().getString("api-secret");

        getLogger().info("AsterLink has been enabled!");
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

        if (!command.getName().equalsIgnoreCase("link")) {
            return true;
        }

        FloodgateApi floodgate = FloodgateApi.getInstance();

        if (!floodgate.isFloodgatePlayer(player.getUniqueId())) {
            player.sendMessage("§cAsterLink is currently available for Bedrock players only.");
            return true;
        }

        String code = generateCode();

        player.sendMessage("§8§m--------------------------");
        player.sendMessage("§b§lASTER MC §7| Discord Linking");
        player.sendMessage("");
        player.sendMessage("§fYour linking code:");
        player.sendMessage("§b§l" + code);
        player.sendMessage("");
        player.sendMessage("§7Use §f/link " + code + " §7in the Aster MC Discord.");
        player.sendMessage("§7This code expires in §f5 minutes§7.");
        player.sendMessage("§8§m--------------------------");

        sendCodeToApi(player, code);

        return true;
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private void sendCodeToApi(Player player, String code) {

        getServer().getScheduler().runTaskAsynchronously(this, () -> {

            try {

                URI uri = URI.create(apiUrl + "/api/link/create");

                HttpURLConnection connection =
                        (HttpURLConnection) uri.toURL().openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + apiSecret
                );

                connection.setDoOutput(true);

                String json = """
                        {
                          "minecraftUuid": "%s",
                          "minecraftUsername": "%s",
                          "code": "%s"
                        }
                        """.formatted(
                        player.getUniqueId(),
                        escapeJson(player.getName()),
                        code
                );

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(
                            json.getBytes(StandardCharsets.UTF_8)
                    );
                }

                int responseCode = connection.getResponseCode();

                if (responseCode != 200) {

                    getLogger().warning(
                            "AsterLink API returned HTTP " + responseCode
                    );

                    player.sendMessage(
                            "§cCould not create your linking code. Please try again."
                    );
                }

                connection.disconnect();

            } catch (Exception error) {

                getLogger().warning(
                        "Could not connect to AsterLink API: "
                                + error.getMessage()
                );

                player.sendMessage(
                        "§cCould not connect to the Aster MC linking service."
                );
            }
        });
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}