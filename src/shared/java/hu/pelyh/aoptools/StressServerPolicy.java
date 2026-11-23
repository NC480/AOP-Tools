package hu.pelyh.aoptools;

import io.netty.util.NetUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class StressServerPolicy {
    private StressServerPolicy() { }

    static String normalize(String host, int port) throws UnknownHostException {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("A port 1 és 65535 között lehet.");
        return endpoint(parseLocalAddress(host), port);
    }

    public static InetAddress parseLocalAddress(String host) throws UnknownHostException {
        String literal = host == null ? "" : host.trim();
        if (literal.equalsIgnoreCase("localhost")) literal = "127.0.0.1";
        if (literal.startsWith("[") && literal.endsWith("]") && literal.contains(":")) {
            literal = literal.substring(1, literal.length() - 1);
        }
        // Accept only dotted decimal IPv4 or unscoped IPv6. Never resolve a hostname.
        if (!(literal.matches("(?:0|[1-9][0-9]{0,2})(?:\\.(?:0|[1-9][0-9]{0,2})){3}")
                || (literal.contains(":") && literal.matches("[0-9A-Fa-f:]+")))) {
            throw new IllegalArgumentException("Csak localhost vagy közvetlen privát IP-cím használható.");
        }
        byte[] bytes = NetUtil.createByteArrayFromIpAddressString(literal);
        if (bytes == null) throw new IllegalArgumentException("Hibás IP-cím.");
        InetAddress address = InetAddress.getByAddress(bytes);
        if (!isPrivate(address)) throw new IllegalArgumentException("Csak loopback vagy privát LAN-cím használható.");
        return address;
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

    public static String endpoint(InetAddress address, int port) {
        String host = address.getHostAddress();
        return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
    }

    public static boolean permits(InetAddress address, int port, Collection<String> whitelist) {
        return address != null && isPrivate(address) && port >= 1 && port <= 65535
                && whitelist != null && whitelist.contains(endpoint(address, port));
    }

    private static Parsed parse(String entry) {
        String value = entry == null ? "" : entry.trim();
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close < 0 || close + 2 > value.length() || value.charAt(close + 1) != ':')
                throw new IllegalArgumentException("Hibás IPv6 whitelist-bejegyzés.");
            String host = value.substring(1, close);
            if (!host.contains(":")) throw new IllegalArgumentException("A zárójel csak IPv6-címhez használható.");
            return new Parsed(host, parsePort(value.substring(close + 2)));
        }
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || value.indexOf(':') != colon)
            throw new IllegalArgumentException("A formátum: IP:port.");
        return new Parsed(value.substring(0, colon), parsePort(value.substring(colon + 1)));
    }

    private static int parsePort(String value) {
        if (!value.matches("[0-9]{1,5}")) throw new IllegalArgumentException("Hibás port.");
        return Integer.parseInt(value);
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    private record Parsed(String host, int port) { }
}
