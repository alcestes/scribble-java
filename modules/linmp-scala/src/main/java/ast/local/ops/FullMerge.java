package ast.local.ops;

import java.util.Collection;

import ast.local.LocalBranch;
import ast.local.LocalCase;
import ast.local.LocalEnd;
import ast.local.LocalRec;
import ast.local.LocalSelect;
import ast.local.LocalType;
import ast.local.LocalTypeVisitor;
import ast.name.MessageLab;
import ast.name.RecVar;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.main.ScribbleException;

/** Merging operator that only merges branches where one contains
 *  all the labels of the other (and mergeable continuations).
 *
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
class FullMerge extends LocalTypeVisitor<LocalType>
{
	private Collection<String> errors = new java.util.LinkedList<String>();
	private LocalType visiting, t2; // We will update t2 during visit
	
	/** Merge the two given local types
	 * 
	 * @param t First type to merge
	 * @param u Second type to merge
	 * @return
	 */
	public static LocalType apply(LocalType t, LocalType u) throws ScribbleException
	{
		FullMerge m = new FullMerge(t, u);
		return m.process();
	}
	
	private FullMerge(LocalType t, LocalType u)
	{
		visiting = t;
		t2 = u;
	}

	@Override
	protected LocalType process() throws ScribbleException
	{
		LocalType res = visit(visiting);
		if (errors.isEmpty())
		{
			return res;
		}
		throw new ScribbleException("Error(s) merging " +visiting+ " and " +t2+ ": "
				                    + String.join(";", errors));
	}

	@Override
	protected LocalEnd visit(LocalEnd node)
	{
		if (!node.equals(t2))
		{
			cannotMerge(node, t2);
		}
		return node;
	}

	@Override
	protected LocalBranch visit(LocalBranch node)
	{
		if (!(t2 instanceof LocalBranch))
		{
			cannotMerge(node, t2);
			return node;
		}
		
		LocalBranch t2b = (LocalBranch)t2;
		
		if (!node.src.equals(t2b.src)) {
			cannotMerge(node, t2);
			return node;
		}
		
		Map<MessageLab,LocalCase> newcases = mergeCases(node.cases, t2b.cases,
														node, t2b);
		return new LocalBranch(node.src, newcases);
	}
	
	@Override
	protected LocalSelect visit(LocalSelect node)
	{
		if (!(t2 instanceof LocalSelect))
		{
			cannotMerge(node, t2);
			return node;
		}
		
		LocalSelect t2s = (LocalSelect)t2;
		
		if (!node.dest.equals(t2s.dest)) {
			cannotMerge(node, t2);
			return node;
		}
		
		Map<MessageLab,LocalCase> newcases = mergeCases(node.cases, t2s.cases,
														node, t2s);
		return new LocalSelect(node.dest, newcases);
	}
	
	private Map<MessageLab,LocalCase> mergeCases(Map<MessageLab,LocalCase> vcases, Map<MessageLab,LocalCase> t2cases,
												 LocalType v, LocalType t2)
	{
		Set<MessageLab> labsv = vcases.keySet();
		Set<MessageLab> labst2 = t2cases.keySet();
		
		Set<MessageLab> common = new HashSet<>(labsv);
		common.retainAll(labst2); // Labels common to visited and "other"

		Set<MessageLab> onlyv = new HashSet<>(labsv);
		onlyv.removeAll(labst2); // Labels only in the visited branching

		Set<MessageLab> onlyt2 = new HashSet<>(labst2);
		onlyt2.removeAll(labsv); // Labels only in the "other" branching

		Map<MessageLab,LocalCase> newcases = new HashMap<>();

		for (MessageLab l: common)
		{
			LocalCase vcl = vcases.get(l);
			if (!vcl.pay.equals(t2cases.get(l).pay))
			{
				// Payloads mismatch
				cannotMerge(v, t2);
				return newcases;
			}

			t2 = t2cases.get(l).body; // Update t2 before visiting
			newcases.put(l, new LocalCase(vcl.pay, visit(vcl.body)));
		}

		newcases.putAll(onlyv.stream()
				.collect(Collectors.toMap(l -> l, l -> vcases.get(l))));
		newcases.putAll(onlyt2.stream()
				.collect(Collectors.toMap(l -> l, l -> t2cases.get(l))));
		return newcases;
	}

	@Override
	protected LocalRec visit(LocalRec node)
	{
		if (!(t2 instanceof LocalRec))
		{
			cannotMerge(node, t2);
			return node;
		}
		
		LocalRec t2r = (LocalRec)t2;
		if (!node.recvar.equals(t2r.recvar))
		{
			cannotMerge(node, t2);
			return node;
		}
		
		t2 = t2r.body;
		return new LocalRec(node.recvar, visit(node.body));
	}

	@Override
	protected RecVar visit(RecVar node)
	{
		if (!node.equals(t2))
		{
			cannotMerge(node, t2);
		}
		return node;
	}
	
	private void cannotMerge(LocalType t1, LocalType t2)
	{
		errors.add("Cannot merge " + t1 + " and " + t2);
	}
}
