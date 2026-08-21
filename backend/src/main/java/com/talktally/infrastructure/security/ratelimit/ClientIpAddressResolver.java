package com.talktally.infrastructure.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Trusts the first forwarded address because the Render public application port
 * is reached only through Render's proxy/load balancer, which places the real
 * client address first. This deployment-specific signal is used only for abuse
 * limiting, never authorization.
 */
final class ClientIpAddressResolver {

	static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
	static final int MAX_FORWARDED_HEADER_LENGTH = 512;
	private static final String UNKNOWN_ADDRESS = "unknown";

	String resolve(HttpServletRequest request) {
		String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
		if (forwarded != null
				&& !forwarded.isBlank()
				&& forwarded.length() <= MAX_FORWARDED_HEADER_LENGTH) {
			int separator = forwarded.indexOf(',');
			String firstAddress = separator >= 0
					? forwarded.substring(0, separator)
					: forwarded;
			Optional<String> normalized = normalizeLiteral(firstAddress.strip());
			if (normalized.isPresent()) {
				return normalized.get();
			}
		}
		return normalizeLiteral(request.getRemoteAddr()).orElse(UNKNOWN_ADDRESS);
	}

	static Optional<String> normalizeLiteral(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return Optional.empty();
		}
		String value = candidate.strip();
		if (value.indexOf(':') >= 0) {
			return normalizeIpv6(value);
		}
		return normalizeIpv4(value);
	}

	private static Optional<String> normalizeIpv4(String value) {
		if (value.length() > 15) {
			return Optional.empty();
		}
		String[] octets = value.split("\\.", -1);
		if (octets.length != 4) {
			return Optional.empty();
		}
		int[] normalized = new int[4];
		for (int index = 0; index < octets.length; index++) {
			String octet = octets[index];
			if (octet.isEmpty()
					|| octet.length() > 3
					|| !octet.chars().allMatch(character -> character >= '0' && character <= '9')) {
				return Optional.empty();
			}
			int parsed;
			try {
				parsed = Integer.parseInt(octet);
			}
			catch (NumberFormatException exception) {
				return Optional.empty();
			}
			if (parsed > 255) {
				return Optional.empty();
			}
			normalized[index] = parsed;
		}
		return Optional.of(
				normalized[0] + "." + normalized[1] + "." + normalized[2] + "." + normalized[3]);
	}

	private static Optional<String> normalizeIpv6(String value) {
		if (value.length() > 45 || !value.matches("[0-9A-Fa-f:.]+")) {
			return Optional.empty();
		}
		int compression = value.indexOf("::");
		if (compression != value.lastIndexOf("::")) {
			return Optional.empty();
		}
		boolean compressed = compression >= 0;
		String left = compressed ? value.substring(0, compression) : value;
		String right = compressed ? value.substring(compression + 2) : "";
		List<Integer> groups = new ArrayList<>(8);
		if (!appendGroups(left, !compressed, groups)
				|| !appendGroups(right, true, groups)) {
			return Optional.empty();
		}
		if (compressed) {
			if (groups.size() >= 8) {
				return Optional.empty();
			}
			int leftGroupCount = countGroups(left);
			while (groups.size() < 8) {
				groups.add(leftGroupCount, 0);
			}
		}
		else if (groups.size() != 8) {
			return Optional.empty();
		}
		return Optional.of(groups.stream()
				.map(group -> String.format(Locale.ROOT, "%04x", group))
				.reduce((first, second) -> first + ":" + second)
				.orElseThrow());
	}

	private static boolean appendGroups(
			String part, boolean mayEndWithIpv4, List<Integer> groups) {
		if (part.isEmpty()) {
			return true;
		}
		String[] tokens = part.split(":", -1);
		for (int index = 0; index < tokens.length; index++) {
			String token = tokens[index];
			if (token.isEmpty()) {
				return false;
			}
			if (token.indexOf('.') >= 0) {
				if (!mayEndWithIpv4 || index != tokens.length - 1) {
					return false;
				}
				Optional<String> ipv4 = normalizeIpv4(token);
				if (ipv4.isEmpty()) {
					return false;
				}
				String[] octets = ipv4.get().split("\\.");
				groups.add(Integer.parseInt(octets[0]) * 256 + Integer.parseInt(octets[1]));
				groups.add(Integer.parseInt(octets[2]) * 256 + Integer.parseInt(octets[3]));
			}
			else {
				if (token.length() > 4) {
					return false;
				}
				groups.add(Integer.parseInt(token, 16));
			}
		}
		return true;
	}

	private static int countGroups(String part) {
		return part.isEmpty() ? 0 : part.split(":", -1).length;
	}
}
