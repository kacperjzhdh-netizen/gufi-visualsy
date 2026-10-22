package pl.fazevisuals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class LicenseManager {
    // ZMIEN na adres swojego serwera licencji (najlepiej HTTPS)
    private static final String API_URL = "https://TWOJA-DOMENA.pl/verify";
    private static final long RECHECK_MS = 5 * 60 * 1000L;

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fazevisuals.json");

    private static volatile boolean active = false;
    private static volatile long expiresAt = 0; // epoch seconds
    private static volatile String message = "brak klucza - wpisz /fazevisuals key <klucz>";
    private static volatile boolean checking = false;
    private static volatile long lastCheck = 0;
    private static String key = "";

    private LicenseManager() {}

    public static void init() {
        try {
            if (Files.exists(FILE)) {
                JsonObject o = GSON.fromJson(Files.readString(FILE), JsonObject.class);
                if (o != null && o.has("key")) key = o.get("key").getAsString();
            }
        } catch (Exception ignored) {}
        if (!key.isEmpty()) verifyAsync();
    }

    public static void setKey(String k) {
        key = k.trim();
        try {
            JsonObject o = new JsonObject();
            o.addProperty("key", key);
            Files.writeString(FILE, GSON.toJson(o));
        } catch (Exception ignored) {}
        verifyAsync();
    }

    public static boolean isActive() { return active; }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (active && now / 1000 >= expiresAt) {
            active = false;
            message = "licencja wygasla";
        }
        if (!key.isEmpty() && !checking && now - lastCheck > RECHECK_MS) verifyAsync();
    }

    public static String statusText() {
        return active ? "licencja aktywna (" + timeLeftText() + ")" : message;
    }

    public static String timeLeftText() {
        if (expiresAt >= 4102444000L) return "Licencja: lifetime";
        long left = Math.max(0, expiresAt - System.currentTimeMillis() / 1000);
        long d = left / 86400, h = (left % 86400) / 3600, m = (left % 3600) / 60;
        return "Licencja: " + d + "d " + h + "h " + m + "m";
    }

    private static void verifyAsync() {
        if (checking || key.isEmpty()) return;
        checking = true;
        lastCheck = System.currentTimeMillis();
        String nick = MinecraftClient.getInstance().getSession().getUsername();

        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        body.addProperty("nick", nick);

        HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
            try {
                if (err != null || resp == null) {
                    // brak polaczenia z serwerem: zachowaj stan do lokalnego wygasniecia
                    if (!active) message = "nie mozna polaczyc z serwerem licencji";
                    return;
                }
                JsonObject o = GSON.fromJson(resp.body(), JsonObject.class);
                if (o != null && o.has("valid") && o.get("valid").getAsBoolean()) {
                    expiresAt = o.get("expires").getAsLong();
                    active = true;
                    message = "ok";
                } else {
                    active = false;
                    message = (o != null && o.has("reason")) ? o.get("reason").getAsString() : "nieprawidlowy klucz";
                }
            } catch (Exception e) {
                if (!active) message = "blad odpowiedzi serwera";
            } finally {
                checking = false;
            }
        });
    }
}
