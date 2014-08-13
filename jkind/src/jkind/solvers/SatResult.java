package jkind.solvers;

public class SatResult extends Result {
	final private Model model;

	public SatResult(Model model) {
		super();
		this.model = model;
	}

	public Model getModel() {
		return model;
	}
}
