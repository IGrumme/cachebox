package CB_Core.Solver.Functions;

import CB_Core.TranslationEngine.Translation;

public class FunctionLength extends Function
{
	public FunctionLength()
	{
		Names.add(new LacalNames("Length", "en"));
		Names.add(new LacalNames("L�nge", "de"));
		Names.add(new LacalNames("Len", "en"));
		Names.add(new LacalNames("Len", "de"));
	}

	@Override
	public String getName()
	{
		return Translation.Get("solverFuncLength");
	}

	@Override
	public String getDescription()
	{
		return Translation.Get("solverDescLength");
	}

	@Override
	public String Calculate(String[] parameter)
	{
		if (parameter.length != 1)
		{
			return Translation.Get("solverErrParamCount", "1", "$solverFuncLength");
		}
		return String.valueOf(parameter[0].length());
	}

	@Override
	public int getAnzParam()
	{
		return 1;
	}

	@Override
	public boolean needsTextArgument()
	{
		return true;
	}

}
