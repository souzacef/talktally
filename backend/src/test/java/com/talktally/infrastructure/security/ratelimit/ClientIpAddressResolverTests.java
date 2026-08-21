package com.talktally.infrastructure.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpAddressResolverTests {

	private final ClientIpAddressResolver resolver = new ClientIpAddressResolver();

	@Test
	void fallsBackToNormalizedRemoteAddressWithoutForwardedHeader() {
		MockHttpServletRequest request = request("192.168.001.010");

		assertEquals("192.168.1.10", resolver.resolve(request));
	}

	@Test
	void usesFirstRenderForwardedAddressAndNormalizesWhitespaceAndChains() {
		MockHttpServletRequest single = request("10.0.0.1");
		single.addHeader(ClientIpAddressResolver.FORWARDED_FOR_HEADER, "  203.0.113.7  ");
		MockHttpServletRequest chain = request("10.0.0.2");
		chain.addHeader(
				ClientIpAddressResolver.FORWARDED_FOR_HEADER,
				" 2001:DB8::1 , 10.0.0.3, 10.0.0.4");

		assertEquals("203.0.113.7", resolver.resolve(single));
		assertEquals(
				"2001:0db8:0000:0000:0000:0000:0000:0001",
				resolver.resolve(chain));
	}

	@Test
	void unusableOrOversizedForwardedMaterialFallsBackSafely() {
		MockHttpServletRequest malformed = request("198.51.100.9");
		malformed.addHeader(
				ClientIpAddressResolver.FORWARDED_FOR_HEADER,
				"not-an-ip, 203.0.113.8");
		MockHttpServletRequest missingFirst = request("198.51.100.10");
		missingFirst.addHeader(
				ClientIpAddressResolver.FORWARDED_FOR_HEADER,
				", 203.0.113.8");
		MockHttpServletRequest oversized = request("198.51.100.11");
		oversized.addHeader(
				ClientIpAddressResolver.FORWARDED_FOR_HEADER,
				"1".repeat(ClientIpAddressResolver.MAX_FORWARDED_HEADER_LENGTH + 1));

		assertEquals("198.51.100.9", resolver.resolve(malformed));
		assertEquals("198.51.100.10", resolver.resolve(missingFirst));
		assertEquals("198.51.100.11", resolver.resolve(oversized));
	}

	@Test
	void acceptsOnlyIpLiteralsAndNeverTreatsHostnamesAsAddresses() {
		assertTrue(ClientIpAddressResolver.normalizeLiteral("example.com").isEmpty());
		assertTrue(ClientIpAddressResolver.normalizeLiteral("256.1.1.1").isEmpty());
		assertTrue(ClientIpAddressResolver.normalizeLiteral("2001:db8:::1").isEmpty());
		assertEquals(
				"0000:0000:0000:0000:0000:ffff:c000:0201",
				ClientIpAddressResolver.normalizeLiteral("::ffff:192.0.2.1").orElseThrow());
	}

	private static MockHttpServletRequest request(String remoteAddress) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddress);
		return request;
	}
}
