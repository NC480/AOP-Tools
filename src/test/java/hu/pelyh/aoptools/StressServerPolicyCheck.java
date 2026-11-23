package hu.pelyh.aoptools;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** No sockets, DNS, game client or server are used by these checks. */
public final class StressServerPolicyCheck {
    public static void main(String[] args) throws Exception {
        var local = StressServerPolicy.parseLocalAddress("LOCALHOST");
        var configured = StressServerPolicy.canonicalEntries(List.of(
                "localhost:25565", "127.0.0.1:25565", "[::1]:25565", "192.168.1.2:1234",
                "example.invalid:25565", "8.8.8.8:25565", "127.0.0.1:+80", "[localhost]:80",
                "localhost:0", "localhost:65536", "127.0.0.1", "[::1]"));
        require(configured.size() == 3, "duplicates and invalid entries must be removed");
        require(StressServerPolicy.permits(local, 25565, configured), "exact localhost endpoint");
        require(StressServerPolicy.permits(StressServerPolicy.parseLocalAddress("[::1]"), 25565, configured),
                "canonical IPv6 loopback endpoint");
        require(!StressServerPolicy.permits(local, 25564, configured), "wrong port");
        require(!StressServerPolicy.permits(local, 25565, List.of()), "empty whitelist");
        require(!StressServerPolicy.permits(local, 0, List.of("127.0.0.1:0")), "invalid port");
        var publicAddress = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        require(!StressServerPolicy.permits(publicAddress, 80, List.of("8.8.8.8:80")),
                "public IP even when listed explicitly");
        for (String host : List.of("", "a", "cafe", "example.invalid", "127.1", "2130706433",
                "127.00.0.1", "127.0.0.256", "[127.0.0.1]", "8.8.8.8", "0.0.0.0", "::",
                "ff02::1", "2001:db8::1", "fe80::1%eth0", "localhost.invalid", "::1%1")) {
            try {
                StressServerPolicy.parseLocalAddress(host);
                throw new AssertionError("Accepted invalid or non-local host: " + host);
            } catch (IllegalArgumentException expected) {
                // Rejected before any resolver or connection can run.
            }
        }
        var oversized = new ArrayList<String>();
        for (int port = 1; port <= 65; port++) oversized.add("127.0.0.1:" + port);
        require(StressServerPolicy.canonicalEntries(oversized).size() == 64, "whitelist limit");
        require(StressBotNames.prepare(3, "AOPTest", List.of("Alice", "Bob"))
                        .equals(List.of("Alice", "Bob", "AOPTest001")),
                "custom names followed by generated fallback");
        require(StressBotNames.prepare(2, "AOPTest", List.of("One", "Two", "Three"))
                        .equals(List.of("One", "Two")),
                "bot count limits selected custom names");
        rejectNames(List.of("same", "SAME"));
        rejectNames(List.of("contains-dash"));
        System.out.println("Offline whitelist checks passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejectNames(List<String> names) {
        try {
            StressBotNames.prepare(Math.min(names.size(), 10), "AOPTest", names);
            throw new AssertionError("Accepted invalid names: " + names);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
