package Windy.util.auth;

import com.google.gson.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;

public final class MicrosoftAuth {
    public static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    public static final int PORT = 25576;

    private static MicrosoftLoginCallback activeCallback;

    public static CompletableFuture<String> acquireMSAuthCode(final Executor executor) {
        return acquireMSAuthCode(MicrosoftAuth::openWebLink, executor);
    }
    public static CompletableFuture<String> acquireMSAuthCode(final Consumer<URI> browserAction, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            MicrosoftLoginCallback callback = null;
            try {
                synchronized (MicrosoftAuth.class) {
                    if (activeCallback != null) activeCallback.close();
                    callback = new MicrosoftLoginCallback(PORT, configuredSuccessPage());
                    activeCallback = callback;
                }
                String authUrl = "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID
                        + "&response_type=code&redirect_uri=" + URLEncoder.encode(redirectUri(), StandardCharsets.UTF_8)
                        + "&scope=XboxLive.signin+XboxLive.offline_access&state=" + callback.state() + "&prompt=select_account";
                browserAction.accept(URI.create(authUrl));
                return callback.awaitCode(5, TimeUnit.MINUTES);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                if (callback != null) callback.close();
                throw new CancellationException("Microsoft sign-in was cancelled.");
            } catch (Exception error) {
                if (callback != null) callback.failed();
                throw new CompletionException("Unable to acquire Microsoft auth code.", error);
            }
        }, executor);
    }
    private static String redirectUri() { return "http://localhost:" + PORT + "/callback"; }
    private static URI configuredSuccessPage() {
        try { return MicrosoftLoginCallback.safeSuccessPage(URI.create(System.getProperty("windy.auth.successUrl", ""))); }
        catch (IllegalArgumentException error) { return null; }
    }
    public static synchronized void completeBrowserSignIn() { if (activeCallback != null) activeCallback.successful(); }
    public static synchronized void failBrowserSignIn() { if (activeCallback != null) activeCallback.failed(); }
    public static synchronized void cancelBrowserSignIn() { if (activeCallback != null) activeCallback.cancelIfPending(); }

    public static CompletableFuture<String> acquireMSAccessToken(final String authCode, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String redirectUri = redirectUri();
                Map<String, String> params = new HashMap<>();
                params.put("client_id", CLIENT_ID);
                params.put("grant_type", "authorization_code");
                params.put("code", authCode);
                params.put("redirect_uri", redirectUri);

                String resString = postForm("https://login.live.com/oauth20_token.srf", params);
                JsonObject json = new JsonParser().parse(resString).getAsJsonObject();

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) : "There was no access token."
                        ));
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(final String accessToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", "d=" + accessToken);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");

                String resString = postJson("https://user.auth.xboxlive.com/user/authenticate", entity.toString());
                JsonObject json = new JsonParser().parse(resString).getAsJsonObject();

                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("XErr") ? json.get("Message").getAsString() : "No Xbox access token."));
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(final String accessToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                JsonArray userTokens = new JsonArray();
                userTokens.add(new JsonPrimitive(accessToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", userTokens);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");

                String resString = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", entity.toString());
                JsonObject json = new JsonParser().parse(resString).getAsJsonObject();

                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .map(token -> {
                            String uhs = json.get("DisplayClaims").getAsJsonObject()
                                    .get("xui").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("uhs").getAsString();
                            Map<String, String> result = new HashMap<>();
                            result.put("Token", token);
                            result.put("uhs", uhs);
                            return result;
                        })
                        .orElseThrow(() -> new Exception("No XSTS token."));
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(final String xstsToken, final String userHash, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject entity = new JsonObject();
                entity.addProperty("identityToken", String.format("XBL3.0 x=%s;%s", userHash, xstsToken));

                String resString = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", entity.toString());
                JsonObject json = new JsonParser().parse(resString).getAsJsonObject();

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception("No Minecraft access token."));
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(final String mcToken, final Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String resString = getJson("https://api.minecraftservices.com/minecraft/profile", mcToken);
                JsonObject json = new JsonParser().parse(resString).getAsJsonObject();

                return Optional.ofNullable(json.get("id"))
                        .map(JsonElement::getAsString)
                        .map(uuid -> new Session(
                                json.get("name").getAsString(),
                                uuid, // Surové ID bez pomlček přesně tak, jak ho vyžaduje Minecraft Session
                                mcToken,
                                "mojang"
                        ))
                        .orElseThrow(() -> new Exception("No Minecraft profile."));
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }
    private static String postJson(String urlStr, String jsonBody) throws IOException {
        return request(urlStr, "POST", "application/json; charset=UTF-8", jsonBody, null);
    }
    private static String postForm(String urlStr, Map<String, String> params) throws IOException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return request(urlStr, "POST", "application/x-www-form-urlencoded", body.toString(), null);
    }
    private static String getJson(String urlStr, String token) throws IOException {
        return request(urlStr, "GET", null, null, token);
    }
    private static String request(String urlStr, String method, String contentType, String body, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
        try {
            if (body != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) { output.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) throw new IOException("Authentication service returned HTTP " + status);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                if (status >= 300 && status < 400) throw new IOException("Unexpected authentication redirect.");
                return response.toString();
            }
        } finally { connection.disconnect(); }
    }
    public static void openWebLink(URI uri) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            throw new IllegalStateException("Open the sign-in URL in a desktop browser.");
        try { Desktop.getDesktop().browse(uri); }
        catch (IOException error) { throw new CompletionException("Unable to open the sign-in browser.", error); }
    }
}
