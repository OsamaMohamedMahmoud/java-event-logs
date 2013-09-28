package jvm.pm.tool.logs;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import noNamespace.AuditTrailEntryDocument.AuditTrailEntry;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry.EventType;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry.Timestamp;
import noNamespace.DataDocument.Data;
import noNamespace.DataDocument.Data.Attribute;
import noNamespace.Eventtypes.Enum;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Nodes.Node;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Nodes.Node.Properties.P;
import noNamespace.ProcessDocument.Process;
import noNamespace.ProcessInstanceDocument.ProcessInstance;
import noNamespace.WorkflowLogDocument;
import noNamespace.WorkflowLogDocument.WorkflowLog;

import org.apache.xmlbeans.XmlException;

public class MXMLLogBuilder {
	private WorkflowLogDocument workflowLogDoc;
	private SimpleDateFormat mxmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.s");

	public MXMLLogBuilder(boolean buildHeader) {
		workflowLogDoc = WorkflowLogDocument.Factory.newInstance();
		if (buildHeader) {
			WorkflowLog workflowLog = workflowLogDoc.addNewWorkflowLog();
			workflowLog.addNewSource().setProgram("Logs Source");
		}
	}

	public MXMLLogBuilder(File xmlFile) throws XmlException, IOException {
		workflowLogDoc = WorkflowLogDocument.Factory.parse(xmlFile);
	}

	public void save(File xmlFile) throws IOException {
		workflowLogDoc.save(xmlFile);
	}

	public WorkflowLogDocument getWorkflowLogDocument() {
		return workflowLogDoc;
	}

	public Process getNewProcess() {
		Process process = workflowLogDoc.getWorkflowLog().addNewProcess();
		process.setDescription("Process Desc");
		process.setId("DEFAULT");
		return process;
	}

	public AuditTrailEntry createAuditTrailEntry(String workflowModelElement, Calendar time, Enum eventType, String originator, Node node) {
		AuditTrailEntry auditTrailEntry = AuditTrailEntry.Factory.newInstance();
		auditTrailEntry.setWorkflowModelElement(workflowModelElement);
		if (time != null) {
			Timestamp timestamp = Timestamp.Factory.newInstance();
			timestamp.setStringValue(mxmlDateFormat.format(time.getTime()));
			auditTrailEntry.setTimestamp(timestamp);
		}
		if (eventType != null) {
			EventType e = EventType.Factory.newInstance();
			e.setStringValue(eventType.toString());
			auditTrailEntry.setEventType(e);
		} else {
			EventType e = EventType.Factory.newInstance();
			e.setStringValue(EventType.COMPLETE.toString());
			auditTrailEntry.setEventType(e);
		}
		if (originator != null) {
			auditTrailEntry.setOriginator(originator);
		}
		if (node != null) {
			Data data = auditTrailEntry.addNewData();
			P[] props = node.getProperties().getPArray();
			for (P p : props) {
				Attribute att = data.addNewAttribute();
				att.setName(p.getName());
				att.setStringValue(p.getStringValue());
			}
		}
		return auditTrailEntry;
	}

	public static void main(String[] args) throws XmlException, IOException {
		File xmlFile = new File("./files/AllAbstractActionsMXML.xml");

		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		Process process = builder.getNewProcess();

		for (int i = 1; i < 5; i++) {
			ProcessInstance instance = process.addNewProcessInstance();
			instance.setId("id " + i);
			int eventsCount = (2 + (int) (Math.random() * ((8 - 2) + 1)));
			for (int j = 0; j < eventsCount; j++) {
				AuditTrailEntry entry = builder.createAuditTrailEntry("event name " + j, Calendar.getInstance(), EventType.COMPLETE, "event resource " + j, null);
				instance.addNewAuditTrailEntry().set(entry);
			}
		}

		builder.save(xmlFile);
		System.out.println(builder.getWorkflowLogDocument());
	}

}
