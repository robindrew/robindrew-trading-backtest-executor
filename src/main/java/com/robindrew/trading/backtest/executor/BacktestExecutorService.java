package com.robindrew.trading.backtest.executor;

import com.robindrew.common.service.AbstractService;
import com.robindrew.common.service.component.heartbeat.HeartbeatComponent;
import com.robindrew.common.service.component.logging.LoggingComponent;
import com.robindrew.common.service.component.properties.PropertiesComponent;
import com.robindrew.common.service.component.stats.StatsComponent;
import com.robindrew.trading.backtest.executor.history.HistoryComponent;
import com.robindrew.trading.backtest.executor.jetty.JettyComponent;

public class BacktestExecutorService extends AbstractService {

	/**
	 * Entry point for the Backtest Executor Service.
	 */
	public static void main(String[] args) {
		BacktestExecutorService service = new BacktestExecutorService(args);
		service.startAsync();
	}

	private final HeartbeatComponent heartbeat = new HeartbeatComponent();
	private final PropertiesComponent properties = new PropertiesComponent();
	private final LoggingComponent logging = new LoggingComponent();
	private final StatsComponent stats = new StatsComponent();
	private final JettyComponent jetty = new JettyComponent();
	private final HistoryComponent history = new HistoryComponent();

	public BacktestExecutorService(String[] args) {
		super(args);
	}

	@Override
	protected void startupService() throws Exception {
		start(properties);
		start(logging);
		start(heartbeat);
		start(stats);
		start(history);
		start(jetty);
	}

	@Override
	protected void shutdownService() throws Exception {
		stop(jetty);
		stop(heartbeat);
	}
}
