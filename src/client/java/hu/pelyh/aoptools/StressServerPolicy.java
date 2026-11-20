package hu.pelyh.aoptools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class StressServerPolicy {
    private StressServerPolicy() { }

    static String normalize(String host, int port) throws UnknownHostException {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("A port 1 és 65535 között lehet.");
        String cleanHost = host == null ? "" : host.trim();
        if (cleanHost.isEmpty()) throw new IllegalArgumentException("A szervercím üres.");
        if (!cleanHost.equalsIgnoreCase("localhost") && !looksLikeIpLiteral(cleanHost))
            throw new IllegalArgumentException("Csak localhost vagy közvetlen privát IP-cím használható.");
        InetAddress address = InetAddress.getByName(stripBrackets(cleanHost));
        if (!isPrivate(address)) throw new IllegalArgumentException("Csak loopback vagy privát LAN-cím whitelistelhető.");
        return endpoint(address, port);
    }

    static List<String> canonicalEntries(List<String> entries) {
        Set<String> result = new LinkedHashSet<>();
        if (entries == null) return List.of();
        for (String entry : entries) {
            try {
                Parsed parsed = parse(entry);
                result.add(normalize(parsed.host(), parsed.port()));
                if (result.size() >= 64) break;
            } catch (RuntimeException | UnknownHostException ignored) { }
        }
        return List.copyOf(result);
    }

    static String endpoint(InetAddress address, int port) {
        String host = address.getHostAddress();
        return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
    }

    private static Parsed parse(String entry) {
        String value = entry == null ? "" : entry.trim();
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close < 0 || close + 2 > value.length() || value.charAt(close + 1) != ':')
                throw new IllegalArgumentException("Hibás IPv6 whitelist-bejegyzés.");
            return new Parsed(value.substring(1, close), Integer.parseInt(value.substring(close + 2)));
        }
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || value.indexOf(':') != colon)
            throw new IllegalArgumentException("A formátum: IP:port.");
        return new Parsed(value.substring(0, colon), Integer.parseInt(value.substring(colon + 1)));
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    private static boolean looksLikeIpLiteral(String host) {
        String value = stripBrackets(host);
        return value.matches("[0-9.]+") || value.matches("[0-9A-Fa-f:]+");
    }

    private static String stripBrackets(String host) {
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    private record Parsed(String host, int port) { }
}
