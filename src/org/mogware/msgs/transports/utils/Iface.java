package org.mogware.msgs.transports.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public final class Iface {
    public static InetAddress resolve(String addrStr, boolean ipv4only)
            throws ErrnoException {
        if (addrStr.equals("*"))
            addrStr = "0.0.0.0";
        InetAddress addr = Literal.resolve(addrStr, ipv4only);
        if (addr != null)
            return addr;
        InetAddress ipv4 = null;
        InetAddress ipv6 = null;
        try {
            Enumeration<NetworkInterface> nifs =
                    NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (! nif.getName().equals(addrStr))
                    continue;
                for (InterfaceAddress ifa: nif.getInterfaceAddresses()) {
                    InetAddress ia = ifa.getAddress();
                    if (ia instanceof Inet6Address)
                        ipv6 = ia;
                    else
                        ipv4 = ia;
                }
            }
        } catch (SocketException ex) {
            throw new ErrnoException(Global.ENODEV);
        }
        if (ipv6 != null && !ipv4only)
            return ipv6;
        if (ipv4 != null)
            return ipv4;
        return null;
    }
}