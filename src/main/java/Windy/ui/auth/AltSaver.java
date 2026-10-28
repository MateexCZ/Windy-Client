package Windy.ui.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class AltSaver {
    private static final File ALT_FILE = new File(Minecraft.getMinecraft().mcDataDir, "windy_alts.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<AltAccount> accounts = new ArrayList<>();

    public static class AltAccount {
        public String username;
        public String uuid;
        public String token;
        public boolean isCracked;
        public boolean isMicrosoft;

        public AltAccount(String username, String uuid, String token, boolean isCracked, boolean isMicrosoft) {
            this.username = username;
            this.uuid = uuid;
            this.token = token;
            this.isCracked = isCracked;
            this.isMicrosoft = isMicrosoft;
        }
    }

    public static void loadAlts() {
        if (ALT_FILE.exists()) {
            try (FileReader reader = new FileReader(ALT_FILE)) {
                List<AltAccount> loaded = GSON.fromJson(reader, new TypeToken<List<AltAccount>>(){}.getType());
                if (loaded != null) {
                    accounts = loaded;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveAlts() {
        try (FileWriter writer = new FileWriter(ALT_FILE)) {
            GSON.toJson(accounts, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addAlt(String username, String uuid, String token, boolean isCracked, boolean isMicrosoft) {
        accounts.removeIf(a -> a.username.equalsIgnoreCase(username));
        accounts.add(new AltAccount(username, uuid, token, isCracked, isMicrosoft));
        saveAlts();
    }

    public static void removeAlt(AltAccount alt) {
        accounts.remove(alt);
        saveAlts();
    }
}
