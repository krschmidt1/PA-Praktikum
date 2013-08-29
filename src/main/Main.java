package main;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MainProgram mainProgram = new MainProgram();
		if(mainProgram.init())
			mainProgram.run();
		else
			System.out.println("Program can not be initialized!");
	}

}
