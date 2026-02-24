package com.ankur.webcurve.client;

public interface ExecutionListener {
	public void OnOrder(ClientOrder order, String info);
	public void OnExecution(Execution exec, String info);
}
