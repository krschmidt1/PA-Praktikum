package main;

public class MainProgram {
	private boolean running = true;
	
	public MainProgram() {
	}
	
	public void run() {
		while(running) {
			// TODO
			stop();
		}
	}
	
	public void stop() {
		running = false;
	}
}
