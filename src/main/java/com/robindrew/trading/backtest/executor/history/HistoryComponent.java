package com.robindrew.trading.backtest.executor.history;

import static com.robindrew.common.dependency.DependencyFactory.setDependency;

import java.io.File;

import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceManager;
import com.robindrew.trading.price.candle.format.pcf.source.file.PcfFileManager;

public class HistoryComponent extends AbstractIdleComponent {

	private static final IProperty<String> propertyHistoricPricesDir = new StringProperty("historic.prices.directory");

	@Override
	protected void startupComponent() throws Exception {

		// Create the PCF source manager
		IPcfSourceManager fileManager = new PcfFileManager(new File(propertyHistoricPricesDir.get()));
		setDependency(IPcfSourceManager.class, fileManager);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// Nothing to do ...
	}

}
