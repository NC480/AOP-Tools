package hu.pelyh.aoptools;

import io.netty.util.NetUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class StressServerPolicy {
    private StressServerPolicy() { }

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

    public static String endpoint(InetAddress address, int port) {
        String host = address.getHostAddress();
        return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
    }

    public static boolean authorizes(InetAddress address, int port, String authorizedEndpoint) {
        return address != null && isPrivate(address) && port >= 1 && port <= 65535
                && endpoint(address, port).equals(authorizedEndpoint);
    }

    private static boolean isPrivate(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }
}
