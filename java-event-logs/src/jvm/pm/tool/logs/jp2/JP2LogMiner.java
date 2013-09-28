package jvm.pm.tool.logs.jp2;

import java.io.File;
import java.util.Calendar;

import jvm.pm.tool.logs.LogMiner;
import jvm.pm.tool.logs.MXMLLogBuilder;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry.EventType;
import noNamespace.Eventtypes.Enum;
import noNamespace.ProcessDocument.Process;
import noNamespace.ProcessInstanceDocument.ProcessInstance;
import ch.ethz.origo.jpProfiler.xmlns.callingContextTree.CallingContextTreeDocument;
import ch.ethz.origo.jpProfiler.xmlns.callingContextTree.CallingContextTreeDocument.CallingContextTree;
import ch.ethz.origo.jpProfiler.xmlns.callingContextTree.CallsiteDocument.Callsite;
import ch.ethz.origo.jpProfiler.xmlns.callingContextTree.MethodDocument.Method;

public class JP2LogMiner extends LogMiner {

	private CallingContextTree cct;
	private Long startTime;

	public JP2LogMiner(File inFile, File outFolder) throws Exception {
		super(inFile, outFolder);
		CallingContextTreeDocument cctDoc = CallingContextTreeDocument.Factory.parse(inFile);
		cct = cctDoc.getCallingContextTree();
		startTime = cct.getStartTime();
	}

	private Calendar getTime(long ntime) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(startTime);
		calendar.add(Calendar.MILLISECOND, (int) ntime);
		return calendar;
	}

	private void generateMethodOneProcess() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		Method[] methods = cct.getMethodArray();
		addMethods(builder, methods, true, null);
		builder.save(new File(outFolder.getPath() + "/JP2MethodOneProcess.mxml"));
	}

	private void addMethods(MXMLLogBuilder builder, Method[] methods, boolean newProcess, ProcessInstance instance) {
		for (int i = 0; i < methods.length; i++) {
			if (newProcess) {
				Process process = builder.getNewProcess();
				process.setId("Thread " + methods[i].getName());
				instance = process.addNewProcessInstance();
				instance.setId(methods[i].getName());
			}
			addMethod(methods[i], instance, builder);
		}
	}

	private void addMethod(Method method, ProcessInstance instance, MXMLLogBuilder builder) {
		String name = method.getName();
		String klass = method.getDeclaringClass();
		String params = method.getParams();
		String mreturn = method.getReturn();
		String action = mreturn + "-" + klass + "-" + name + "-" + params;
		long[] stimes = method.getSStamps().getSStampArray();
		long[] etimes = method.getEStamps().getEStampArray();
		AuditTrailEntry entry = null;
		Calendar stime = getTime(stimes[0]);
		Calendar etime = getTime(etimes[0]);
		Callsite[] calls = method.getCallsiteArray();
		Enum event = null;
		if (calls.length > 0) {
			event = EventType.START;
		}
		entry = builder.createAuditTrailEntry(action, stime, event, klass, null);
		instance.addNewAuditTrailEntry().set(entry);
		for (int i = 0; i < calls.length; i++) {
			Method[] inMethods = calls[i].getMethodArray();
			addMethods(builder, inMethods, false, instance);
		}
		if (calls.length > 0) {
			entry = builder.createAuditTrailEntry(action, etime, null, klass, null);
			instance.addNewAuditTrailEntry().set(entry);
		}
	}

	@Override
	protected void startMining() throws Exception {
		System.out.println("JP2 Miner: Start");
		System.out.println("JP2 Miner: Mine Methods One Process");
		generateMethodOneProcess();
		System.out.println("JP2 Miner: End");
	}

}
