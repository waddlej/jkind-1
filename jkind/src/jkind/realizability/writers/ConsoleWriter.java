package jkind.realizability.writers;

import java.util.List;

import jkind.results.Counterexample;
import jkind.results.layout.Layout;
import jkind.util.Util;

public class ConsoleWriter extends Writer {
	private final Layout layout;

	public ConsoleWriter(Layout layout) {
		super();
		this.layout = layout;
	}

	@Override
	public void begin() {
	}

	@Override
	public void end() {
	}

	@Override
	public void writeBaseStep(int k) {
	}

	@Override
	public void writeRealizable(int k, double runtime) {
		writeLine();
		System.out.println("REALIZABLE || K = " + k + " || Time = " + Util.secondsToTime(runtime));
		writeLine();
		System.out.println();
	}

	@Override
	public void writeUnrealizable(int k, List<String> conflicts, double runtime) {
		writeLine();
		String details = conflicts.isEmpty() ? "" : ": " + conflicts;
		System.out.println("UNREALIZABLE" + details + " || K = " + k + " || Time = "
				+ Util.secondsToTime(runtime));
		writeLine();
		System.out.println();
	}

	@Override
	public  void writeUnrealizable(int k, List<String> conflicts, List<List<String>> diagnoses, double runtime) {
		writeLine();
		String details = conflicts.isEmpty() ? "" : ": " + conflicts;
		String diags = diagnoses.isEmpty() ? "" : ": " + diagnoses;
		System.out.println("UNREALIZABLE" + details + " || K = " + k + " || Time = "
				+ Util.secondsToTime(runtime));
		System.out.println("DIAGNOSES" + diags);
		writeLine();
		System.out.println();
	}


	@Override
	public void writeUnrealizable(Counterexample cex, List<String> conflicts, double runtime) {
		writeLine();
		String details = conflicts.isEmpty() ? "" : ": " + conflicts;
		System.out.println("UNREALIZABLE" + details + " || K = " + cex.getLength() + " || Time = "
				+ Util.secondsToTime(runtime));
		if (layout != null) {
			System.out.println(cex.toString(layout));
		}
		writeLine();
		System.out.println();
	}

	@Override
	public void writeUnrealizable(int k, List<Counterexample> counterexamples, List<String> conflicts,
								  List<List<String>> diagnoses, double runtime) {
		writeLine();
		String details = conflicts.isEmpty() ? "" : ": " + conflicts;
		String diags = diagnoses.isEmpty() ? "" : ": " + diagnoses;
		System.out.println("UNREALIZABLE" + details + " || K = " + k + " || Time = "
				+ Util.secondsToTime(runtime));
		System.out.println("DIAGNOSES" + diags);
		writeLine();
		for (Counterexample cex : counterexamples) {
			System.out.println("CEX for conflict : " + conflicts.get(counterexamples.indexOf(cex)));
			if (layout != null) {
				System.out.println(cex.toString(layout));
			}
			writeLine();
		}
		System.out.println();

	}

	@Override
	public void writeUnknown(int trueFor, Counterexample cex, double runtime) {
		writeLine();
		System.out.println("UNKNOWN || True for " + trueFor + " steps" + " || Time = "
				+ Util.secondsToTime(runtime));
		writeLine();
		System.out.println();
		if (cex != null && layout != null) {
			writeLine();
			System.out.println("EXTEND COUNTEREXAMPLE || K = " + cex.getLength());
			System.out.println(cex.toString(layout));
			writeLine();
			System.out.println();
		}
	}

	@Override
	public void writeInconsistent(int k, double runtime) {
		writeLine();
		System.out.println(
				"INCONSISTENT || System inconsistent at " + k + " steps" + " || Time = " + Util.secondsToTime(runtime));
		writeLine();
		System.out.println();
	}

	private void writeLine() {
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }
    
    @Override
    public void writeFixpointRealizable(int k, double runtime) {
        writeLine();
        System.out.println("REALIZABLE || REFINEMENTS = " + k + " || Time = " + Util.secondsToTime(runtime));
        writeLine();
        System.out.println();
    }

	public void writeFixpointRealizable(Counterexample cex, int k, double runtime) {
		writeLine();
		System.out.println("REALIZABLE || REFINEMENTS = " + k + " || Time = " + Util.secondsToTime(runtime));
		if (layout != null) {
			System.out.println(cex.toString(layout));
		}
		writeLine();
		System.out.println();
	}

    @Override
    public void writeFixpointUnrealizable(int k, List<String> conflicts, double runtime) {
        writeLine();
        String details = conflicts.isEmpty() ? "" : ": " + conflicts;
        System.out.println("UNREALIZABLE" + details + " || REFINEMENTS = " + k + " || Time = "
                + Util.secondsToTime(runtime));
        writeLine();
        System.out.println();
    }
}
