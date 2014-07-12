package bsh;

import java.util.Hashtable;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
/*
 * Note: which of these things should be checked at parse time vs. run time?
 */
public class Modifiers implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int CLASS = 0, METHOD = 1, FIELD = 2;
	@SuppressWarnings("rawtypes")
	Hashtable modifiers;

	/**
	 * @param context
	 *            is METHOD or FIELD
	 */
	@SuppressWarnings(
		{ "rawtypes", "unchecked" })
	public void addModifier(int context, String name)
	{
		if (modifiers == null) modifiers = new Hashtable();

		Object existing = modifiers.put(name, Void.TYPE/* arbitrary flag */);
		if (existing != null) throw new IllegalStateException("Duplicate modifier: " + name);

		int count = 0;
		if (hasModifier("private")) ++count;
		if (hasModifier("protected")) ++count;
		if (hasModifier("public")) ++count;
		if (count > 1) throw new IllegalStateException("public/private/protected cannot be used in combination.");

		switch (context)
		{
		case CLASS:
			validateForClass();
			break;
		case METHOD:
			validateForMethod();
			break;
		case FIELD:
			validateForField();
			break;
		}
	}

	@SuppressWarnings("rawtypes")
	public boolean hasModifier(String name)
	{
		if (modifiers == null) modifiers = new Hashtable();
		return modifiers.get(name) != null;
	}

	// could refactor these a bit
	private void validateForMethod()
	{
		insureNo("volatile", "Method");
		insureNo("transient", "Method");
	}

	private void validateForField()
	{
		insureNo("synchronized", "Variable");
		insureNo("native", "Variable");
		insureNo("abstract", "Variable");
	}

	private void validateForClass()
	{
		validateForMethod(); // volatile, transient
		insureNo("native", "Class");
		insureNo("synchronized", "Class");
	}

	private void insureNo(String modifier, String context)
	{
		if (hasModifier(modifier)) throw new IllegalStateException(context + " cannot be declared '" + modifier + "'");
	}

	@Override
	public String toString()
	{
		return "Modifiers: " + modifiers;
	}

}
