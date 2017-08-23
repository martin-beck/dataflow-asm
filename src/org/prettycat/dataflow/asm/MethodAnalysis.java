package org.prettycat.dataflow.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MethodAnalysis {
	private final String owner;
	private final MethodNode method;
	private final Analyzer analyzer;
	private final SimpleFlowInterpreter interpreter;
	
	private final HashMap<Integer, ArrayList<Edge> > edges;
	private final HashSet<Integer> exceptionTargets;
	private final ArrayList<SimpleFlowValue> arguments;
	
	private ArrayList<Integer> lineNumbers = null;
	private final HashSet<String> referencedMethods;

	public static class Edge {

		int target_node;
		boolean is_exception;
		
		Edge(int target_node, boolean is_exception) {
			this.target_node = target_node;
			this.is_exception = is_exception;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Edge) {
				Edge other = (Edge)obj;
				return other.target_node == target_node && other.is_exception == is_exception;
			}
			// TODO Auto-generated method stub
			return super.equals(obj);
		}
	}
	
	public MethodAnalysis(String owner, MethodNode method) throws AnalyzerException {
		this.owner = owner;
		this.method = method;
		this.interpreter = new SimpleFlowInterpreter();
		this.edges = new HashMap<Integer, ArrayList<Edge> >();
		this.exceptionTargets = new HashSet<Integer>();
		this.referencedMethods = new HashSet<String>();
		this.analyzer = new Analyzer(interpreter) {
			@Override
			protected boolean newControlFlowExceptionEdge(int insn, int successor) {
				if (!edges.containsKey(insn)) {
					ArrayList<Edge> exits = new ArrayList<>();
					edges.put(insn, exits);
				}  
				edges.get(insn).add(new Edge(successor, true));
				
				if (exceptionTargets.contains(successor)) {
					return false;
				} else {
					exceptionTargets.add(successor);
					return true;
				}
			}

			@Override
			protected void newControlFlowEdge(int insn, int successor) {
				// System.out.println(insn + " -> " + successor);
				
				if (!edges.containsKey(insn)) {
					ArrayList<Edge> exits = new ArrayList<>();
					edges.put(insn, exits);
				}
				edges.get(insn).add(new Edge(successor, false));
			}
		};
		this.arguments = new ArrayList<SimpleFlowValue>();
		
		runAnalysis();
	}
	
	private void runAnalysis() throws AnalyzerException {
		analyzer.analyze(owner, method);
		
		Frame initialFrame = analyzer.getFrames()[0];
		int nargs = Type.getArgumentTypes(method.desc).length;
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			nargs += 1;
		}
		for (int i = 0; i < nargs; ++i) {
			arguments.add((SimpleFlowValue) initialFrame.getLocal(i));
		}
		
		fillLineNumbers();
		extractReferencedMethods();
	}
	
	private void fillLineNumbers() {
		HashMap<LabelNode, Integer> labelLineNumbers = new HashMap<>();
		for (int i = 0; i < method.instructions.size(); i += 1) {
			AbstractInsnNode insn = method.instructions.get(i);
			if (insn instanceof LineNumberNode) {
				LineNumberNode linenoNode = (LineNumberNode)insn;
				labelLineNumbers.put(linenoNode.start, linenoNode.line);
			}
		}
		
		lineNumbers = new ArrayList<Integer>();
		int currentLineno = -1;
		for (int i = 0; i < method.instructions.size(); i += 1) {
			AbstractInsnNode insn = method.instructions.get(i);
			if (labelLineNumbers.containsKey(insn)) {
				currentLineno = labelLineNumbers.get(insn);
			}
			lineNumbers.add(currentLineno);
		}
	}
	
	private void extractReferencedMethods() {
		for (int i = 0; i < method.instructions.size(); i += 1) {
			AbstractInsnNode insn = method.instructions.get(i);
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode minsn = (MethodInsnNode)insn;
				referencedMethods.add(minsn.owner + "/" + minsn.name);
			}
		}
	}
	
	public String getFullyQualifiedMethodName() {
		return "java:"+owner.replace('/', '.')+"."+method.name+"["+method.desc+"]";
	}
	
	public String getFullyQualifiedInstructionName(int index) {
		return getFullyQualifiedMethodName() + "/" + "instructions" + "/" + index;
	}
	
	public String getFullyQualifiedParameterName(int index) {
		return getFullyQualifiedMethodName() + "/" + "parameters" + "/" + index;
	}
	
	private static String getFullyQualifiedTypeName(Type t) {
		return "java:"+t;
	}
	
	private Element writeInputXML(Document doc, SimpleFlowValue input) {
		if (input.origin != null && !input.isMerge) {
			return XMLProtocol.createValueOfElement(doc, getFullyQualifiedInstructionName(method.instructions.indexOf(input.origin)));
		} else {
			int paramIndex = arguments.indexOf(input);
			if (paramIndex >= 0) {
				return XMLProtocol.createValueOfElement(doc, getFullyQualifiedParameterName(paramIndex));
			} else if (input.isMerge) {
				Element merge = XMLProtocol.createMergeElement(doc);
				for (SimpleFlowValue subInput: input.inputs) {
					merge.appendChild(writeInputXML(doc, subInput));
				}
				return merge;
			} else {
				return XMLProtocol.createUnknownElement(doc);
			}
		}
	}
	
	private Element writeInputsXML(Document doc, SimpleFlowValue value) {
		Element result = XMLProtocol.createInputsElement(doc);
		for (SimpleFlowValue input: value.inputs) {
			result.appendChild(writeInputXML(doc, input));
		}
		return result;
	}
	
	private Element writeInstructionXML(
			Document doc, 
			int index,
			AbstractInsnNode instruction,
			int lineNumber) 
	{
		Element result = XMLProtocol.createInstructionElement(
				doc, 
				instruction.getOpcode(), 
				lineNumber,
				getFullyQualifiedInstructionName(index));
		SimpleFlowValue value = interpreter.getValue(instruction);
		if (value != null && value.inputs.size() > 0) {
			result.appendChild(writeInputsXML(doc, value));
		}
		ArrayList<Edge> exits = edges.get(index);
		if (exits != null) {
			Element exitsNode = XMLProtocol.createExitsElement(doc);
			result.appendChild(exitsNode);
			for (Edge exit: exits) {
				exitsNode.appendChild(
					XMLProtocol.createExitElement(doc, getFullyQualifiedInstructionName(exit.target_node), exit.is_exception)
				);
			}
		}
		return result;
	}
	
	private Element writeInstructionsXML(Document doc) {
		Element result = XMLProtocol.createInstructionsElement(doc);
		
		int i = 0;
		int lineno = 0;
		for (Frame frame: analyzer.getFrames()) {
			final AbstractInsnNode instruction = method.instructions.get(i);
			System.out.println(instruction);
			result.appendChild(writeInstructionXML(doc, i, instruction, lineNumbers.get(i)));
			
			i += 1;
		}	
		
		return result;
	}
	
	public Element writeParametersXML(Document doc) {
		Element result = XMLProtocol.createParametersElement(doc);
		int i = 0;
		for (SimpleFlowValue argument: arguments) {
			result.appendChild(XMLProtocol.createParameterElement(doc, getFullyQualifiedParameterName(i), getFullyQualifiedTypeName(argument.type)));
			i += 1;
		}
		return result;
	}
	
	public Element writeXML(Document doc) {
		// dump();
		
		Element methodElement = XMLProtocol.createMethodElement(doc, getFullyQualifiedMethodName());
		
		methodElement.appendChild(writeParametersXML(doc));
		methodElement.appendChild(writeInstructionsXML(doc));
		
		return methodElement;
	}
	
	public Analyzer getAnalyzer() {
		return analyzer;
	}
	
	public SimpleFlowInterpreter getInterpreter() {
		return interpreter;
	}
	
	public HashMap<Integer, ArrayList<Edge> > getEdges() {
		return edges;
	}
	
	public ArrayList<SimpleFlowValue> getArguments() {
		return arguments;
	}
	
	private void gatherNestedInputs(StringBuilder into, SimpleFlowValue rootInput) {
		if (rootInput.origin != null) {
			into.append(rootInput.origin);
			return;
		}
		if (rootInput.inputs.size() == 1) {
			gatherNestedInputs(into, rootInput.inputs.get(0));
			return;
		}
		into.append("[");
		boolean first = true;
		for (SimpleFlowValue input: rootInput.inputs) {
			if (!first) {
				into.append(", ");
			}
			first = false;
			gatherNestedInputs(into, input);
		}
		into.append("]");
	}
	
	public void dump() {
		int i = 0;
		for (Frame frame: analyzer.getFrames()) {
			final AbstractInsnNode instruction = method.instructions.get(i);
			System.out.println("instruction "+instruction+"; frame: "+i+" "+frame);
			SimpleFlowValue value = interpreter.getValue(instruction);
			if (value != null) {
				for (SimpleFlowValue input: value.inputs) {
					StringBuilder b = new StringBuilder();
					b.append("  <- ");
					b.append(input);
					if (input.origin != null) {
						b.append(" from ");
						b.append(input.origin);
					} else {
						int argIndex = arguments.indexOf(input);
						if (argIndex >= 0) {
							b.append(" (argument ");
							b.append(argIndex);
							b.append(")");
						} else {
							b.append(" ");	
							gatherNestedInputs(b, input);
						}
					}
					System.out.println(b);
				}
			}
			ArrayList<MethodAnalysis.Edge> exits = edges.get(new Integer(i));
			if (exits != null) {
				for (MethodAnalysis.Edge edge: exits) {
					System.out.println("  -> "+edge.target_node+" "+edge.is_exception);
				}
			}	
			i = i + 1;
		}
	}
}
