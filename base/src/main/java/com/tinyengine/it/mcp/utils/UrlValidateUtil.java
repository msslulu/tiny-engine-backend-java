package com.tinyengine.it.mcp.utils;

import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.config.OpenAIConfig;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
@Component
public class UrlValidateUtil {
	private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
	private final OpenAIConfig config;

	public UrlValidateUtil(OpenAIConfig config) {
		this.config = config;
	}


	public void validateFinalUrl(String finalUrl) {
		if(finalUrl == null || finalUrl.isEmpty()) {
			throw new ServiceException("400", "baseUrl cannot be null or empty");
		}

		URI uri;
		try {
			uri = new URI(finalUrl);
		} catch (URISyntaxException e) {
			throw new ServiceException("400", "Invalid baseUrl format");
		}

		String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			throw new ServiceException("400", "Invalid baseUrl: missing host");
		}

		boolean isLoopback = LOOPBACK_HOSTS.contains(host.toLowerCase());

		List<String> allowedHosts = config.getAllowedHosts();

		if (allowedHosts == null || allowedHosts.isEmpty()) {
			if (!config.isAllowAnyHost()) {
				throw new ServiceException("500", "No AI allowed hosts configured");
			}

			enforceHttpsAndIpCheck(uri, host);
			return;
		}

		boolean matched = allowedHosts.stream()
			.anyMatch(allowed -> allowed.equalsIgnoreCase(host));
		if (!matched) {
			throw new ServiceException("400",
				"Host not allowed: " + host + ". Allowed hosts: " + allowedHosts);
		}

		if (isLoopback) {
			throw new ServiceException("400", "Loopback addresses are not allowed for custom baseUrl");
		}

		enforceHttpsAndIpCheck(uri, host);
	}

	void enforceHttpsAndIpCheck(URI uri, String host) {
		String scheme = uri.getScheme();
		if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
			throw new ServiceException("400", "Only HTTPS protocol is allowed for custom baseUrl");
		}

		try {
			InetAddress[] addresses = resolveHostAddresses(host);
			boolean hasBlockedAddress = Arrays.stream(addresses).anyMatch(this::isBlockedAddress);
			if (hasBlockedAddress) {
				throw new ServiceException("400", "Internal network addresses are not allowed");
			}
		} catch (UnknownHostException e) {
			throw new ServiceException("400", "Unable to resolve host: " + host);
		}
	}

	InetAddress[] resolveHostAddresses(String host) throws UnknownHostException {
		return InetAddress.getAllByName(host);
	}

	boolean isBlockedAddress(InetAddress address) {
		if (address.isLoopbackAddress()
			|| address.isSiteLocalAddress()
			|| address.isLinkLocalAddress()
			|| address.isAnyLocalAddress()
			|| address.isMulticastAddress()) {
			return true;
		}

		if (address instanceof Inet4Address) {
			return isBlockedIpv4((Inet4Address) address);
		}
		if (address instanceof Inet6Address) {
			return isBlockedIpv6((Inet6Address) address);
		}
		return false;
	}

	private boolean isBlockedIpv4(Inet4Address address) {
		byte[] octets = address.getAddress();
		int first = octets[0] & 0xFF;
		int second = octets[1] & 0xFF;
		int third = octets[2] & 0xFF;

		if (first == 0) {
			return true;
		}
		if (first == 100 && second >= 64 && second <= 127) {
			return true;
		}
		if (first == 192 && second == 0 && third == 0) {
			return true;
		}
		if (first == 192 && second == 0 && third == 2) {
			return true;
		}
		if (first == 198 && (second == 18 || second == 19)) {
			return true;
		}
		if (first == 198 && second == 51 && third == 100) {
			return true;
		}
		if (first == 203 && second == 0 && third == 113) {
			return true;
		}
		return first >= 240;
	}

	private boolean isBlockedIpv6(Inet6Address address) {
		byte[] octets = address.getAddress();
		int first = octets[0] & 0xFF;
		int second = octets[1] & 0xFF;

		if ((first & 0xFE) == 0xFC) {
			return true;
		}
		if (first == 0x20 && second == 0x01) {
			int third = octets[2] & 0xFF;
			int fourth = octets[3] & 0xFF;
			if (third == 0x0D && fourth == 0xB8) {
				return true;
			}
		}
		return first == 0xFF;
	}

}
