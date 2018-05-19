package com.robindrew.trading.backtest.executor.jetty.page;

import static com.robindrew.common.http.ContentType.IMAGE_PNG;

import java.util.List;

import com.google.common.io.ByteSource;
import com.robindrew.common.http.servlet.executor.IHttpExecutor;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.charts.PriceCandleCanvas;
import com.robindrew.trading.price.candle.interval.IPriceInterval;

public class ChartPage implements IHttpExecutor {

	@Override
	public void execute(IHttpRequest request, IHttpResponse response) {

		int width = request.getInteger("width");
		int height = request.getInteger("height");

		HistoryQuery query = new HistoryQuery(request);
		IPriceInterval interval = query.getInterval();
		List<IPriceCandle> candles = query.getCandles();

		PriceCandleCanvas canvas = new PriceCandleCanvas(width, height);
		canvas.renderCandles(candles, interval);

		byte[] png = canvas.toPng();
		response.ok(IMAGE_PNG, ByteSource.wrap(png));
	}

}
