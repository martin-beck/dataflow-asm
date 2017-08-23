package org.prettycat.dataflow.asm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLProtocol {
	public static final String NAMESPACE = "https://xmlns.zombofant.net/prettycat/1.0/asm";
	
	public static Element createASMElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "asm");
		return result;
	}
	
	public static Element createMethodElement(
			Document doc,
			String fqmn)
	{
		Element result = doc.createElementNS(NAMESPACE, "method");
		result.setAttribute("id", fqmn);
		return result;
	}
	
	public static Element createParametersElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "parameters");
		return result;
	}
	
	public static Element createParameterElement(
			Document doc,
			String uid,
			String fqtn)
	{
		Element result = doc.createElementNS(NAMESPACE, "parameter");
		result.setAttribute("id", uid);
		result.setAttribute("type", fqtn);
		return result;
	}
	
	public static Element createInstructionsElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "insns");
		return result;
	}

	public static Element createInstructionElement(
			Document doc,
			int opcode,
			int lineNumber,
			String uid)
	{
		Element result = doc.createElementNS(NAMESPACE, "insn");
		result.setAttribute("opcode", ""+opcode);
		result.setAttribute("line", ""+lineNumber);
		result.setAttribute("id", uid);
		return result;
	}
	
	public static Element createExitsElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "exits");
		return result;
	}

	public static Element createExitElement(
			Document doc,
			String uid,
			boolean exceptional)
	{
		Element result = doc.createElementNS(NAMESPACE, "exit");
		result.setAttribute("to", uid);
		result.setAttribute("exceptional", ""+exceptional);
		return result;
	}
	
	public static Element createInputsElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "inputs");
		return result;
	}

	public static Element createInputElement(
			Document doc)
	{
		Element result = doc.createElementNS(NAMESPACE, "input");
		return result;
	}
	
	public static Element createValueOfElement(Document doc, String uid) {
		Element result = doc.createElementNS(NAMESPACE, "value-of");
		result.setAttribute("from", uid);
		return result;
	}
	
	public static Element createUnknownElement(Document doc) {
		Element result = doc.createElementNS(NAMESPACE, "unknown");
		return result;
	}
	
	public static Element createMergeElement(Document doc) {
		Element result = doc.createElementNS(NAMESPACE, "merge");
		return result;
	}
}
