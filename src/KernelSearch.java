import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import gurobi.GRBCallback;
import gurobi.GRBException;
import model.Instance;

public class KernelSearch
{
	private Instance instance;
	private String logPath;
	private Configuration config;
	private List<Item> items;
	private ItemSorter sorter;
	private BucketBuilder bucketBuilder;
	private KernelBuilder kernelBuilder;
	private int tlim;
	private Solution bestSolution;
	private List<Bucket> buckets;
	private Kernel kernel;
	private int tlimKernel;
	private int tlimBucket;
	private int numIterations;
	private GRBCallback callback;
	private Instant startTime;
	private boolean printDebugInfo;
	
	public KernelSearch(Instance instance, String logPath, Configuration config, boolean printDebugInfo)
	{
		this.logPath = logPath;
		this.config = config;
		this.instance = instance;
		this.printDebugInfo = printDebugInfo;
		bestSolution = new Solution();
		configure(config);
	}
	
	private void configure(Configuration configuration)
	{
		sorter = config.getItemSorter();
		tlim = config.getTimeLimit();
		bucketBuilder = config.getBucketBuilder();
		kernelBuilder = config.getKernelBuilder();
		tlimKernel = config.getTimeLimitKernel();
		numIterations = config.getNumIterations();
		tlimBucket = config.getTimeLimitBucket();
	}
	
	public Solution start() throws IOException, GRBException
	{
		startTime = Instant.now();
		callback = new CustomCallback(logPath, startTime);
		items = buildItems();
		sorter.sort(items);
		kernel = kernelBuilder.build(items, config);
		if(printDebugInfo) {
			System.out.println("items size:  "+items.size()+" - items in kernel: "+kernel.size());
		}
		solveKernel();
		iterateBuckets();
		
		return bestSolution;
	}

	private List<Item> buildItems() throws IOException, GRBException
	{
		if(printDebugInfo) {
			System.out.println("---------------------------------");
			System.out.println("starting lp relaxation - remaining time = "+getRemainingTime());
			System.out.println("---------------------------------");
		}
		Model model = new Model(instance, logPath, getRemainingTime(), config, true, printDebugInfo); // time limit equal to the global time limit
		model.buildModel();
		model.solve();
		List<Item> items = new ArrayList<>();
		List<String> varNames = model.getVarNames();
		for(String v : varNames)
		{
			// take just the decision variables
			if(v.contains("x")) {
				double value = model.getVarValue(v);
				double rc = model.getVarRC(v); // can be called only after solving the LP relaxation
				Item it = new Item(v, value, rc);
				items.add(it);
			}
		}
		if(printDebugInfo) {
			System.out.println("---------------------------------");
			System.out.println("lp relaxation finished");
			System.out.println("---------------------------------");
		}
		return items;
	}
	
	private void solveKernel() throws IOException, GRBException
	{
		if(printDebugInfo) {
			System.out.println("---------------------------------");
			System.out.println("starting solveKernel - remaining time = "+getRemainingTime());
			System.out.println("---------------------------------");
		}
		Model model = new Model(instance, logPath, Math.min(tlimKernel, getRemainingTime()), config, false, printDebugInfo);	
		
		model.buildModel();
		if(!bestSolution.isEmpty())
			model.readSolution(bestSolution);
		
		List<Item> toDisable = items.stream().filter(it -> !kernel.contains(it)).collect(Collectors.toList());
		model.disableItems(toDisable);
		model.setCallback(callback);
		model.solve();
		if(model.hasSolution() && (model.getSolution().getObj() < bestSolution.getObj() || bestSolution.isEmpty()))
		{
			bestSolution = model.getSolution();
			model.exportSolution();
		}
		if(printDebugInfo) {
			System.out.println("---------------------------------");
			System.out.println("solveKernel finished");
			System.out.println("---------------------------------");
		}
	}
	
	private void iterateBuckets() throws IOException
	{
		for (int i = 0; i < numIterations; i++)
		{
			// decrease bucket size
			config.setBucketSize(config.getBucketSize()/(i+1));
			List<Item> its = items.stream().filter(it -> !kernel.contains(it)).collect(Collectors.toList());
			buckets = bucketBuilder.build(its, config);
			int size = (int) Math.floor(its.size()*config.getBucketSize());
			if(printDebugInfo) {
				System.out.println("number of buckets: "+buckets.size());
				System.out.println("items in each bucket except the last one: "+size);
			}
			if(getRemainingTime() == 0) {
				System.out.println("Time over!");
				return;
			}
			solveBuckets();			
		}		
	}

	private void solveBuckets() throws IOException
	{
		if(printDebugInfo) {
			System.out.println("Starting solveBuckets - remaining time = " + getRemainingTime());
		}
		int i = -1;
		for(Bucket b : buckets)
		{
			i++;
			if(printDebugInfo) {
				System.out.println("Solving bucket "+i);
			}
			List<Item> toDisable = items.stream().filter(it -> !kernel.contains(it) && !b.contains(it)).collect(Collectors.toList());

			Model model = new Model(instance, logPath, Math.min(tlimBucket, getRemainingTime()), config, false, printDebugInfo);	
			model.buildModel();
					
			model.disableItems(toDisable);
			model.addBucketConstraint(b.getItems()); // can we use this constraint regardless of the type of variables chosen as items?
			
			if(!bestSolution.isEmpty())
			{
				model.addObjConstraint(bestSolution.getObj());		
				model.readSolution(bestSolution);
			}
			
			model.setCallback(callback);
			model.solve();
			
			if(model.hasSolution() && (model.getSolution().getObj() < bestSolution.getObj()  || bestSolution.isEmpty()))
			{
				bestSolution = model.getSolution();
				List<Item> selected = model.getSelectedItems(b.getItems());
				selected.forEach(it -> kernel.addItem(it));
				selected.forEach(it -> b.removeItem(it));
				model.exportSolution();
			}
			
			if(getRemainingTime() <= 0)
				return;
		}	
	}

	private int getRemainingTime()
	{
		return (int) (tlim - Duration.between(startTime, Instant.now()).getSeconds());
	}	
}