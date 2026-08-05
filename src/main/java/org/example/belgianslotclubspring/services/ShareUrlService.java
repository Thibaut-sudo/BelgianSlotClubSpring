package org.example.belgianslotclubspring.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

@Service
public class ShareUrlService {

    public record ShareBase(String baseUrl, String source) {
    }

    /**
     * Base URL pour ouvrir le site depuis un téléphone :
     * tunnel Cloudflare (4G) si actif, sinon IP Wi‑Fi locale.
     */
    public ShareBase resolve(String requestScheme, String requestHost, int requestPort) {
        if (requestHost != null && requestHost.contains("trycloudflare.com")) {
            return new ShareBase(requestScheme + "://" + requestHost, "tunnel");
        }

        Optional<String> tunnel = readTunnelUrl();
        if (tunnel.isPresent()) {
            return new ShareBase(stripTrailingSlash(tunnel.get()), "tunnel");
        }

        Optional<String> lan = detectLanIpv4();
        if (lan.isPresent()) {
            int port = requestPort > 0 ? requestPort : 8080;
            boolean defaultPort = ("http".equals(requestScheme) && port == 80)
                    || ("https".equals(requestScheme) && port == 443);
            String base = requestScheme + "://" + lan.get() + (defaultPort ? "" : ":" + port);
            return new ShareBase(base, "lan");
        }

        String host = requestHost != null ? requestHost : "localhost";
        int port = requestPort > 0 ? requestPort : 8080;
        boolean defaultPort = ("http".equals(requestScheme) && port == 80)
                || ("https".equals(requestScheme) && port == 443);
        String base = requestScheme + "://" + host + (defaultPort ? "" : ":" + port);
        return new ShareBase(base, "local");
    }

    private Optional<String> readTunnelUrl() {
        if (!isTunnelProcessAlive()) {
            return Optional.empty();
        }
        Path file = Path.of(System.getProperty("user.dir"), ".cloudflared.url");
        if (!Files.isRegularFile(file)) {
            // Essayer de récupérer l'URL depuis le log cloudflared
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
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(interfaces)) {
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address ipv4
                            && !ipv4.isLoopbackAddress()
                            && ipv4.isSiteLocalAddress()) {
                        return Optional.of(ipv4.getHostAddress());
                    }
                }
            }
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
