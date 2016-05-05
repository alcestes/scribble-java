/**
 * 
 */
package ast.local;

import ast.name.MessageLab;
import ast.name.RecVar;

import java.util.Collection;
import java.util.Map;

import org.scribble.main.ScribbleException;

/** Perform sanity checks on a global type AST
 * @author ascalas
 *
 */
public class LocalTypeSanitizer extends LocalTypeVisitor<LocalType>
{
	private Collection<RecVar> bound = new java.util.HashSet<RecVar>();
	private Collection<String> errors = new java.util.LinkedList<String>();
	static private LocalType ltype;
	
	/** Sanitize the given global type
	 * 
	 * @param lt Local type to be sanitized
	 * @return A sanitized version of the given local type
	 * @throws ScribbleException
	 */
	public static LocalType apply(LocalType lt) throws ScribbleException
	{
		LocalTypeSanitizer s = new LocalTypeSanitizer(lt);
		return s.process();
	}
	
	private LocalTypeSanitizer(LocalType lt)
	{
		ltype = lt;
	}
	
	@Override
	protected LocalType process() throws ScribbleException
	{
		LocalType res = visit(ltype);
		if (errors.isEmpty())
		{
			return res;
		}
		throw new ScribbleException("Error(s) validating " + ltype + ": "
				                    + String.join(";", errors));
	}
	
	@Override
	protected LocalEnd visit(LocalEnd node)
	{
		return node;
	}
	
	@Override
	protected LocalBranch visit(LocalBranch node)
	{
		Map<MessageLab, LocalCase> cases2 = new java.util.HashMap<MessageLab, LocalCase>();
		for (Map.Entry<MessageLab, LocalCase> x: node.cases.entrySet())
		{
			cases2.put(x.getKey(), visit(x.getValue()));
		}
		
		return new LocalBranch(node.src, cases2);
	}
	
	protected LocalCase visit(LocalCase c)
	{
		ast.PayloadType pay = c.pay;
		if (c.pay instanceof LocalType)
		{
			try
			{
				pay = LocalTypeSanitizer.apply((LocalType)c.pay);
			}
			catch (ScribbleException e)
			{
				errors.add(e.toString());
			}
		}
		
		return new LocalCase(pay, visit(c.body));
	}
	
	@Override
	protected LocalSelect visit(LocalSelect node)
	{
		Map<MessageLab, LocalCase> cases2 = new java.util.HashMap<MessageLab, LocalCase>();
		for (Map.Entry<MessageLab, LocalCase> x: node.cases.entrySet())
		{
			cases2.put(x.getKey(), visit(x.getValue()));
		}
		
		return new LocalSelect(node.dest, cases2);
	}

	@Override
	protected LocalRec visit(LocalRec node)
	{
		// FIXME: here we are assuming that all recursion vars are distinct
		this.bound.add(node.recvar);
		LocalRec r = new LocalRec(node.recvar, visit(node.body));
		this.bound.remove(node.recvar);
		return r;
	}

	@Override
	protected RecVar visit(RecVar node) {
		if (!this.bound.contains(node))
		{
			errors.add("Unbound variable: " + node);
		}
		
		return node;
	}
}
