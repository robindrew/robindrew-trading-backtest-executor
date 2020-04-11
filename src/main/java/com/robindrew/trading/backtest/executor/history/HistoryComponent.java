package com.robindrew.trading.backtest.executor.history;

import static com.robindrew.common.dependency.DependencyFactory.setDependency;

import java.io.File;

import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.price.candle.format.pcf.source.IPcfSourceProviderLocator;
import com.robindrew.trading.price.candle.format.pcf.source.file.PcfFileProviderLocator;

public class HistoryComponent extends AbstractIdleComponent {

	private static final IProperty<String> propertyHistoricPricesDir = new StringProperty("historic.prices.directory");

	@Override
	protected void startupComponent() throws Exception {

		// Create the PCF source manager
		IPcfSourceProviderLocator fileManager = new PcfFileProviderLocator(new File(propertyHistoricPricesDir.get()));
		setDependency(IPcfSourceProviderLocator.class, fileManager);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// Nothing to do ...
	}

}
