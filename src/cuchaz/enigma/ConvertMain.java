/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.JarFile;

import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.MappingsConverter;
import cuchaz.enigma.convert.MatchesReader;
import cuchaz.enigma.convert.MatchesWriter;
import cuchaz.enigma.convert.MemberMatches;
import cuchaz.enigma.gui.ClassMatchingGui;
import cuchaz.enigma.gui.MemberMatchingGui;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.ClassMapping;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.FieldMapping;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.Mappings;
import cuchaz.enigma.mapping.MappingsChecker;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodMapping;


public class ConvertMain {

	public static void main(String[] args)
	throws IOException, MappingParseException {
		try{
			//Get all are args
			String inVer = getArg(args, 1, "Old Version", true);
			String inSnapshot = getArg(args, 2, "Old Shapshot", true);
			String outVer = getArg(args, 3, "New Version", true);
			String outSnapshot = getArg(args, 4, "New Snapshot", true);
			String side = getArg(args, 5, "Side", true);
			if(inSnapshot.equalsIgnoreCase("none")){
				inSnapshot = null;
			}
			if(outSnapshot.equalsIgnoreCase("none")){
				outSnapshot = null;
			}
			//Get the home Directory
			File home = new File(System.getProperty("user.home"));
			//Find the minecraft jars.
			JarFile sourceJar = new JarFile(new File(home, getJarPath(inVer, inSnapshot)));
			JarFile destJar = new JarFile(new File(home, getJarPath(outVer, outSnapshot)));
			//Get the mapping files
			File inMappingsFile = new File("../Enigma Mappings/" + getVersionKey(inVer, inSnapshot, side) + ".mappings");
			File outMappingsFile = new File("../Enigma Mappings/" + getVersionKey(outVer, outSnapshot, side) + ".mappings");
			Mappings mappings = new MappingsReader().read(new FileReader(inMappingsFile));
			//Make the Match Files..
			File classMatchesFile = new File(inVer + "to" + outVer + "-" + side + ".class.matches");
			File fieldMatchesFile = new File(inVer + "to" + outVer + "-" + side + ".field.matches");
			File methodMatchesFile = new File(inVer + "to" + outVer + "-" + side + ".method.matches");

			String command = getArg(args, 0, "command", true);

			if(command.equalsIgnoreCase("computeClassMatches")){
				computeClassMatches(classMatchesFile, sourceJar, destJar, mappings);
			}else if(command.equalsIgnoreCase("editClassMatches")){
				editClasssMatches(classMatchesFile, sourceJar, destJar, mappings);
			}else if(command.equalsIgnoreCase("computeFieldMatches")){
				computeFieldMatches(fieldMatchesFile, destJar, outMappingsFile, classMatchesFile);
			}else if(command.equalsIgnoreCase("editFieldMatches")){
				editFieldMatches(sourceJar, destJar, outMappingsFile, mappings, classMatchesFile, fieldMatchesFile);
			}else if(command.equalsIgnoreCase("computeMethodMatches")){
				computeMethodMatches(methodMatchesFile, destJar, outMappingsFile, classMatchesFile);
			}else if(command.equalsIgnoreCase("editMethodMatches")){
				editMethodMatches(sourceJar, destJar, outMappingsFile, mappings, classMatchesFile, methodMatchesFile);
			}else if(command.equalsIgnoreCase("convertMappings")){
				convertMappings(outMappingsFile, sourceJar, destJar, mappings, classMatchesFile, fieldMatchesFile, methodMatchesFile);
			}
		}catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println(String.format("%s - %s", Constants.Name, Constants.Version));
		System.out.println("Usage:");
		System.out.println("\tjava -cp enigma.jar cuchaz.enigma.ConvertMain <command> <old-version> <old-snapshot> <new-version> <new-snapshot> <side>");
		System.out.println("\twhere <command> is one of:");
		System.out.println("\t\tcomputeClassMatches");
		System.out.println("\t\teditClassMatches");
		System.out.println("\t\tcomputeFieldMatches");
		System.out.println("\t\teditFieldMatches");
		System.out.println("\t\teditMethodMatches");
		System.out.println("\t\tconvertMappings");
		System.out.println("\twhere <old-version> is the version of Minecraft (1.8 for example)");
		System.out.println("\twhere <old-snapshpt> is the snapshot of Minecraft (14w32a for example) if it is not snapshot use 'none'");
		System.out.println("\twhere <new-version> is the version of Minecraft (1.9 for example)");
		System.out.println("\twhere <new-snapshpt> is the snapshot of Minecraft (15w32a for example) if it is not snapshot use 'none'");
		System.out.println("\twhere <side> is ether client or server");
	}

	//Copy of getArg from CommandMain.... Should make a utils class.
	private static String getArg(String[] args, int i, String name, boolean required) {
		if (i >= args.length) {
			if (required) {
				throw new IllegalArgumentException(name + " is required");
			} else {
				return null;
			}
		}
		return args[i];
	}


	private static String getJarPath(String version, String snapshot) {
		String os = System.getProperty("os.name");
		String osSpecific = "";
		if(os.toLowerCase().contains("win")){
			osSpecific = "AppData\\Roaming\\";
		}
		if (snapshot != null) {
			return osSpecific + ".minecraft/versions/" + snapshot + "/" + snapshot + ".jar";
		}
		return osSpecific + ".minecraft/versions/" + version + "/" + version + ".jar";
	}
	
	private static String getVersionKey(String version, String snapshot, String side) {
		if (snapshot == null) {
			return version + "-" + side;
		}
		return version + "." + snapshot + "-" + side;
	}
	
	private static void computeClassMatches(File classMatchesFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		ClassMatches classMatches = MappingsConverter.computeClassMatches(sourceJar, destJar, mappings);
		MatchesWriter.writeClasses(classMatches, classMatchesFile);
		System.out.println("Wrote:\n\t" + classMatchesFile.getAbsolutePath());
	}
	
	private static void editClasssMatches(final File classMatchesFile, JarFile sourceJar, JarFile destJar, Mappings mappings)
	throws IOException {
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		System.out.println("Starting GUI...");
		new ClassMatchingGui(classMatches, deobfuscators.source, deobfuscators.dest).setSaveListener(new ClassMatchingGui.SaveListener() {
			@Override
			public void save(ClassMatches matches) {
				try {
					MatchesWriter.writeClasses(matches, classMatchesFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	@SuppressWarnings("unused")
	private static void convertMappings(File outMappingsFile, JarFile sourceJar, JarFile destJar, Mappings mappings, File classMatchesFile)
	throws IOException {
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		
		Mappings newMappings = MappingsConverter.newMappings(classMatches, mappings, deobfuscators.source, deobfuscators.dest);
		
		try (FileWriter out = new FileWriter(outMappingsFile)) {
			new MappingsWriter().write(out, newMappings);
		}
		System.out.println("Write converted mappings to: " + outMappingsFile.getAbsolutePath());
	}
	
	private static void computeFieldMatches(File memberMatchesFile, JarFile destJar, File destMappingsFile, File classMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		System.out.println("Reading mappings...");
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		System.out.println("Indexing dest jar...");
		Deobfuscator destDeobfuscator = new Deobfuscator(destJar);
		
		System.out.println("Writing matches...");
		
		// get the matched and unmatched mappings
		MemberMatches<FieldEntry> fieldMatches = MappingsConverter.computeMemberMatches(
			destDeobfuscator,
			destMappings,
			classMatches,
			MappingsConverter.getFieldDoer()
		);
		
		MatchesWriter.writeMembers(fieldMatches, memberMatchesFile);
		System.out.println("Wrote:\n\t" + memberMatchesFile.getAbsolutePath());
	}
	
	private static void editFieldMatches(JarFile sourceJar, JarFile destJar, File destMappingsFile, Mappings sourceMappings, File classMatchesFile, final File fieldMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		MemberMatches<FieldEntry> fieldMatches = MatchesReader.readMembers(fieldMatchesFile);
		
		// prep deobfuscators
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(sourceMappings);
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		MappingsChecker checker = new MappingsChecker(deobfuscators.dest.getJarIndex());
		checker.dropBrokenMappings(destMappings);
		deobfuscators.dest.setMappings(destMappings);
		
		new MemberMatchingGui<FieldEntry>(classMatches, fieldMatches, deobfuscators.source, deobfuscators.dest).setSaveListener(new MemberMatchingGui.SaveListener<FieldEntry>() {
			@Override
			public void save(MemberMatches<FieldEntry> matches) {
				try {
					MatchesWriter.writeMembers(matches, fieldMatchesFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	@SuppressWarnings("unused")
	private static void convertMappings(File outMappingsFile, JarFile sourceJar, JarFile destJar, Mappings mappings, File classMatchesFile, File fieldMatchesFile)
	throws IOException {
		
		System.out.println("Reading matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		MemberMatches<FieldEntry> fieldMatches = MatchesReader.readMembers(fieldMatchesFile);

		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		
		// apply matches
		Mappings newMappings = MappingsConverter.newMappings(classMatches, mappings, deobfuscators.source, deobfuscators.dest);
		MappingsConverter.applyMemberMatches(newMappings, classMatches, fieldMatches, MappingsConverter.getFieldDoer());
		
		// write out the converted mappings
		try (FileWriter out = new FileWriter(outMappingsFile)) {
			new MappingsWriter().write(out, newMappings);
		}
		System.out.println("Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath());
	}


	private static void computeMethodMatches(File methodMatchesFile, JarFile destJar, File destMappingsFile, File classMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading class matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		System.out.println("Reading mappings...");
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		System.out.println("Indexing dest jar...");
		Deobfuscator destDeobfuscator = new Deobfuscator(destJar);
		
		System.out.println("Writing method matches...");
		
		// get the matched and unmatched mappings
		MemberMatches<BehaviorEntry> methodMatches = MappingsConverter.computeMemberMatches(
			destDeobfuscator,
			destMappings,
			classMatches,
			MappingsConverter.getMethodDoer()
		);
		
		MatchesWriter.writeMembers(methodMatches, methodMatchesFile);
		System.out.println("Wrote:\n\t" + methodMatchesFile.getAbsolutePath());
	}
	
	private static void editMethodMatches(JarFile sourceJar, JarFile destJar, File destMappingsFile, Mappings sourceMappings, File classMatchesFile, final File methodMatchesFile)
	throws IOException, MappingParseException {
		
		System.out.println("Reading matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		MemberMatches<BehaviorEntry> methodMatches = MatchesReader.readMembers(methodMatchesFile);
		
		// prep deobfuscators
		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(sourceMappings);
		Mappings destMappings = new MappingsReader().read(new FileReader(destMappingsFile));
		MappingsChecker checker = new MappingsChecker(deobfuscators.dest.getJarIndex());
		checker.dropBrokenMappings(destMappings);
		deobfuscators.dest.setMappings(destMappings);
		
		new MemberMatchingGui<BehaviorEntry>(classMatches, methodMatches, deobfuscators.source, deobfuscators.dest).setSaveListener(new MemberMatchingGui.SaveListener<BehaviorEntry>() {
			@Override
			public void save(MemberMatches<BehaviorEntry> matches) {
				try {
					MatchesWriter.writeMembers(matches, methodMatchesFile);
				} catch (IOException ex) {
					throw new Error(ex);
				}
			}
		});
	}
	
	private static void convertMappings(File outMappingsFile, JarFile sourceJar, JarFile destJar, Mappings mappings, File classMatchesFile, File fieldMatchesFile, File methodMatchesFile)
	throws IOException {
		
		System.out.println("Reading matches...");
		ClassMatches classMatches = MatchesReader.readClasses(classMatchesFile);
		MemberMatches<FieldEntry> fieldMatches = MatchesReader.readMembers(fieldMatchesFile);
		MemberMatches<BehaviorEntry> methodMatches = MatchesReader.readMembers(methodMatchesFile);

		Deobfuscators deobfuscators = new Deobfuscators(sourceJar, destJar);
		deobfuscators.source.setMappings(mappings);
		
		// apply matches
		Mappings newMappings = MappingsConverter.newMappings(classMatches, mappings, deobfuscators.source, deobfuscators.dest);
		MappingsConverter.applyMemberMatches(newMappings, classMatches, fieldMatches, MappingsConverter.getFieldDoer());
		MappingsConverter.applyMemberMatches(newMappings, classMatches, methodMatches, MappingsConverter.getMethodDoer());
		
		// check the final mappings
		MappingsChecker checker = new MappingsChecker(deobfuscators.dest.getJarIndex());
		checker.dropBrokenMappings(newMappings);
		
		for (java.util.Map.Entry<ClassEntry,ClassMapping> mapping : checker.getDroppedClassMappings().entrySet()) {
			System.out.println("WARNING: Broken class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ")");
		}
		for (java.util.Map.Entry<ClassEntry,ClassMapping> mapping : checker.getDroppedInnerClassMappings().entrySet()) {
			System.out.println("WARNING: Broken inner class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ")");
		}
		for (java.util.Map.Entry<FieldEntry,FieldMapping> mapping : checker.getDroppedFieldMappings().entrySet()) {
			System.out.println("WARNING: Broken field entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ")");
		}
		for (java.util.Map.Entry<BehaviorEntry,MethodMapping> mapping : checker.getDroppedMethodMappings().entrySet()) {
			System.out.println("WARNING: Broken behavior entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ")");
		}
		
		// write out the converted mappings
		try (FileWriter out = new FileWriter(outMappingsFile)) {
			new MappingsWriter().write(out, newMappings);
		}
		System.out.println("Wrote converted mappings to:\n\t" + outMappingsFile.getAbsolutePath());
	}

	private static class Deobfuscators {
		
		public Deobfuscator source;
		public Deobfuscator dest;
		
		public Deobfuscators(JarFile sourceJar, JarFile destJar) {
			System.out.println("Indexing source jar...");
			IndexerThread sourceIndexer = new IndexerThread(sourceJar);
			sourceIndexer.start();
			System.out.println("Indexing dest jar...");
			IndexerThread destIndexer = new IndexerThread(destJar);
			destIndexer.start();
			sourceIndexer.joinOrBail();
			destIndexer.joinOrBail();
			source = sourceIndexer.deobfuscator;
			dest = destIndexer.deobfuscator;
		}
	}
	
	private static class IndexerThread extends Thread {
		
		private JarFile m_jarFile;
		public Deobfuscator deobfuscator;
		
		public IndexerThread(JarFile jarFile) {
			m_jarFile = jarFile;
			deobfuscator = null;
		}
		
		public void joinOrBail() {
			try {
				join();
			} catch (InterruptedException ex) {
				throw new Error(ex);
			}
		}

		@Override
		public void run() {
			try {
				deobfuscator = new Deobfuscator(m_jarFile);
			} catch (IOException ex) {
				throw new Error(ex);
			}
		}
	}
}