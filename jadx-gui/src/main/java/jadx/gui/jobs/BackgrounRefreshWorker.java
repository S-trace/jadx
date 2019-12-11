package jadx.gui.jobs;

import java.util.concurrent.Future;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.UiUtils;

public class BackgrounRefreshWorker extends SwingWorker<Void, Void> {
	private static final Logger LOG = LoggerFactory.getLogger(BackgrounRefreshWorker.class);
	private final MainWindow mainWindow;
	private CacheObject cacheObject;

	public BackgrounRefreshWorker(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void exec() {
		execute();
	}

	@Override
	protected Void doInBackground() {
		cacheObject = mainWindow.getCacheObject();
		RefreshJob refreshJob = cacheObject.getRefreshJob();
		try {
			System.gc();
			LOG.info("Memory usage: Before refresh: {}", UiUtils.memoryInfo());
			long start = System.nanoTime();
			runJob(refreshJob);
			System.gc();
			LOG.info("Memory usage: After refresh: {}, refresh took {} ms", UiUtils.memoryInfo(), (System.nanoTime() - start) / 1000000);
		} catch (Exception e) {
			LOG.error("Exception in background worker", e);
		}
		cacheObject.setRefreshJob(null);
		mainWindow.resetIndex();
		return null;
	}

	private void runJob(BackgroundJob job) {
		if (isCancelled()) {
			return;
		}
		Future<Boolean> future = job.process();
		while (!future.isDone()) {
			try {
				if (isCancelled()) {
					future.cancel(false);
				}
				Thread.sleep(500);
			} catch (Exception e) {
				LOG.error("Background worker error", e);
			}
		}
	}
}
