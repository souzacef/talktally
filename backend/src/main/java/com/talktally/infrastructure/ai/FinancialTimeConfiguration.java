package com.talktally.infrastructure.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class FinancialTimeConfiguration {

	@Bean
	ZoneId financialZoneId(
			@Value("${talktally.financial-time-zone:America/Sao_Paulo}") String zoneId) {
		return ZoneId.of(zoneId);
	}

	@Bean
	Clock financialClock(ZoneId financialZoneId) {
		return Clock.system(financialZoneId);
	}
}
