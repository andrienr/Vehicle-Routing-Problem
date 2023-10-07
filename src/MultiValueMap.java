import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiValueMap<K,V> 
	{
	    private final Map<K,Set<V>> mappings = new HashMap<K,Set<V>>();

	    public Set<V> getValues(K key)
	    {
	        return mappings.get(key);
	    }
	    
	    public int size() 
	    {
	    	return mappings.size();
	    }

	    public Boolean putValue(K key, V value)
	    {
	        Set<V> target = mappings.get(key);

	        if(target == null) 
	        {
	            target = new HashSet<V>();
	            mappings.put(key,target);
	        }

	        return target.add(value);
	    }

	}