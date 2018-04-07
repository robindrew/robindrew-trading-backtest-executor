package com.robindrew.trading.backtest.executor.jetty.page;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.charts.googlecharts.GoogleCandlestickChartData;

public class HistoryPage extends AbstractServicePage {

	private static final Logger log = LoggerFactory.getLogger(HistoryPage.class);

	private static final int MAXIMUM_CHART_CANDLES = 120;

	private boolean chartDataEnabled = false;

	public HistoryPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		HistoryQuery query = new HistoryQuery(request, dataMap);
		List<IPriceCandle> candles = query.getCandles();
		dataMap.put("candles", candles);

		// Google Chart Data
		if (chartDataEnabled) {
			dataMap.put("chartData", getChartData(candles));
		}
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

}
