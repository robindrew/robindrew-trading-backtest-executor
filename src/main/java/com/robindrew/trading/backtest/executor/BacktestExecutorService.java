package com.robindrew.trading.backtest.executor;

import com.robindrew.common.service.AbstractService;
import com.robindrew.common.service.component.heartbeat.HeartbeatComponent;
import com.robindrew.common.service.component.logging.LoggingComponent;
import com.robindrew.common.service.component.properties.PropertiesComponent;
import com.robindrew.common.service.component.stats.StatsComponent;

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

	public BacktestExecutorService(String[] args) {
		super(args);
	}

	@Override
	protected void startupService() throws Exception {
		start(properties);
		start(logging);
		start(heartbeat);
		start(stats);
	}

	@Override
	protected void shutdownService() throws Exception {
		stop(heartbeat);
	}
}
