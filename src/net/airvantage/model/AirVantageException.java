package net.airvantage.model;

public class AirVantageException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private AvError error;

	public AirVantageException(net.airvantage.model.AvError error) {
		super();
		this.error = error;
	}
	
	public AvError getError() {
		return error;
	}
}
