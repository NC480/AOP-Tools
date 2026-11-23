package hu.pelyh.aoptools;

import java.net.InetAddress;
import java.util.List;

/** No sockets, DNS, game client or server are used by these checks. */
public final class StressServerPolicyCheck {
    public static void main(String[] args) throws Exception {
        var local = StressServerPolicy.parseLocalAddress("LOCALHOST");
        require(StressServerPolicy.authorizes(local, 25565, "127.0.0.1:25565"), "exact localhost endpoint");
        require(StressServerPolicy.authorizes(StressServerPolicy.parseLocalAddress("[::1]"), 25565,
                "[0:0:0:0:0:0:0:1]:25565"), "canonical IPv6 loopback endpoint");
        require(!StressServerPolicy.authorizes(local, 25564, "127.0.0.1:25565"), "wrong port");
        require(!StressServerPolicy.authorizes(local, 25565, "127.0.0.1:25564"), "wrong endpoint");
        require(!StressServerPolicy.authorizes(local, 0, "127.0.0.1:0"), "invalid port");
        var publicAddress = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        require(!StressServerPolicy.authorizes(publicAddress, 80, "8.8.8.8:80"), "public IP");
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
        require(StressBotNames.prepare(3, "AOPTest", List.of("Alice", "Bob"))
                        .equals(List.of("Alice", "Bob", "AOPTest001")),
                "custom names followed by generated fallback");
        require(StressBotNames.prepare(2, "AOPTest", List.of("One", "Two", "Three"))
                        .equals(List.of("One", "Two")),
                "bot count limits selected custom names");
        rejectNames(List.of("same", "SAME"));
        rejectNames(List.of("contains-dash"));
        System.out.println("Offline endpoint policy checks passed.");
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
