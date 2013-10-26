package jvm.pm.tool.logs.igv;

import java.io.File;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;

import jvm.pm.tool.logs.LogMiner;
import jvm.pm.tool.logs.MXMLLogBuilder;
import noNamespace.AuditTrailEntryDocument.AuditTrailEntry;
import noNamespace.GraphDocumentDocument1;
import noNamespace.GraphDocumentDocument1.GraphDocument;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Edges.Edge;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Edges.RemoveEdge;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Nodes.Node;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Nodes.Node.Properties.P;
import noNamespace.GraphDocumentDocument1.GraphDocument.Group.Graph.Nodes.RemoveNode;
import noNamespace.ProcessDocument.Process;
import noNamespace.ProcessInstanceDocument.ProcessInstance;

public class IGVLogMiner extends LogMiner {

	private GraphDocument gDoc;
	private HashMap<String, String> txtNodes;
	private HashMap<String, Node> nodes;
	private HashMap<String, Edge> edges;
	private Long initTime;

	public IGVLogMiner(File inFile, File outFolder) throws Exception {
		super(inFile, outFolder);
		GraphDocumentDocument1 empDoc = GraphDocumentDocument1.Factory.parse(inFile);
		gDoc = empDoc.getGraphDocument();
		txtNodes = new HashMap<String, String>();
		nodes = new HashMap<String, Node>();
		edges = new HashMap<String, Edge>();
		initTime = System.currentTimeMillis();
	}

	private Calendar getTime(BigDecimal stamp) {
		int addedValue = stamp.multiply(new BigDecimal(1000)).toBigInteger().intValue();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(initTime);
		calendar.add(Calendar.MILLISECOND, addedValue);
		return calendar;
	}

//	private String getProbValue(P[] ps, String name) {
//		String value = null;
//		for (int i = 0; i < ps.length; i++) {
//			if (ps[i].getName().equalsIgnoreCase(name)) {
//				value = ps[i].getStringValue();
//			}
//		}
//		return value;
//	}

	private String getProbValue(noNamespace.GraphDocumentDocument1.GraphDocument.Group.Properties.P[] ps, String name) {
		String value = null;
		for (int i = 0; i < ps.length; i++) {
			if (ps[i].getName().equalsIgnoreCase(name)) {
				value = ps[i].getStringValue();
			}
		}
		return value;
	}

	private void addNode(Node node) {
		String txt = "";
		P[] props = node.getProperties().getPArray();
		String name = "", idx = "", id = node.getId();
		for (P p : props) {
			if (p.getName().equalsIgnoreCase("idx")) {
				idx = p.getStringValue();
			} else if (p.getName().equalsIgnoreCase("name")) {
				name = p.getStringValue();
			}
		}
		txt = idx.trim() + " " + name.trim();
		nodes.put(id, node);
		txtNodes.put(id, txt);
	}

	private void generateIGVMXMLGraphEvents() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		Group[] groups = gDoc.getGroupArray();
		Process process = builder.getNewProcess();
		for (Group group : groups) {
			ProcessInstance instance = process.addNewProcessInstance();
			String methodName = group.getMethod().getName();
			instance.setId(methodName);
			Graph[] graphs = group.getGraphArray();
			for (Graph graph : graphs) {
				String action = graph.getName();
				BigDecimal stamp = graph.getTstamp();
				AuditTrailEntry entry = builder.createAuditTrailEntry(action, getTime(stamp), null, methodName, null);
				instance.addNewAuditTrailEntry().set(entry);
			}
		}
		builder.save(new File(outFolder.getPath() + "/"+fileName+"_CompilationEvents_IGV.mxml"));
	}

	private void generateIGVMXMLMethods() throws Exception {
		MXMLLogBuilder builder = new MXMLLogBuilder(true);
		Group[] groups = gDoc.getGroupArray();
		for (Group group : groups) {
			if(!getProbValue(group.getProperties().getPArray(), "name").contains("PropertyMaker.getPropId"))
				continue;
			Process process = builder.getNewProcess();
			process.setId(getProbValue(group.getProperties().getPArray(), "name"));
			Graph[] graphs = group.getGraphArray();
			this.edges.clear();
			this.nodes.clear();
			this.txtNodes.clear();
			for (Graph graph : graphs) {
				RemoveNode[] rNodes = graph.getNodesArray()[0].getRemoveNodeArray();
				for (RemoveNode rNode : rNodes) {
					this.nodes.remove(rNode.getId());
				}
				Node[] nodes = graph.getNodesArray()[0].getNodeArray();
				for (Node node : nodes) {
					addNode(node);
				}
				RemoveEdge[] rEdges = graph.getEdgesArray()[0].getRemoveEdgeArray();
				for (RemoveEdge rEdge : rEdges) {
					this.edges.remove(rEdge.getFrom() + rEdge.getTo() + rEdge.getTo());
				}
				Edge[] edges = graph.getEdgesArray()[0].getEdgeArray();
				for (Edge edge : edges) {
					this.edges.put(edge.getFrom() + edge.getTo() + edge.getTo(), edge);
				}
				for (String id : this.edges.keySet()) {
					Edge edge = this.edges.get(id);
					AuditTrailEntry fentry = builder.createAuditTrailEntry(this.txtNodes.get(edge.getFrom()), null, null, null, this.nodes.get(edge.getFrom()));
					AuditTrailEntry tentry = builder.createAuditTrailEntry(this.txtNodes.get(edge.getTo()), null, null, null, this.nodes.get(edge.getTo()));
//					AuditTrailEntry fentry = builder.createAuditTrailEntry(this.txtNodes.get(edge.getFrom()), null, null, graph.getName(), this.nodes.get(edge.getFrom()));
//					AuditTrailEntry tentry = builder.createAuditTrailEntry(this.txtNodes.get(edge.getTo()), null, null, graph.getName(), this.nodes.get(edge.getTo()));
					ProcessInstance instance = process.addNewProcessInstance();
					instance.setId(graph.getName());
					instance.addNewAuditTrailEntry().set(fentry);
					instance.addNewAuditTrailEntry().set(tentry);
				}
			}
//			break;
		}
		builder.save(new File(outFolder.getPath() + "/"+fileName+"_Methods_IGV.mxml"));
	}

	@Override
	protected void startMining() throws Exception {
		System.out.println("IGV Miner: Start");
		System.out.println("IGV Miner: Mine Method Visule");
		generateIGVMXMLMethods();
		System.out.println("IGV Miner: Mine Graph Visule");
		generateIGVMXMLGraphEvents();
		System.out.println("IGV Miner: End");
	}

}
