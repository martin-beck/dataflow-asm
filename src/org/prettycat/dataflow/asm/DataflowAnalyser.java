package org.prettycat.dataflow.asm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DataflowAnalyser {
	
	final static String CLASS_FILE_DIR = "./bin";
	final static String CLASS_NAME = "org/prettycat/examples/test/TestClass";
	final static String EXTRACTION_ANNOTATION = "Lorg/senecade/asm/Extract;";
	final static boolean EXTRACT_ALL = false;
	
	public static void main(String[] args) throws IOException {
		ClassPath cp = new ClassPath();
		cp.addPath(Paths.get(CLASS_FILE_DIR));
		
		/*Path classFilePath = Paths.get(CLASS_FILE_DIR + "/" + CLASS_NAME + ".class");
		byte[] sourceClass;
		
		try {
			sourceClass = Files.readAllBytes(classFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}*/
		
		byte[] sourceClass = cp.readClass("org.prettycat.examples.test.TestClass");

		ClassNode sourceClassNode = new ClassNode(Opcodes.ASM5);
		ClassReader sourceClassReader = new ClassReader(sourceClass);

		sourceClassReader.accept(sourceClassNode, 0);


		Path path = Paths.get("./out.xml");
		
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			doc.setXmlStandalone(true);
			
			Element root = XMLProtocol.createASMElement(doc);
			doc.appendChild(root);
		
			for (MethodNode method: (List<MethodNode>)sourceClassNode.methods) {
				System.out.println(method.name + " " + method.desc);
				
				try {
					// methodFlow(sourceClassNode.name, method);
					MethodAnalysis analysis = new MethodAnalysis(sourceClassNode.name, method);
					root.appendChild(analysis.writeXML(doc));
				} catch (AnalyzerException e) {
					System.out.println("analysis failed: "+e);
				}
			}
			
			try (BufferedWriter backend_writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
				TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(backend_writer));
			} catch (IOException e) {
				System.err.format("failed to write to %s: %s\n", path, e);
			}
		} catch (ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
