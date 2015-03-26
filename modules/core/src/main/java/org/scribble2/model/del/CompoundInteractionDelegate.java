package org.scribble2.model.del;

import org.scribble2.model.ModelNode;
import org.scribble2.model.visit.WellFormedChoiceChecker;
import org.scribble2.model.visit.env.WellFormedChoiceEnv;
import org.scribble2.util.ScribbleException;

// For CompoundInteractionNode and ProtocolBlock
public class CompoundInteractionDelegate extends ModelDelegateBase
{
	//public CompoundInteractionDelegate(Env env)
	public CompoundInteractionDelegate()
	{
		//super(env);
	}

	@Override
	public WellFormedChoiceChecker enterWFChoiceCheck(ModelNode parent, ModelNode child, WellFormedChoiceChecker checker) throws ScribbleException
	{
		//WellFormedChoiceEnv env = new WellFormedChoiceEnv(checker.peekEnv());
		WellFormedChoiceEnv env = checker.peekEnv().push();
		/*env.initial.clear();
		env.initialInterrupts.clear();*/
		//env = env.enableChoiceSubject(((GlobalChoice) child).subj.toName());
		//checker.setEnv(env);
		checker.pushEnv(env);
		return checker;
	}
	//public void enter(Choice<? extends ProtocolBlock<? extends InteractionSequence<? extends InteractionNode>>> cho, WellFormedChoiceChecker checker)
	
	@Override
	public ModelNode leaveWFChoiceCheck(ModelNode parent, ModelNode child, WellFormedChoiceChecker checker, ModelNode visited) throws ScribbleException
	{
		WellFormedChoiceEnv env = checker.popEnv();
		//env = checker.popEnv().merge(env);  // No merge here: merging of child blocks is handled "manually" by the compound interaction nodes
		//checker.pushEnv(env);
		setEnv(env);
		return visited;
	}

	/*@Override
	public Projector enterProjection(ModelNode parent, ModelNode child, Projector proj) throws ScribbleException
	{
		ProjectionEnv env = proj.peekEnv().push();
		proj.pushEnv(env);
		return proj;
	}
	//public void enter(Choice<? extends ProtocolBlock<? extends InteractionSequence<? extends InteractionNode>>> cho, WellFormedChoiceChecker checker)
	
	@Override
	public ModelNode leaveProjection(ModelNode parent, ModelNode child, Projector proj, ModelNode visited) throws ScribbleException
	{
		ProjectionEnv env = proj.popEnv();
		//env = checker.popEnv().merge(env);  // No merge here: merging of child blocks is handled "manually" by the compound interaction nodes
		//checker.pushEnv(env);
		setEnv(env);
		return visited;
	}*/
}
