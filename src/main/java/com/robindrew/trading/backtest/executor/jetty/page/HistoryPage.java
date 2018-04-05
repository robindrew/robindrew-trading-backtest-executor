package com.robindrew.trading.backtest.executor.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.PriceCandles;
import com.robindrew.trading.price.candle.charts.googlecharts.GoogleCandlestickChartData;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceManager;
import com.robindrew.trading.price.candle.io.stream.source.IPriceCandleStreamSource;
import com.robindrew.trading.provider.ITradeDataProvider;
import com.robindrew.trading.provider.TradeDataProvider;

public class HistoryPage extends AbstractServicePage {

	private static final Logger log = LoggerFactory.getLogger(HistoryPage.class);

	private static final int MAXIMUM_CHART_CANDLES = 120;

	public HistoryPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		String providerName = get(request, "provider", "");
		String instrumentName = get(request, "instrument", "");
		String dateTime = get(request, "date", "2016-01-01 00:00:00");
		String period = get(request, "period", "1 Hour");

		IPcfSourceManager manager = getDependency(IPcfSourceManager.class);

		// Provider
		Set<ITradeDataProvider> providers = manager.getProviders();
		ITradeDataProvider provider = providers.iterator().next();
		if (!providerName.isEmpty()) {
			provider = TradeDataProvider.valueOf(providerName);
		}
		log.info("[Provider] {}", provider);
		dataMap.put("provider", provider);
		dataMap.put("providers", providers);

		// Instrument
		Set<? extends IInstrument> instruments = manager.getInstruments(provider);
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
		List<IPriceCandle> candles = getPriceCandles(manager, provider, instrument, date, period);
		log.info("[Candles] {}", candles.size());
		
		String chartData = getChartData(candles);
		dataMap.put("chartData", chartData);
	}

	private String getChartData(List<IPriceCandle> candles) {
		if (candles.isEmpty()) {
			log.warn("No Candles Loaded");
			return null;
		}

		// Sanity check
		int count = candles.size();
		if (candles.size() > MAXIMUM_CHART_CANDLES) {
			candles = candles.subList(0, MAXIMUM_CHART_CANDLES);
			log.info("Chart Candles: {} (Reduced from {})", candles.size(), count);
		} else {
			log.info("Chart Candles: {}", candles.size());
		}

		GoogleCandlestickChartData data = new GoogleCandlestickChartData(candles, DateTimeFormatter.ISO_DATE);
		return data.toChartData();
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

	private List<IPriceCandle> getPriceCandles(IPcfSourceManager manager, ITradeDataProvider provider, IInstrument instrument, LocalDateTime date, String period) {

		// Dates
		LocalDateTime fromDate = date;
		LocalDateTime toDate = date.plusDays(7);

		long interval = 1;
		TimeUnit unit = TimeUnit.HOURS;
		
		// Get the source
		IPriceCandleStreamSource source = manager.getSourceSet(instrument, provider).asStreamSource(fromDate, toDate);
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
}
