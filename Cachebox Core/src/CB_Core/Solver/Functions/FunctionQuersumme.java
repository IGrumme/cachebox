package CB_Core.Solver.Functions;

import CB_Core.TranslationEngine.Translation;

public class FunctionQuersumme extends Function
{
	public FunctionQuersumme()
	{
		Names.add(new LacalNames("Crosstotal", "en"));
		Names.add(new LacalNames("Quersumme", "de"));
		Names.add(new LacalNames("CT", "en"));
		Names.add(new LacalNames("QS", "de"));
	}

	@Override
	public String getName()
	{
		return Translation.Get("solverFuncCrosstotal");
	}

	@Override
	public String getDescription()
	{
		return Translation.Get("solverDescCrosstotal");
	}

	@Override
	public String Calculate(String[] parameter)
	{
		if (parameter.length != 1)
		{
			return Translation.Get("solverErrParamCount", "1", "$solverFuncCrosstotal");
		}
		String wert = parameter[0].trim();
		int result = 0;
		for (char c : wert.toCharArray())
		{
			int i = (int) c - 48;
			if ((i >= 0) && (i <= 9)) result += i;
		}
		return String.valueOf(result);
	}

	@Override
	public int getAnzParam()
	{
		return 1;
	}

	@Override
	public boolean needsTextArgument()
	{
		return false;
	}

}
