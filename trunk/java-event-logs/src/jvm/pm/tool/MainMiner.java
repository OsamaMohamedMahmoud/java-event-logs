package jvm.pm.tool;

import java.io.File;

import jvm.pm.tool.logs.LogMiner;
import jvm.pm.tool.logs.LogMiner.LogTypes;

public class MainMiner {

	public static void usage() {
		System.out.println("Usage: java-event-logs [ -logc ] [ -igv ] [ -jp2 ] inputFile outputFolder");
		System.out.println("  -logc	:  input file from jvm hotspot LogCompilation");
		System.out.println("  -igv	:  input file from jvm ideal graph visualizer");
		System.out.println("  -jp2	:  input file from jp2 tool");
		System.exit(0);
	}

	public static void main(String[] args) {
		if (args.length != 3) {
			usage();
		}
		String type = args[0];
		String inputFile = args[1];
		String outputFolder = args[2];
		LogTypes logType = null;
		if (type.equalsIgnoreCase("-logc")) {
			logType = LogTypes.LogCompilation;
		} else if (type.equalsIgnoreCase("-igv")) {
			logType = LogTypes.IGV;
		} else if (type.equalsIgnoreCase("-jp2")) {
			logType = LogTypes.JP2;
		} else {
			System.out.println("Invalid usage parameters");
			usage();
		}
		File inFile = new File(inputFile);
		if (!(inFile.exists() && inFile.isFile())) {
			System.out.println("Invalid input file");
			usage();
		}
		File outFolder = new File(outputFolder);
		if (!(outFolder.exists() && outFolder.isDirectory())) {
			System.out.println("Invalid output folder");
			usage();
		}
		LogMiner logMiner = LogMiner.getInstance(logType, inFile, outFolder);
		logMiner.mine();
	}
}
