package net.sythix.client.module.advanced;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import net.sythix.client.SythixClient;
import net.sythix.client.core.Notifications;
import net.sythix.client.module.Category;
import net.sythix.client.module.Module;
import net.sythix.client.setting.BooleanSetting;
import net.sythix.client.setting.ButtonSetting;
import net.sythix.client.setting.InfoSetting;
import net.sythix.client.setting.TextSetting;
import org.apache.commons.io.IOUtils;

/**
 * #60 Shows the client version and can check a URL you choose for a newer one. Off by default:
 * nothing is downloaded or installed automatically, and only the version text is fetched.
 *
 * The URL should return either a plain version ("0.3.0") or JSON with a "version" field.
 */
public class UpdateManager extends Module
{
    private final BooleanSetting checkOnStart;
    private final TextSetting url;
    private volatile String latest = "Not checked";
    private boolean started;

    public UpdateManager()
    {
        super("Update Manager", "Version info and an optional update check against a URL you set.", Category.ADVANCED, true);
        this.setCore();
        this.add(new InfoSetting("Installed", "Your Sythix version.", () -> SythixClient.VERSION + " (Minecraft " + SythixClient.MINECRAFT_VERSION + ")"));
        this.add(new InfoSetting("Latest", "Result of the last check.", () -> this.latest));
        this.url = this.add(new TextSetting("Update URL", "Where to read the latest version (https). Leave empty to never check.", "", 200));
        this.checkOnStart = this.add(new BooleanSetting("Check On Start", "Check the URL once when the game starts.", false));
        this.add(new ButtonSetting("Check Now", "Check the URL now.", "Check", () -> this.check(true)));
    }

    public void onTick()
    {
        if (!this.started)
        {
            this.started = true;

            if (this.checkOnStart.isOn())
            {
                this.check(false);
            }
        }
    }

    private void check(boolean manual)
    {
        String address = this.url.get().trim();

        if (!address.startsWith("https://"))
        {
            this.latest = "No https URL set";

            if (manual)
            {
                Notifications.post("Update check", "Set an https Update URL first.", Notifications.Kind.WARNING);
            }

            return;
        }

        this.latest = "Checking...";
        Thread thread = new Thread(() -> {
            String result;
            boolean newer = false;

            try
            {
                HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestProperty("User-Agent", SythixClient.FULL_NAME + "/" + SythixClient.VERSION);

                try (InputStream in = c.getInputStream())
                {
                    String body = IOUtils.toString(in, StandardCharsets.UTF_8).trim();

                    if (body.length() > 4096)
                    {
                        body = body.substring(0, 4096);
                    }

                    String version = body.startsWith("{") ? new com.google.gson.JsonParser().parse(body).getAsJsonObject().get("version").getAsString() : body.split("\\s+")[0];
                    newer = compare(version, SythixClient.VERSION) > 0;
                    result = version + (newer ? " (update available)" : " (up to date)");
                }
            }
            catch (Exception e)
            {
                result = "Check failed: " + e.getClass().getSimpleName();
            }

            String text = result;
            boolean update = newer;
            this.mc.addScheduledTask(() -> {
                this.latest = text;

                if (manual || update)
                {
                    Notifications.post("Update check", text, update ? Notifications.Kind.SUCCESS : Notifications.Kind.INFO);
                }
            });
        }, "Sythix update check");
        thread.setDaemon(true);
        thread.start();
    }

    /** Compares dotted version numbers ("0.10.1" > "0.9"). */
    static int compare(String a, String b)
    {
        String[] x = a.replaceAll("[^0-9.]", "").split("\\.");
        String[] y = b.replaceAll("[^0-9.]", "").split("\\.");

        for (int i = 0; i < Math.max(x.length, y.length); ++i)
        {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;

            if (p != q)
            {
                return Integer.compare(p, q);
            }
        }

        return 0;
    }
}
