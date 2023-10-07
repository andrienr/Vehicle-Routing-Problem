import java.util.Collections;
import java.util.List;

public class ItemRandomSorter implements ItemSorter
{
	
	@Override
	public void sort(List<Item> items)
	{
        Collections.shuffle(items);
	}

}
