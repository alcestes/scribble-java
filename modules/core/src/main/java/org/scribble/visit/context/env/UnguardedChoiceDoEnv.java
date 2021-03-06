package org.scribble.visit.context.env;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.sesstype.name.Role;
import org.scribble.visit.env.Env;

// Cf. UnfoldingEnv
public class UnguardedChoiceDoEnv extends Env<UnguardedChoiceDoEnv>
{
	//private boolean shouldPrune;
	public Set<Role> subjs;  // If well-formed (including wrt. local choice syntax) should be a singleton: but this is currently checked in subsequent choice subject inference pass, not here
	
	public UnguardedChoiceDoEnv()
	{
		//this.shouldPrune = true;
		this.subjs = new HashSet<>();
	}

	//protected ChoiceUnguardedSubprotocolEnv(boolean shouldPrune, Set<Role> subjs)
	protected UnguardedChoiceDoEnv(Set<Role> subjs)
	{
		//this.shouldPrune = shouldUnfold;
		this.subjs = new HashSet<>(subjs);
	}

	@Override
	protected UnguardedChoiceDoEnv copy()
	{
		//return new ChoiceUnguardedSubprotocolEnv(this.shouldPrune, this.subjs);
		return new UnguardedChoiceDoEnv(this.subjs);
	}

	@Override
	public UnguardedChoiceDoEnv enterContext()
	{
		return copy();
	}

	@Override
	public UnguardedChoiceDoEnv mergeContext(UnguardedChoiceDoEnv env)
	{
		return mergeContexts(Arrays.asList(env));
	}

	@Override
	public UnguardedChoiceDoEnv mergeContexts(List<UnguardedChoiceDoEnv> envs)
	{
		UnguardedChoiceDoEnv copy = copy();
		//copy.shouldPrune = (envs.stream().filter((e) -> !e.shouldPrune).count() > 0);  // Look for false, cf. UnfoldingEnv
		//copy.subjs = envs.stream().flatMap((e) -> e.subjs.stream()).collect(Collectors.toSet());
		copy.subjs.addAll(envs.stream().flatMap((e) -> e.subjs.stream()).collect(Collectors.toSet()));

		//System.out.println("ddd1: " + envs);
		//System.out.println("ddd2: " + copy.subjs);
		
		return copy;
	}
	
	public UnguardedChoiceDoEnv setChoiceSubject(Role r)
	{
		UnguardedChoiceDoEnv copy = copy();
		if (copy.subjs.isEmpty())
		{
			//System.out.println("BBB: " + r);
			copy.subjs.add(r);
		}
		return copy;
	}

	/*public boolean shouldPrune()
	{
		return this.shouldPrune;
	}

	public ChoiceUnguardedSubprotocolEnv disablePrune()
	{	
		ChoiceUnguardedSubprotocolEnv copy = copy();
		copy.shouldPrune = false;
		return copy;
	}*/
	
	@Override
	public String toString()
	{
		return super.toString() + ": " + this.subjs;
	}
}
