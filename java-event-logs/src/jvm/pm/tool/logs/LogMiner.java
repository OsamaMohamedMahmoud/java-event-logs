package jvm.pm.tool.logs;

import java.io.File;

import jvm.pm.tool.logs.igv.IGVLogMiner;
import jvm.pm.tool.logs.jp2.JP2LogMiner;
import jvm.pm.tool.logs.logc.LogCompilationLogMiner;

public abstract class LogMiner {

	protected File inFile, outFolder;
	protected String fileName;

	public enum LogTypes {
		C1Visualizer, IGV, LogCompilation, JP2
	}

	protected LogMiner(File inFile, File outFolder) {
		this.inFile = inFile;
		this.outFolder = outFolder;
		fileName = inFile.getName().replaceFirst("[.][^.]+$", "");
	}

	public static LogMiner getInstance(LogTypes type, File inFile, File outFolder) {
		try {
			if (inFile != null && inFile.exists() && outFolder != null && outFolder.exists()) {
				switch (type) {
				case IGV:
					return new IGVLogMiner(inFile, outFolder);
				case LogCompilation:
					return new LogCompilationLogMiner(inFile, outFolder);
				case JP2:
					return new JP2LogMiner(inFile, outFolder);
				default:
					throw new RuntimeException("Unsupported Type");
				}
			} else {
				throw new RuntimeException("File/Folder Not Found");
			}
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public void mine() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("LogMiner: Start of Mining");
					startMining();
					System.out.println("LogMiner: End of Mining");
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
			}
		});
		thread.start();
	}

	protected abstract void startMining() throws Exception;
}
