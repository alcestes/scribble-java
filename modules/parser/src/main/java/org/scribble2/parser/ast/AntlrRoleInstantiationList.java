package org.scribble2.parser.ast;

import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.scribble2.model.RoleInstantiation;
import org.scribble2.model.RoleInstantiationList;
import org.scribble2.parser.AntlrModuleParser;
import org.scribble2.parser.util.Util;

public class AntlrRoleInstantiationList
{
	public static RoleInstantiationList parseRoleInstantiationList(AntlrModuleParser parser, CommonTree ct)
	{
		List<RoleInstantiation> ris = new LinkedList<>();
		for (CommonTree ri : getRoleInstantiationChildren(ct))
		{
			ris.add((RoleInstantiation) parser.parse(ri));
		}
		return new RoleInstantiationList(ris);
	}

	public static final List<CommonTree> getRoleInstantiationChildren(CommonTree ct)
	{
		return Util.toCommonTreeList(ct.getChildren());
	}
}
