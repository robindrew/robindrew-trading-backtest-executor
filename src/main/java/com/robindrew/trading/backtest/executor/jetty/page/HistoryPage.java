package com.robindrew.trading.backtest.executor.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.date.UnitTime;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.common.text.parser.UnitTimeParser;
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

	private static final int MAXIMUM_CHART_CANDLES = 100;

	public HistoryPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		String providerName = get(request, "provider", "");
		String instrumentName = get(request, "instrument", "");
		String period = get(request, "period", "1 Minute");
		String from = get(request, "from", "2016-01-01");
		int number = getInt(request, "number", 30);

		if (number > MAXIMUM_CHART_CANDLES) {
			number = MAXIMUM_CHART_CANDLES;
		}

		IPcfSourceManager manager = getDependency(IPcfSourceManager.class);

		// Providers
		Set<ITradeDataProvider> providers = manager.getProviders();
		ITradeDataProvider provider = null;
		if (!providerName.isEmpty()) {
			provider = TradeDataProvider.valueOf(providerName);
		}
		dataMap.put("provider", provider);
		dataMap.put("providers", providers);

		// Instruments
		Set<? extends IInstrument> instruments;
		if (provider == null) {
			instruments = manager.getInstruments();
		} else {
			instruments = manager.getInstruments(provider);
		}
		IInstrument instrument = null;
		for (IInstrument entry : instruments) {
			if (entry.getName().equals(instrumentName)) {
				instrument = entry;
				request.setValue("instrument", instrumentName);
				break;
			}
		}
		dataMap.put("instrument", instrument);
		dataMap.put("instruments", instruments);

		// Draw the chart!
		if (instrument != null && provider != null) {
			String chartData = getChartData(manager, provider, instrument, from, number, period);
			dataMap.put("chartData", chartData);
		}

		dataMap.put("from", from);
		dataMap.put("number", number);
		dataMap.put("period", period);
		dataMap.put("periods", getPeriods());
	}

	private String getChartData(IPcfSourceManager manager, ITradeDataProvider provider, IInstrument instrument, String from, int number, String period) {
		try {
			LocalDateTime fromDate = LocalDateTime.of(LocalDate.parse(from), LocalTime.of(0, 0));
			UnitTime time = new UnitTimeParser().parse(period);
			long offset = time.toMillis() * number;

			LocalDateTime toDate = fromDate.plus(offset, ChronoUnit.MILLIS);

			IPriceCandleStreamSource source = manager.getSourceSet(instrument, provider).asStreamSource(fromDate, toDate);

			source = PriceCandles.aggregate(source, time.getTime(), time.getUnit());

			log.info("Loading Candles for {} from {} for {} periods", instrument, from, number);
			List<IPriceCandle> candles = PriceCandles.drainToList(source);
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

		} catch (Exception e) {
			log.warn("Failed to get chart data", e);
			return null;
		}
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

	private int getInt(IHttpRequest request, String key, int defaultValue) {
		int value = request.getInt(key, 0);
		if (value == 0) {
			value = request.getValue(key, defaultValue);
		} else {
			request.setValue(key, value);
		}
		return value;
	}

	public List<String> getPeriods() {
		List<String> list = new ArrayList<>();
		list.add("1 Minute");
		list.add("5 Minutes");
		list.add("10 Minutes");
		list.add("30 Minutes");
		list.add("1 Hours");
		list.add("2 Hours");
		list.add("3 Hours");
		list.add("4 Hours");
		list.add("1 Day");
		list.add("7 Days");
		return list;
	}
}
