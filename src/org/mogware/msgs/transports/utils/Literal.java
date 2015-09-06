package org.mogware.msgs.transports.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public final class Literal {
    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV6_PART_COUNT = 8;

    public static InetAddress resolve(String ipStr, boolean ipv4only) {
        byte[] addr = null;
        if (!ipv4only)
            addr = stringToV6(ipStr);
        if (addr == null)
            addr = stringToV4(ipStr);
        if (addr == null)
            return null;
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static byte[] stringToV4(String ip) {
        if (ip.contains(":"))
            return null;
        String[] address = ip.split("\\.");
        if (address.length != IPV4_PART_COUNT)
            return null;
        byte[] bytes = new byte[IPV4_PART_COUNT];
        try {
            for (int i = 0; i < bytes.length; i++) {
                int piece = Integer.parseInt(address[i]);
                if (piece < 0 || piece > 255)
                    return null;
                if (address[i].startsWith("0") && address[i].length() != 1)
                    return null;
                bytes[i] = (byte) piece;
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return bytes;
    }

    private static byte[] stringToV6(String ipStr) {
        if (!ipStr.contains(":"))
            return null;
        if (ipStr.contains(":::"))
            return null;
        if (ipStr.contains("."))
            if ((ipStr = dottedQuadToHex(ipStr)) == null)
                return null;
        ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
        int partsHi = 0;
        int partsLo = 0;
        String[] addressHalves = ipStr.split("::", 2);
        if (!addressHalves[0].equals("")) {
            String[] parts = addressHalves[0].split(":", IPV6_PART_COUNT);
            try {
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals(""))
                        return null;
                    int piece = Integer.parseInt(parts[i], 16);
                    rawBytes.putShort(2 * i, (short) piece);
                }
                partsHi = parts.length;
            } catch (NumberFormatException ex) {
                return null;
            }
        } else
            partsHi = 1;
        if (addressHalves.length > 1) {
            if (!addressHalves[1].equals("")) {
                String[] parts = addressHalves[1].split(":", IPV6_PART_COUNT);
                try {
                    for (int i = 0; i < parts.length; i++) {
                        int partsIndex = parts.length - i - 1;
                        if (parts[partsIndex].equals(""))
                            return null;
                        int piece = Integer.parseInt(parts[partsIndex], 16);
                        int bytesIndex = 2 * (IPV6_PART_COUNT - i - 1);
                        rawBytes.putShort(bytesIndex, (short) piece);
                    }
                    partsLo = parts.length;
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else
                partsLo = 1;
        }
        int totalParts = partsHi + partsLo;
        if (totalParts > IPV6_PART_COUNT)
            return null;
        if (addressHalves.length == 1 && totalParts != IPV6_PART_COUNT)
            return null;
        return rawBytes.array();
    }

    private static String dottedQuadToHex(String ipStr) {
        int lastColon = ipStr.lastIndexOf(':');
        String initialPart = ipStr.substring(0, lastColon + 1);
        String dottedQuad = ipStr.substring(lastColon + 1);
        byte[] quad = stringToV4(dottedQuad);
        if (quad == null)
            return null;
        String penultimate = Integer.toHexString(
                ((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
        String ultimate = Integer.toHexString(
                ((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
        return initialPart + penultimate + ":" + ultimate;
    }
}
