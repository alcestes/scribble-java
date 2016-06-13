package ast;

import org.scribble.ast.Module;
import org.scribble.ast.context.ModuleContext;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.del.ModuleDel;
import org.scribble.main.MainContext;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.util.ScribParserException;
import org.scribble.visit.Job;
import org.scribble.visit.JobContext;

import ast.global.GlobalType;
import ast.global.GlobalTypeTranslator;

public class ScribProtocolTranslator
{
	//public GlobalType parseAndCheck(Path mainmod, String simplename) throws ScribbleException, ScribParserException
	public GlobalType parseAndCheck(MainContext maincon, GProtocolName simplename) throws ScribbleException, ScribParserException
	{
		/*Module main = parseMainScribModule(mainmod);
		Job job = new Job(false, parsed, main.getFullModuleName(), false, false, false, false);*/

		//MainContext maincon = newMainContext(mainmod);
		Job job = new Job(maincon.debug, maincon.getParsedModules(), maincon.main, maincon.useOldWF, maincon.noLiveness, maincon.minEfsm, maincon.fair);
		job.checkLinearMPScalaWellFormedness();  // FIXME TODO
		Module main = job.getContext().getMainModule();

		GProtocolDecl gpd = (GProtocolDecl) main.getProtocolDecl(simplename);  // FIXME: cast
		ModuleContext mainmodc = ((ModuleDel) main.del()).getModuleContext();
		JobContext jobc = job.getContext();
		return new GlobalTypeTranslator(jobc, mainmodc).translate(gpd);
	}
	
	// TODO: doesn't support Scribble module imports yet (no import path given to resource locator)
	/*private Module parseMainScribModule(Path mainmod) throws ScribbleException, ScribParserException
	{
		AntlrParser antlrParser = new AntlrParser();
		ScribParser scribParser = new ScribParser();
		//ResourceLocator locator = new DirectoryResourceLocator(Collections.emptyList()); 
		//this.loader = new ScribModuleLoader(this.locator, this.antlrParser, this.scribParser);
		Resource res = DirectoryResourceLocator.getResourceByFullPath(mainmod);
		Module main = (Module) scribParser.parse(antlrParser.parseAntlrTree(res));
		Map<ModuleName, Module> parsed = new HashMap<>();
		parsed.put(main.getFullModuleName(), main);
		//Job job = new Job(false, parsed, main.getFullModuleName(), false, false, false, false);
		MainContext mc = newMainContext(mainmod);
		Job job = new Job(mc.debug, mc.getParsedModules(), mc.main, mc.useOldWF, mc.noLiveness, mc.minEfsm, mc.fair);
		job.checkLinearMPScalaWellFormedness();  // FIXME TODO
		return job.getContext().getMainModule();
	}*/
}
