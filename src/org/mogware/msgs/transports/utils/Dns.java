package org.mogware.msgs.transports.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class Dns {
    public static InetAddress resolve(String addrStr, boolean ipv4only) {
        InetAddress addr = Literal.resolve(addrStr, ipv4only);
        if (addr != null)
            return addr;
        try {
            for (InetAddress ia: InetAddress.getAllByName(addrStr)) {
                if (ipv4only && (ia instanceof Inet6Address))
                    continue;
                addr = ia;
                break;
            }
        } catch (UnknownHostException ex) {
            return null;
        }
        return addr;
    }

    public static boolean checkHostname(String name) {
        if (name.charAt(0) == '-')
            return false;
        int namelen = name.length();
        int labelsz = 0;
        for (int i = 0; true; i++, namelen--) {
            if (namelen == 0)
                return labelsz != 0;
            char ch = name.charAt(i);
            if (ch == '.') {
                if (labelsz == 0)
                    return false;
                labelsz = 0;
                continue;
            }
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                    (ch >= '0' && ch <= '9') || ch == '-') {
                labelsz++;
                if (labelsz > 63)
                    return false;
                continue;
            }
            return false;
        }
    }
}
