package org.example.belgianslotclubspring.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;

@Service
public class ShareUrlService {

    public record ShareBase(String baseUrl, String source) {
    }

    private final String configuredPublicBaseUrl;

    public ShareUrlService(
            @Value("${app.share.public-base-url:}") String configuredPublicBaseUrl
    ) {
        this.configuredPublicBaseUrl = configuredPublicBaseUrl == null
                ? ""
                : configuredPublicBaseUrl.trim();
    }

    /**
     * Base URL pour ouvrir le site depuis un téléphone :
     * domaine public configuré / Host de la requête / tunnel Cloudflare / IP Wi‑Fi.
     */
    public ShareBase resolve(String requestScheme, String requestHost, int requestPort) {
        if (!configuredPublicBaseUrl.isBlank()) {
            return new ShareBase(stripTrailingSlash(configuredPublicBaseUrl), "public");
        }

        if (isPublicHostname(requestHost)) {
            return new ShareBase(buildFromHost(requestScheme, requestHost, requestPort), "public");
        }

        if (requestHost != null && requestHost.toLowerCase(Locale.ROOT).contains("trycloudflare.com")) {
            return new ShareBase(requestScheme + "://" + requestHost, "tunnel");
        }

        Optional<String> tunnel = readTunnelUrl();
        if (tunnel.isPresent()) {
            return new ShareBase(stripTrailingSlash(tunnel.get()), "tunnel");
        }

        Optional<String> lan = detectLanIpv4();
        if (lan.isPresent()) {
            return new ShareBase(buildFromHost(requestScheme, lan.get(), requestPort > 0 ? requestPort : 8080), "lan");
        }

        String host = requestHost != null ? requestHost : "localhost";
        int port = requestPort > 0 ? requestPort : 8080;
        return new ShareBase(buildFromHost(requestScheme, host, port), "local");
    }

    private static String buildFromHost(String scheme, String host, int port) {
        String safeScheme = (scheme == null || scheme.isBlank()) ? "http" : scheme;
        boolean defaultPort = ("http".equals(safeScheme) && (port <= 0 || port == 80))
                || ("https".equals(safeScheme) && (port <= 0 || port == 443));
        return safeScheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    /** Hostname public (domaine), pas localhost / IP privée. */
    static boolean isPublicHostname(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(h) || h.endsWith(".local") || h.endsWith(".localhost")) {
            return false;
        }
        // IP ?
        if (h.chars().allMatch(c -> (c >= '0' && c <= '9') || c == '.')) {
            return !isPrivateIpv4(h);
        }
        // Contient un point → probablement un vrai domaine
        return h.contains(".");
    }

    private static boolean isPrivateIpv4(String ip) {
        try {
            byte[] b = InetAddress.getByName(ip).getAddress();
            if (b.length != 4) {
                return true;
            }
            int a = b[0] & 0xff;
            int c = b[1] & 0xff;
            if (a == 10) return true;
            if (a == 127) return true;
            if (a == 192 && c == 168) return true;
            if (a == 172 && c >= 16 && c <= 31) return true; // Docker + RFC1918
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private Optional<String> readTunnelUrl() {
        if (!isTunnelProcessAlive()) {
            return Optional.empty();
        }
        Path file = Path.of(System.getProperty("user.dir"), ".cloudflared.url");
        if (!Files.isRegularFile(file)) {
            return readTunnelUrlFromLog();
        }
        try {
            String url = Files.readString(file).trim();
            if (url.startsWith("https://") && url.contains("trycloudflare.com")) {
                return Optional.of(url);
            }
        } catch (IOException ignored) {
            // ignore
        }
        return readTunnelUrlFromLog();
    }

    private boolean isTunnelProcessAlive() {
        try {
            Process process = new ProcessBuilder("pgrep", "-f", "cloudflared tunnel --url http://localhost:8080")
                    .redirectErrorStream(true)
                    .start();
            int code = process.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<String> readTunnelUrlFromLog() {
        Path log = Path.of(System.getProperty("user.dir"), ".cloudflared.log");
        if (!Files.isRegularFile(log)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(log);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("https://[a-zA-Z0-9.-]+\\.trycloudflare\\.com")
                    .matcher(content);
            String last = null;
            while (m.find()) {
                last = m.group();
            }
            if (last != null) {
                Files.writeString(Path.of(System.getProperty("user.dir"), ".cloudflared.url"), last);
                return Optional.of(last);
            }
        } catch (IOException ignored) {
            // ignore
        }
        return Optional.empty();
    }

    private Optional<String> detectLanIpv4() {
        try {
            String preferred = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(interfaces)) {
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) {
                    continue;
                }
                String name = nif.getName() == null ? "" : nif.getName().toLowerCase(Locale.ROOT);
                // Évite Docker / bridges virtuels (souvent 172.17.0.1)
                if (name.startsWith("docker") || name.startsWith("br-") || name.startsWith("veth")
                        || name.startsWith("virbr") || name.equals("cni0")) {
                    continue;
                }
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address ipv4)
                            || ipv4.isLoopbackAddress()
                            || !ipv4.isSiteLocalAddress()) {
                        continue;
                    }
                    String ip = ipv4.getHostAddress();
                    if (ip.startsWith("172.1") || ip.startsWith("172.2") || ip.startsWith("172.3")) {
                        // Plage Docker typique — on garde seulement en dernier recours
                        if (preferred == null) {
                            preferred = ip;
                        }
                        continue;
                    }
                    // Préfère 192.168.x / 10.x (vrai LAN)
                    return Optional.of(ip);
                }
            }
            return Optional.ofNullable(preferred);
        } catch (Exception ignored) {
            // ignore
        }
        return Optional.empty();
    }

    private static String stripTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
