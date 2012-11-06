package jkind.processes;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindException;
import jkind.Settings;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.processes.messages.CounterexampleMessage;
import jkind.processes.messages.Message;
import jkind.processes.messages.ValidMessage;
import jkind.slicing.CounterexampleSlicer;
import jkind.solvers.Model;
import jkind.translation.Lustre2Sexps;
import jkind.translation.Util;
import jkind.writers.ConsoleWriter;
import jkind.writers.Writer;
import jkind.writers.XmlWriter;

public class Director {
	private String filename;
	private Node node;
	private List<String> remainingProperties;
	private List<String> validProperties;
	private List<String> invalidProperties;
	private Map<String, Type> typeMap;
	private CounterexampleSlicer cexSlicer;
	private Writer writer;
	
	private BaseProcess baseProcess;
	private InductiveProcess inductiveProcess;
	private InvariantProcess invariantProcess;
	
	private Thread baseThread;
	private Thread inductiveThread;
	private Thread invariantThread;
	
	protected BlockingQueue<Message> incoming;
	private long startTime;
	
	public Director(String filename, Node node) {
		this.filename = filename;
		this.node = node;
		this.cexSlicer = new CounterexampleSlicer(node);
		this.remainingProperties = new ArrayList<String>(node.properties);
		this.validProperties = new ArrayList<String>();
		this.invalidProperties = new ArrayList<String>();
		this.typeMap = Util.getTypeMap(node);
		this.writer = getWriter();
		this.incoming = new LinkedBlockingQueue<Message>();
	}

	private Writer getWriter() {
		if (Settings.xml) {
			try {
				return new XmlWriter(filename + ".xml", typeMap);
			} catch (FileNotFoundException e) {
				throw new JKindException("Unable to open XML output file", e);
			}
		} else {
			return new ConsoleWriter();
		}
	}

	public void run() {
		printHeader();
		writer.begin();
		startThreads();
		
		startTime = System.currentTimeMillis();
		long timeout = startTime + Settings.timeout * 1000;
		while (System.currentTimeMillis() < timeout && !remainingProperties.isEmpty()
				&& someCriticalThreadAlive() && !someThreadFailed()) {
			processMessages();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		processMessages();
		if (!remainingProperties.isEmpty()) {
			writer.writeUnknown(remainingProperties);
		}

		writer.end();
		printSummary();
		reportFailures();

		stopSolvers(); // Should do this after reportFailures as it may trigger spurious failures
	}

	private boolean someCriticalThreadAlive() {
		boolean result = baseThread.isAlive();
		if (inductiveThread != null) {
			result = result || inductiveThread.isAlive();
		}
		return result;
	}

	private boolean someThreadFailed() {
		boolean result = baseProcess.getThrowable() != null;
		if (inductiveThread != null) {
			result = result || inductiveProcess.getThrowable() != null;
		}
		if (invariantThread != null) {
			result = result || invariantProcess.getThrowable() != null;
		}
		return result;
	}

	private void reportFailures() {
		if (baseProcess.getThrowable() != null) {
			Throwable t = baseProcess.getThrowable();
			System.out.println("Base process failed");
			t.printStackTrace(System.out);
		}
		if (inductiveThread != null && inductiveProcess.getThrowable() != null) {
			Throwable t = inductiveProcess.getThrowable();
			System.out.println("Inductive process failed");
			t.printStackTrace(System.out);
		}
		if (invariantThread != null && invariantProcess.getThrowable() != null) {
			Throwable t = invariantProcess.getThrowable();
			System.out.println("Invariant process failed");
			t.printStackTrace(System.out);
		}
	}

	private void printHeader() {
		System.out.println("==========================================");
		System.out.println("  JAVA KIND");
		System.out.println("==========================================");
		System.out.println();
		System.out.println("There are " + remainingProperties.size() + " properties to be checked.");
		System.out.println("PROPERTIES TO BE CHECKED: " + remainingProperties);
		System.out.println();
	}

	private void startThreads() {
		Lustre2Sexps translation = new Lustre2Sexps(node);
		
		baseProcess = new BaseProcess(filename, node.properties, translation, this);
		baseThread = new Thread(baseProcess, "base");
		
		if (Settings.useInductiveProcess) {
			inductiveProcess = new InductiveProcess(filename, node.properties, translation,
					this);
			baseProcess.setInductiveProcess(inductiveProcess);
			inductiveProcess.setBaseProcess(baseProcess);
			inductiveThread = new Thread(inductiveProcess, "inductive");
		}
		
		if (Settings.useInvariantProcess) {
			invariantProcess = new InvariantProcess(filename, translation, typeMap);
			invariantProcess.setInductiveProcess(inductiveProcess);
			invariantThread = new Thread(invariantProcess, "invariant");
		}
		
		
		baseThread.start();
		if (Settings.useInductiveProcess) {
			inductiveThread.start();
		}
		if (Settings.useInvariantProcess) {
			invariantThread.start();
		}
	}

	private void stopSolvers() {
		baseProcess.stopSolver();
		if (inductiveProcess != null) {
			inductiveProcess.stopSolver();
		}
		if (invariantProcess != null) {
			invariantProcess.stopSolver();
		}
	}

	private void processMessages() {
		while (!incoming.isEmpty()) {
			Message message = incoming.poll();
			long elapsed = System.currentTimeMillis() - startTime;
			if (message instanceof ValidMessage) {
				ValidMessage vm = (ValidMessage) message;
				remainingProperties.removeAll(vm.valid);
				validProperties.addAll(vm.valid);
				writer.writeValid(vm.valid, vm.k, elapsed);
			} else if (message instanceof CounterexampleMessage) {
				CounterexampleMessage cex = (CounterexampleMessage) message;
				remainingProperties.removeAll(cex.invalid);
				invalidProperties.addAll(cex.invalid);
				for (String invalidProp : cex.invalid) {
					Model slicedModel = cexSlicer.slice(invalidProp, cex.model);
					writer.writeInvalid(invalidProp, cex.k, slicedModel, elapsed);
				}
			} else {
				throw new JKindException("Unknown message type in director: "
						+ message.getClass().getCanonicalName());
			}
		}
	}

	private void printSummary() {
		System.out.println("    -------------------------------------");
		System.out.println("    --^^--        SUMMARY          --^^--");
		System.out.println("    -------------------------------------");
		System.out.println();
		if (!validProperties.isEmpty()) {
			System.out.println("VALID PROPERTIES: " + validProperties);
			System.out.println();
		}
		if (!invalidProperties.isEmpty()) {
			System.out.println("INVALID PROPERTIES: " + invalidProperties);
			System.out.println();
		}
		if (!remainingProperties.isEmpty()) {
			System.out.println("UNKNOWN PROPERTIES: " + remainingProperties);
			System.out.println();
		}
	}
}
