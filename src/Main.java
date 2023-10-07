import java.io.IOException;
import gurobi.GRBException;
import model.Instance;
import model.Node;
import reader.InstanceReader;

public class Main {

	private static final String TIME_LIMIT = "-t";
	private static final String FILE_NAME = "-f";

	public static void main(String[] args) throws IOException, GRBException {
	    if (args.length < 1) {
	        System.out.println("Usage: java -jar -t timeLimit -f fileName");
	        System.exit(1);
        }
		
	    String timeLimit = "";
	    String fileName = "";

	    for(int i=0; i<args.length; i+=2)
	    {
	        String key = args[i];
	        String value = args[i+1];

	        switch (key)
	        {
	            case TIME_LIMIT : timeLimit = value; 
	            	break;
	            case FILE_NAME : fileName = value; 
	            	break;
	        }
	    }

	    boolean printDebugInfo = false;
		Instance instance = new Instance();
		InstanceReader instanceReader = new InstanceReader(instance, fileName, printDebugInfo);		
		String pathlog = "logs/";
		String pathConfig = "configs/config.txt";
		Configuration config = ConfigurationReader.read(timeLimit, pathConfig);		
		
		/**
		 * optional params:
		 * 
		 */
		 int maxActivities = 0;
		 int maxUnits = 0;
		 int maxVehicles = 0;

		 
		instanceReader.read(maxActivities,maxUnits,maxVehicles);
		if(printDebugInfo) {
			for(Node p : instance.getNodes()) {
				System.out.println(p.getNodeId()+"  "+p.getPosX()+"  "+p.getPosY()+"  "+p.getProblemType()+"  "+p.isConsecutive());
			}
		}

		instance.buildDistanceMatrix();
		KernelSearch ks = new KernelSearch(instance, pathlog, config, printDebugInfo);
		SolutionPrinter printer = new SolutionPrinter(ks.start(), printDebugInfo);
		printer.printSolution();
	}
	
	 public static double getMaxValue(double[][] ds) {
	        double maxValue = ds[0][0];
	        for (int j = 0; j < ds.length; j++) {
	            for (int i = 0; i < ds[j].length; i++) {
	                if (ds[j][i] > maxValue) {
	                    maxValue = ds[j][i];
	                }
	            }
	        }
	        return maxValue;
	    }
}