package org.scribble2.model;

import java.util.List;


//public class InteractionSequence extends AbstractNode
public abstract class InteractionSequence<T extends InteractionNode> extends ModelNodeBase
{
	//public final List<? extends InteractionNode> actions;
	public final List<T> actions;

	//public InteractionSequence(CommonTree ct, List<? extends InteractionNode> ins)
	protected InteractionSequence(List<T> actions)
	{
		this.actions = actions;
	}
	
	/*protected abstract InteractionSequence<T> reconstruct(CommonTree ct, List<T> ins);
	
	@Override
	//public InteractionSequence visitChildren(NodeVisitor nv) throws ScribbleException
	public InteractionSequence<T> visitChildren(NodeVisitor nv) throws ScribbleException
	{
		List<T> actions = visitChildListWithClassCheck(this, this.actions, nv);  // OK to require all nodes to keep the same class? Maybe better to leave abstract and implement in the global/local subclasses
		//return new InteractionSequence<T>(this.ct, actions);
		return reconstruct(this.ct, actions);
	}*/
	
	public boolean isEmpty()
	{
		return this.actions.isEmpty();
	}
	
	@Override
	public String toString()
	{
		//List<? extends InteractionNode> ins = getSequence();
		if (this.actions.isEmpty())
		{
			return "";
		}
		String s = actions.get(0).toString();
		for (InteractionNode in : actions.subList(1, actions.size()))
		{
			s += "\n" + in;
		}
		return s;
	}
}
