package org.prettycat.dataflow.asm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class ClassPath {
	private interface ClassSource {
		public InputStream openClass(String fqcn); 
	}
	
	private class FileSystemClassSource implements ClassSource {
		private final Path root;
		
		public FileSystemClassSource(Path root) throws IOException {
			this.root = root.toRealPath();
		}
		
		public InputStream openClass(String fqcn) {
			String[] parts = fqcn.split("\\.");
			System.err.println("looking for "+fqcn+" in "+root.toString());
			Path path = this.root;
			for (int i = 0; i < parts.length-1; i++) {
				path = path.resolve(parts[i]);
			}
			path = path.resolve(parts[parts.length-1] + ".class");
			System.err.println("final path: "+path);
			try {
				return new FileInputStream(path.toFile());
			} catch (FileNotFoundException exc) {
				System.err.println("failed to open: "+path);
				return null;
			}
		}
	}
	
	private class JarClassSource implements ClassSource {
		private final JarFile file;
		
		public JarClassSource(File jarfile) throws IOException {
			this.file = new JarFile(jarfile);
		}
		
		public InputStream openClass(String fqcn) {
			String[] parts = fqcn.split("\\.");
			String path = String.join("/", parts) + ".class"; 
			System.err.println("looking for "+path+" in jarfile "+file);
			try {
				return this.file.getInputStream(this.file.getEntry(path));
			} catch (IOException e) {
				System.err.println("failed to open "+path+" from jarfile");
				return null;
			}
		}
	}
	
	private final ArrayList<ClassSource> sources;
	
	public ClassPath() {
		sources = new ArrayList<ClassSource>();
	}
	
	public void addJarFile(File jarfile) throws IOException {
		sources.add(new JarClassSource(jarfile));
	}
	
	public void addPath(Path root) throws IOException {
		sources.add(new FileSystemClassSource(root));
	}
	
	public InputStream openClass(String fqcn) {
		for (ClassSource source: sources) {
			InputStream result = source.openClass(fqcn);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	public byte[] readClass(String fqcn) {
		InputStream inputStream = openClass(fqcn);
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		try {
			while ((length = inputStream.read(buffer)) != -1) {
			    result.write(buffer, 0, length);
			}
		} catch (IOException e) {
			return null;
		}
		return result.toByteArray();
	}
}
