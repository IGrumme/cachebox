package CB_UI.Solver.Functions;

import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI.Solver.DataTypes.DataType;

public class FunctionQuerprodukt extends Function
{
	private static final long serialVersionUID = 8720582376213442054L;

	public FunctionQuerprodukt()
	{
		Names.add(new LacalNames("Crossproduct", "en"));
		Names.add(new LacalNames("Querprodukt", "de"));
		Names.add(new LacalNames("CP", "en"));
		Names.add(new LacalNames("QP", "de"));
	}

	@Override
	public String getName()
	{
		return Translation.Get("solverFuncCrossproduct");
	}

	@Override
	public String getDescription()
	{
		return Translation.Get("solverDescCrossprocuct");
	}

	@Override
	public String Calculate(String[] parameter)
	{
		if (parameter.length != 1)
		{
			return Translation.Get("solverErrParamCount", "1", "$solverFuncCrossproduct");
		}
		String wert = parameter[0].trim();
		int result = 1;
		for (char c : wert.toCharArray())
		{
			int i = (int) c - 48;
			if ((i >= 0) && (i <= 9)) result *= i;
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

	@Override
	public DataType getParamType(int i)
	{
		switch (i)
		{
		case 0:
			return DataType.Integer;
		default:
			return DataType.None;
		}
	}

	@Override
	public DataType getReturnType()
	{
		return DataType.Integer;
	}

	@Override
	public String getParamName(int i)
	{
		switch (i)
		{
		case 0:
			return "solverParamInteger";
		default:
			return super.getParamName(i);
		}
	}

}
