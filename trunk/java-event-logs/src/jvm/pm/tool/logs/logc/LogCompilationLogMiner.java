package jvm.pm.tool.logs.logc;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

import jvm.pm.tool.logs.Entry;
import jvm.pm.tool.logs.LogMiner;
import jvm.pm.tool.logs.MXMLLogBuilder;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry.EventType;
import noNamespace.CompilationLogDocument.CompilationLog;
import noNamespace.HotspotLogDocument;
import noNamespace.HotspotLogDocument.HotspotLog;
import noNamespace.PhaseDocument.Phase;
import noNamespace.ProcessDocument.Process;
import noNamespace.ProcessInstanceDocument.ProcessInstance;
import noNamespace.TaskDocument.Task;

public class LogCompilationLogMiner extends LogMiner {

	private HotspotLog hotspotLog;
	private Long initTime;

	public LogCompilationLogMiner(File inFile, File outFolder) throws Exception {
		super(inFile, outFolder);
		HotspotLogDocument hotspotLogDocument = HotspotLogDocument.Factory.parse(inFile);
		hotspotLog = hotspotLogDocument.getHotspotLog();
		initTime = hotspotLog.getTimeMs();
	}

	private Calendar getTime(BigDecimal stamp) {
		int addedValue = stamp.multiply(new BigDecimal(1000)).toBigInteger().intValue();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(initTime);
		calendar.add(Calendar.MILLISECOND, addedValue);
		return calendar;
	}

	public void generateClassesRelations() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		TreeMap<Number, Entry> entries = new TreeMap<Number, Entry>();
		CompilationLog[] compileLogs = this.hotspotLog.getCompilationLogArray();
		for (int i = 0; i < compileLogs.length; i++) {
			CompilationLog compileLog = compileLogs[i];
			Task[] tasks = compileLog.getTaskArray();
			for (int j = 0; j < tasks.length; j++) {
				Task task = tasks[j];
				BigDecimal time = task.getStamp();
				int cid = task.getCompileId();
				String title = task.getMethod();
				String orig = task.getMethod().split(" ")[0];
				// Phase[] phases = task.getPhaseArray();
				// for (Phase phase : phases) {
				// if (phase.getName() == Phase.Name.PARSE) {
				// orig = phase.getKlassArray(0).getName();
				// }
				// }
				Entry entry = new Entry(title, null, orig, getTime(time), task);
				entries.put(cid, entry);
			}
		}
		Process process = builder.getNewProcess();
		process.setId(String.valueOf(hotspotLog.getProcess()));
		Object[] nums = entries.keySet().toArray();
		for (int i = 0; i < (nums.length - 1); i++) {
			Entry fsentry = entries.get((Number) nums[i]);
			Entry tsentry = entries.get((Number) nums[i + 1]);
			ProcessInstance instance = process.addNewProcessInstance();
			// use social miner
			AuditTrailEntry fentry = builder.createAuditTrailEntry(fsentry.getTitle(), fsentry.getTime(), null, fsentry.getOrig(), null);
			AuditTrailEntry tentry = builder.createAuditTrailEntry(tsentry.getTitle(), tsentry.getTime(), null, tsentry.getOrig(), null);
			// or : and use heuristic miner
			// AuditTrailEntry fentry =
			// builder.createAuditTrailEntry(fsentry.getOrig(),
			// fsentry.getTime(), null, null, null);
			// AuditTrailEntry tentry =
			// builder.createAuditTrailEntry(tsentry.getOrig(),
			// tsentry.getTime(), null, null, null);
			instance.setId(fsentry.getTitle() + " -> " + tsentry.getTitle());
			instance.addNewAuditTrailEntry().set(fentry);
			instance.addNewAuditTrailEntry().set(tentry);
		}
		builder.save(new File(outFolder.getPath() + "/LogComClassesRelations.mxml"));
	}

	public void generateTaskPhases() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		CompilationLog[] compileLogs = this.hotspotLog.getCompilationLogArray();
		Process process = builder.getNewProcess();
		process.setId(String.valueOf(hotspotLog.getProcess()));
		for (int i = 0; i < compileLogs.length; i++) {
			CompilationLog compileLog = compileLogs[i];
			Task[] tasks = compileLog.getTaskArray();
			for (int j = 0; j < tasks.length; j++) {
				Task task = tasks[j];
				String id = String.valueOf(task.getCompileId());
				String orig = task.getMethod();
				ProcessInstance instance = process.addNewProcessInstance();
				instance.setId(id);
				Phase[] phases = task.getPhaseArray();
				for (Phase phase : phases) {
					String title = phase.getName().toString();
					BigDecimal time = phase.getStamp();
					Entry entry = new Entry(title, null, orig, getTime(time), phase);
					AuditTrailEntry pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), null, entry.getOrig(), null);
					instance.addNewAuditTrailEntry().set(pentry);
				}
			}
		}
		builder.save(new File(outFolder.getPath() + "/LogComTaskPhases.mxml"));
	}

	public void generateOptimizerPhases() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		CompilationLog[] compileLogs = this.hotspotLog.getCompilationLogArray();
		Process process = builder.getNewProcess();
		process.setId(String.valueOf(hotspotLog.getProcess()));
		for (int i = 0; i < compileLogs.length; i++) {
			CompilationLog compileLog = compileLogs[i];
			Task[] tasks = compileLog.getTaskArray();
			for (int j = 0; j < tasks.length; j++) {
				Task task = tasks[j];
				String id = String.valueOf(task.getCompileId());
				String orig = task.getMethod();
				ProcessInstance instance = process.addNewProcessInstance();
				instance.setId(id);
				Phase[] phases = task.getPhaseArray();
				for (Phase phase : phases) {
					if (phase.getName() == Phase.Name.OPTIMIZER) {
						Phase[] opPhases = phase.getPhaseArray();
						ArrayList<String> old = new ArrayList<String>();
						for (Phase opPhase : opPhases) {
							String title = opPhase.getName().toString();
							while (old.contains(title)) {
								title += "I";
							}
							old.add(title);
							BigDecimal time = opPhase.getStamp();
							Entry entry = new Entry(title, null, orig, getTime(time), opPhase);
							AuditTrailEntry pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), null, entry.getOrig(), null);
							instance.addNewAuditTrailEntry().set(pentry);
						}
					}

				}
			}
		}
		builder.save(new File(outFolder.getPath() + "/LogComOptimizerPhases.mxml"));
	}

	public void generateTaskOptimizerPhases() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		CompilationLog[] compileLogs = this.hotspotLog.getCompilationLogArray();
		Process process = builder.getNewProcess();
		process.setId(String.valueOf(hotspotLog.getProcess()));
		for (int i = 0; i < compileLogs.length; i++) {
			CompilationLog compileLog = compileLogs[i];
			Task[] tasks = compileLog.getTaskArray();
			for (int j = 0; j < tasks.length; j++) {
				Task task = tasks[j];
				String id = String.valueOf(task.getCompileId());
				String orig = task.getMethod();
				ProcessInstance instance = process.addNewProcessInstance();
				instance.setId(id);
				Phase[] phases = task.getPhaseArray();
				for (Phase phase : phases) {
					if (phase.getName() == Phase.Name.OPTIMIZER) {
						String title = phase.getName().toString();
						BigDecimal time = phase.getStamp();
						Entry entry = new Entry(title, null, orig, getTime(time), phase);
						AuditTrailEntry pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), EventType.START, entry.getOrig(), null);
						instance.addNewAuditTrailEntry().set(pentry);

						Phase[] opPhases = phase.getPhaseArray();
						for (Phase opPhase : opPhases) {
							title = opPhase.getName().toString();
							time = opPhase.getStamp();
							entry = new Entry(title, null, orig, getTime(time), opPhase);
							pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), null, entry.getOrig(), null);
							instance.addNewAuditTrailEntry().set(pentry);
						}

						title = phase.getName().toString();
						time = phase.getStamp();
						entry = new Entry(title, null, orig, getTime(time), phase);
						pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), null, entry.getOrig(), null);
						instance.addNewAuditTrailEntry().set(pentry);
					} else {
						String title = phase.getName().toString();
						BigDecimal time = phase.getStamp();
						Entry entry = new Entry(title, null, orig, getTime(time), phase);
						AuditTrailEntry pentry = builder.createAuditTrailEntry(entry.getTitle(), entry.getTime(), null, entry.getOrig(), null);
						instance.addNewAuditTrailEntry().set(pentry);
					}

				}
			}
		}
		builder.save(new File(outFolder.getPath() + "/LogComTaskOptimizerPhases.mxml"));
	}

	@Override
	protected void startMining() throws Exception {
		System.out.println("Log Compilation Miner: Start");
		System.out.println("Log Compilation Miner: Mine Classes Relations");
		generateClassesRelations();
		System.out.println("Log Compilation Miner: Mine Task Phases");
		generateTaskPhases();
		System.out.println("Log Compilation Miner: Mine Optimizer Phases");
		generateOptimizerPhases();
		System.out.println("Log Compilation Miner: Mine Task Optimizer Phases");
		generateTaskOptimizerPhases();
		System.out.println("Log Compilation Miner: End");

	}

}
