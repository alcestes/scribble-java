package ast.local;

import ast.name.RecVar;

public class LocalRec implements LocalType
{
	//public final Role self;
	
	public final RecVar recvar;
	public final LocalType body;
	
	//public LocalRec(Role self, RecVar recvar, LocalType body)
	public LocalRec(RecVar recvar, LocalType body)
	{
		//this.self = self;
		this.recvar = recvar;
		this.body = body;
	}
	
	@Override
	public String toString()
	{
		return "mu " + this.recvar + "." + this.body;
	}

	@Override
	public int hashCode()
	{
		final int prime = 41;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((recvar == null) ? 0 : recvar.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof LocalRec))
		{
			return false;
		}
		LocalRec other = (LocalRec) obj;
		if (body == null)
		{
			if (other.body != null)
			{
				return false;
			}
		} else if (!body.equals(other.body))
		{
			return false;
		}
		if (recvar == null)
		{
			if (other.recvar != null)
			{
				return false;
			}
		} else if (!recvar.equals(other.recvar))
		{
			return false;
		}
		return true;
	}
}
