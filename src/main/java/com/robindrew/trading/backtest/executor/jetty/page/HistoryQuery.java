package com.robindrew.trading.backtest.executor.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;
import static com.robindrew.trading.price.candle.interval.TimeUnitInterval.ONE_HOUR;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.PriceCandles;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceProviderManager;
import com.robindrew.trading.price.candle.interval.IPriceInterval;
import com.robindrew.trading.price.candle.io.stream.source.IPriceCandleStreamSource;
import com.robindrew.trading.provider.ITradingProvider;
import com.robindrew.trading.provider.TradingProvider;

public class HistoryQuery {

	private static final Logger log = LoggerFactory.getLogger(HistoryQuery.class);

	private final List<IPriceCandle> candles;

	public HistoryQuery(IHttpRequest request) {
		this(request, new HashMap<>());
	}

	public HistoryQuery(IHttpRequest request, Map<String, Object> dataMap) {

		String providerName = get(request, "provider", "");
		String instrumentName = get(request, "instrument", "");
		String dateTime = get(request, "date", "2016-01-01 00:00:00");
		String period = get(request, "period", "1 Hour");

		IPcfSourceProviderManager manager = getDependency(IPcfSourceProviderManager.class);

		// Provider
		ITradingProvider provider = manager.getProvider();
		if (!providerName.isEmpty()) {
			provider = TradingProvider.valueOf(providerName);
		}
		log.info("[Provider] {}", provider);
		dataMap.put("provider", provider);

		// Instrument
		Set<? extends IInstrument> instruments = manager.getInstruments();
		IInstrument instrument = instruments.iterator().next();
		if (!instrumentName.isEmpty()) {
			for (IInstrument entry : instruments) {
				if (entry.getName().equals(instrumentName)) {
					instrument = entry;
					request.setValue("instrument", instrumentName);
					break;
				}
			}
		}
		log.info("[Instrument] {}", instrument);
		dataMap.put("instrument", instrument);
		dataMap.put("instruments", instruments);

		// Date
		LocalDateTime date = parseDate(dateTime);
		dataMap.put("date", date.toString().replace('T', ' '));
		log.info("[Date] {}", date);

		// Period
		log.info("[Period] {}", period);
		dataMap.put("period", period);
		dataMap.put("periods", getPeriods());

		// Chart Data
		candles = getPriceCandles(manager, provider, instrument, date, period);
		log.info("[Candles] {}", candles.size());
	}

	private LocalDateTime parseDate(String dateTime) {
		LocalDate date;
		LocalTime time = LocalTime.of(0, 0, 0, 0);

		dateTime = dateTime.trim();
		int index = dateTime.indexOf('T');
		if (index == -1) {
			index = dateTime.indexOf(' ');
		}
		if (index == -1) {
			date = LocalDate.parse(dateTime);
		} else {
			date = LocalDate.parse(dateTime.substring(0, index).trim());
			time = LocalTime.parse(dateTime.substring(index + 1).trim());
		}
		return LocalDateTime.of(date, time);
	}

	private List<IPriceCandle> getPriceCandles(IPcfSourceProviderManager manager, ITradingProvider provider, IInstrument instrument, LocalDateTime date, String period) {

		// Dates
		LocalDateTime fromDate = date;
		LocalDateTime toDate = date.plusDays(7);

		long interval = 1;
		TimeUnit unit = TimeUnit.HOURS;

		// Get the source
		IPriceCandleStreamSource source = manager.getSourceSet(instrument).asStreamSource(fromDate, toDate);
		source = PriceCandles.aggregate(source, interval, unit);

		return PriceCandles.drainToList(source);
	}

	private String get(IHttpRequest request, String key, String defaultValue) {
		String value = request.get(key, "");
		if (value.isEmpty()) {
			value = request.getValue(key, defaultValue);
		} else {
			request.setValue(key, value);
		}
		return value;
	}

	public List<String> getPeriods() {
		List<String> list = new ArrayList<>();
		list.add("1 Hour");
		list.add("1 Day");
		list.add("1 Week");
		list.add("1 Month");
		list.add("1 Year");
		return list;
	}

	public List<IPriceCandle> getCandles() {
		return ImmutableList.copyOf(candles);
	}

	public IPriceInterval getInterval() {
		return ONE_HOUR;
	}
}
