package com.app.replant.global.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class UuidV5 {

    private static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    private UuidV5() {
    }

    public static UUID fromCategoryAndOrigin(String category, long originId) {
        String seed = category + "_" + originId;
        return fromNamespaceAndName(NAMESPACE_DNS, seed);
    }

    public static UUID fromNamespaceAndName(UUID namespace, String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(toBytes(namespace));
            sha1.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] hash = sha1.digest();

            // Set version to 5
            hash[6] &= 0x0f;
            hash[6] |= 0x50;
            // Set variant to IETF
            hash[8] &= 0x3f;
            hash[8] |= 0x80;

            ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 16);
            long msb = buffer.getLong();
            long lsb = buffer.getLong();
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
