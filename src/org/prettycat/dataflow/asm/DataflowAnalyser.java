package org.prettycat.dataflow.asm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DataflowAnalyser {
	
	final static String CLASS_FILE_DIR = "./bin";
	final static String CLASS_NAME = "org/prettycat/examples/test/TestClass";
	final static String EXTRACTION_ANNOTATION = "Lorg/senecade/asm/Extract;";
	final static boolean EXTRACT_ALL = false;
	
	private final static ArrayList<String> classesToHandle = new ArrayList<>();
	private final static HashSet<String> handledClasses = new HashSet<>();
	private final static ClassPath cp = new ClassPath();
	
	private static void enqueueMethod(final String method) {
		final String class_name = (method.substring(0, method.lastIndexOf("/")));
		enqueueClass(class_name);
	}
	
	private static void enqueueClass(final String class_name) {
		if (handledClasses.contains(class_name)) {
			return;
		}
		if (classesToHandle.contains(class_name)) {
			return;
		}
		classesToHandle.add(class_name);
	}
	
	private static void handleClass(String class_name, Document dest_doc, Element dest_el) {
		System.out.println("processing "+class_name);
		if (handledClasses.contains(class_name)) {
			return;
		}
		handledClasses.add(class_name);
		
		byte[] sourceClass = cp.readClass(class_name);
		if (sourceClass == null) {
			System.err.println("could not open class: "+class_name);
			return;
		}

		ClassNode sourceClassNode = new ClassNode(Opcodes.ASM5);
		ClassReader sourceClassReader = new ClassReader(sourceClass);

		sourceClassReader.accept(sourceClassNode, 0);
		
		for (MethodNode method: (List<MethodNode>)sourceClassNode.methods) {
			System.out.println(method.name + " " + method.desc);
			
			try {
				// methodFlow(sourceClassNode.name, method);
				MethodAnalysis analysis = new MethodAnalysis(sourceClassNode.name, method);
				for (String referenced: analysis.getReferencedMethods()) {
					enqueueMethod(referenced);
				}
				dest_el.appendChild(analysis.writeXML(dest_doc));
			} catch (AnalyzerException e) {
				System.out.println("analysis failed: "+e);
			}
		}
		
		
	}
	
	public static void run(Path output) {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			doc.setXmlStandalone(true);
			
			Element root = XMLProtocol.createASMElement(doc);
			doc.appendChild(root);
			
			while (!classesToHandle.isEmpty()) {
				String next_class = classesToHandle.get(0);
				classesToHandle.remove(0);
				
				handleClass(next_class, doc, root);
			}
			
			try (BufferedWriter backend_writer = Files.newBufferedWriter(output, Charset.forName("UTF-8"))) {
				TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(backend_writer));
			} catch (IOException e) {
				System.err.format("failed to write to %s: %s\n", output, e);
			}
		} catch (ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void printUsage() {
		System.out.println("... [-p CLASSPATH | --class-path CLASSPATH] [-o OUTFILE | --out-file OUTFILE] -- CLASS ...");
	}
	
	private static void printHelp() {
		printUsage();
		System.out.println();
		System.out.println("optional arguments:");
		System.out.println("   -h, --help                   yours truly.");
		System.out.println("   -p, --class-path CLASSPATH   add paths to the search path. If the paths ends in .jar, it is added as a jarfile.");
		System.out.println("   -o, --out-file OUTFILE       set the output file (defaults to ./out.xml)");
		System.out.println();
		System.out.println("positional arguments:");
		System.out.println("   CLASS  add a class to analyse");
	}
	
	private static void failArgument(String message) {
		printUsage();
		System.out.flush();
		System.err.flush();
		System.err.println(message);
		System.err.flush();
		System.exit(1);
	}
	
	private static boolean canUseNext(String[] args, int curr) {
		return (curr < args.length - 1);
	}
	
	private static void addToClassPath(String arg) throws IOException {
		String[] items = arg.split(":");
		for (String path: items) {
			if (path.endsWith(".jar")) {
				cp.addJarFile(Paths.get(path).toFile());
			} else {
				cp.addPath(Paths.get(path));
			}
		}
	}
	
	private static void addClassToHandle(String arg) {
		classesToHandle.add(arg.replace('.', '/'));
	}
	
	public static void main(String[] args) throws IOException {
		int i;
		Path outfile = Paths.get("./out.xml");
		for (i = 0; i < args.length; ++i) {
			String arg = args[i];
			if (arg.equals("-p") || arg.equals("--class-path")) {
				if (!canUseNext(args, i)) {
					failArgument("missing argument to "+arg);
				}
				i += 1;
				try {
					addToClassPath(args[i]);
				} catch (IOException e) {
					failArgument("failed to add class path: "+e);
				}
				continue;
			} else if (arg.equals("-o") || arg.equals("--out-file")) {
				if (!canUseNext(args, i)) {
					failArgument("missing argument to "+arg);
				}
				i += 1;
				outfile = Paths.get(args[i]);
				continue;
			} else if (arg.equals("-h") || arg.equals("--help")) {
				printHelp();
				System.exit(1);
			} else if (arg.equals("--")) {
				break;
			} else if (arg.startsWith("-")) {
				failArgument("unrecognized argument: "+arg);
			} else {
				addClassToHandle(arg);
			}
		}
		
		// pure positional arguments
		for (; i < args.length; ++i) {
			addClassToHandle(args[i]);
		}
		
		if (classesToHandle.isEmpty()) {
			failArgument("at least one CLASS must be given on command line.");
		}
		
		run(outfile);
	}

}
