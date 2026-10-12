# Sythix multiplayer proxy

The browser version of Sythix can't open game connections by itself (browsers only allow web
connections), so it joins servers through this small proxy:

```
browser (Sythix)  --WebSocket-->  sythix_proxy.py  --normal connection-->  any Minecraft server
```

It is one Python file with no extra packages. It needs a computer that is always on and reachable
from the internet, for example a free cloud server.

## 1. Get a free server

Any Linux server works (a $3-5/month VPS, or free tiers). Oracle Cloud's "Always Free" tier is a
good free choice:

1. Sign up at https://www.oracle.com/cloud/free/ (needs a card for identity checks; Always Free
   resources are not charged).
2. Create a VM instance: image **Ubuntu**, shape **VM.Standard.E2.1.Micro** or **Ampere A1**
   (both Always Free). Download the SSH key it offers.
3. In the instance's **Virtual Cloud Network > Security List**, add ingress rules for TCP ports
   **80** and **443** from `0.0.0.0/0`.
4. Connect: `ssh -i your-key ubuntu@YOUR_SERVER_IP`, then open the same ports on the server itself:
   `sudo iptables -I INPUT -p tcp -m multiport --dports 80,443 -j ACCEPT && sudo netfilter-persistent save`

## 2. Get a name for it (free)

Browsers on an `https://` site may only use secure `wss://` proxies, which need a domain name.
Free option: https://www.duckdns.org - sign in, pick a name like `sythix-proxy`, and set it to
your server's IP. You now have `sythix-proxy.duckdns.org`.

## 3. Run the proxy

```bash
sudo apt update && sudo apt install -y python3 caddy
sudo mkdir -p /opt/sythix && sudo cp sythix_proxy.py /opt/sythix/   # copy the file to the server first (scp)
```

Make it start by itself and restart if it stops - create `/etc/systemd/system/sythix-proxy.service`:

```ini
[Unit]
Description=Sythix multiplayer proxy
After=network-online.target

[Service]
ExecStart=/usr/bin/python3 /opt/sythix/sythix_proxy.py --host 127.0.0.1 --port 8765 --behind-proxy --origin https://YOUR-SITE
Restart=always
DynamicUser=yes

[Install]
WantedBy=multi-user.target
```

Replace `https://YOUR-SITE` with your game website (for example `https://sysamiryasar.github.io`),
so only your site can use the proxy. Then: `sudo systemctl enable --now sythix-proxy`

## 4. Secure connection (wss://) with Caddy

Caddy gets a free HTTPS certificate by itself. Put this in `/etc/caddy/Caddyfile`:

```
sythix-proxy.duckdns.org {
	reverse_proxy 127.0.0.1:8765
}
```

`sudo systemctl reload caddy`. Check it: opening https://sythix-proxy.duckdns.org in a browser
shows "Sythix multiplayer proxy is running."

## 5. Tell the website about it

Put a `config.json` next to `index.html` on your website:

```json
{"proxy": "wss://sythix-proxy.duckdns.org"}
```

Players can also type a proxy on the start page (Multiplayer and speed), and links work too:
`https://YOUR-SITE/?proxy=wss://sythix-proxy.duckdns.org&server=play.example.com` opens the game
and joins that server.

## Options

| Option | What it does |
| --- | --- |
| `--port 8765` | port to listen on |
| `--ports 25565-25575` | Minecraft ports players may join |
| `--allow "*.example.com,mc.other.net"` | only these servers (default: any public server) |
| `--origin https://your.site` | only pages from your site may use it |
| `--per-ip 4` / `--max 300` | connection limits per player / in total |
| `--cert c.pem --key k.pem` | serve `wss://` directly without Caddy |
| `--behind-proxy` | trust Caddy/nginx's `X-Forwarded-For` (for the per-player limit) |
| `--allow-private` | also allow local addresses - only for testing on your own computer |

Safety built in: only Minecraft ports, never private/internal addresses (nobody can use your proxy
to reach your own network), connection limits, and SRV records are looked up like the normal game
does (so `play.example.com` works when the server uses one).

Servers that require a Microsoft account (online mode) still won't let the browser version in;
it plays on servers that allow offline-mode players.
