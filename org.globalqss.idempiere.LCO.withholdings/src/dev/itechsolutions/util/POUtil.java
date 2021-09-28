package dev.itechsolutions.util;

import java.text.Collator;
import java.util.Comparator;

import org.compiere.model.PO;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class POUtil {
	public static Comparator<Object> getComparator() {
		return (o1, o2) -> {
			if (o1 == null)
				return -1;
			else if (o2 == null)
				return 1;
			if (!(o1 instanceof PO))
				throw new ClassCastException ("Not PO -1- " + o1);
			if (!(o2 instanceof PO))
				throw new ClassCastException ("Not PO -2- " + o2);
			//	same class
			Collator collator = Collator.getInstance();
			if (o1.getClass().equals(o2.getClass()))
			{
				PO po1 = (PO) o1;
				int index = po1.get_ColumnIndex("DocumentNo");
				if (index == -1)
					index = po1.get_ColumnIndex("Value");
				if (index == -1)
					index = po1.get_ColumnIndex("Name");
				if (index == -1)
					index = po1.get_ColumnIndex("Description");
				if (index != -1)
				{
					Object comp1 = po1.get_Value(index);
					PO po2 = (PO)o2;
					Object comp2 = po2.get_Value(index);
					if (comp1 == null)
						return -1;
					else if (comp2 == null)
						return 1;
					return collator.compare(comp1.toString(), comp2.toString());
				}
			}
			return collator.compare(o1.toString(), o2.toString());
		};
	}
}
