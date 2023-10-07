import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import gurobi.GRBException;

public class SolutionPrinter {

	private Solution solution;
	private MultiValueMap<Integer, Object> routes=new MultiValueMap<Integer, Object>();
	private boolean printDebugInfo;

	public SolutionPrinter(Solution solution, boolean printDebugInfo) throws GRBException{
		this.solution=solution;
		this.printDebugInfo = printDebugInfo;
		prepareData();
	}

	@SuppressWarnings("unchecked")
	void printSolution() throws GRBException{
        System.out.println("Solution: "+solution.getObj());
        
        for(int k=0;k<routes.size();k++) {
    		if(printDebugInfo) {
    			System.out.println("vehicle "+k+" "+routes.getValues(k));
    		}
    		Iterator<Object> j = routes.getValues(k).iterator();
    		List<Integer> fromList = new ArrayList<Integer>();
    		List<Integer> toList = new ArrayList<Integer>();
    		while(j.hasNext())  
            {  
    	    	Map<Integer,Integer> map = (Map<Integer, Integer>) j.next();
    	    	fromList.addAll(map.keySet());
    	    	toList.addAll(map.values());
            }
    		List<Integer> fromListCopy = fromList.stream().collect(Collectors.toList());
    		int currentNode = toList.get(fromList.indexOf(0));
    		String route = "Route for vehicle "+k+" : 0 -> "+currentNode;
    		for(int i=0;i<fromList.size();i++) {
	    		if(fromList.indexOf(currentNode) > -1) {
	    			fromListCopy.remove(fromListCopy.indexOf(currentNode));
    				currentNode = toList.get(fromList.indexOf(currentNode));
    				double currentTime = solution.getVarValue("w^"+k+"_"+currentNode); 
    				DecimalFormat df = new DecimalFormat("###");
    				route+=" -> "+currentNode+" ("+df.format(currentTime)+")";
	    		}else {
	    			break;
	    		}
    		}
    		System.out.println(route);  
    		if (fromListCopy.size()>1) {
        		System.out.println("disjoint circuits exist!!");  
    		}
		}
        System.out.println();
	}
     
	private void prepareData() {
		for (String key: solution.getVars().keySet()) {
        	if(key.contains("x") && solution.getVarValue(key)>0) {
        		Map<Integer,Integer> map =new TreeMap<Integer,Integer>();
        		int vehicle = Integer.parseInt(key.substring(key.indexOf('^')+1,key.indexOf('_')));
        		int fromNode = Integer.parseInt(key.substring(key.indexOf('_')+1,key.indexOf('_',key.indexOf('_')+1)));
        		int toNode = Integer.parseInt(key.substring(key.indexOf('_',key.indexOf('_')+1)+1, key.length()));
//        		System.out.println(key+"  "+vehicle+"  "+fromNode+"  "+toNode);
        		map.put(fromNode, toNode);
        		routes.putValue(vehicle, map);
        	}
        }
	}
}