import net.minecraft.client.main.Main;

import java.util.Arrays;
import java.util.Map;

public class Start
{
    public static void main(String[] args)
    {
        if ("true".equals(System.getProperty("sythix.web")) && "true".equals(System.getProperty("sythix.webdebug")))
        {
            // Browser build debugging: print where the game threads are every 40 seconds
            Thread watchdog = new Thread(() -> {
                while (true)
                {
                    try
                    {
                        Thread.sleep(40000L);
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }

                    for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet())
                    {
                        if (e.getKey().getName().startsWith("Client") || e.getKey().getName().startsWith("Server") || e.getKey().getName().startsWith("main"))
                        {
                            StringBuilder sb = new StringBuilder("[watchdog] " + e.getKey().getName() + " " + e.getKey().getState() + "\n");

                            for (StackTraceElement el : e.getValue())
                            {
                                sb.append("    at ").append(el).append('\n');
                            }

                            System.out.println(sb);
                        }
                    }
                }
            }, "Sythix web watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        }

        Main.main(concat(new String[] {"--version", "mcp", "--accessToken", "0", "--assetsDir", "assets", "--assetIndex", "1.8", "--userProperties", "{}"}, args));
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
