package CB_UI.GL_UI.Controls.Dialogs;

import CB_Core.Solver.DataTypes.DataType;
import CB_Core.Solver.Solver;
import CB_Core.Solver.Functions.Function;
import CB_Translation_Base.TranslationEngine.Translation;
import CB_UI.GL_UI.Activitys.SelectSolverFunction;
import CB_UI.GL_UI.Activitys.SelectSolverFunction.IFunctionResult;
import CB_UI_Base.Enums.WrapType;
import CB_UI_Base.Events.KeyboardFocusChangedEvent;
import CB_UI_Base.Events.KeyboardFocusChangedEventList;
import CB_UI_Base.GL_UI.Fonts;
import CB_UI_Base.GL_UI.GL_View_Base;
import CB_UI_Base.GL_UI.SpriteCacheBase;
import CB_UI_Base.GL_UI.Controls.Box;
import CB_UI_Base.GL_UI.Controls.Button;
import CB_UI_Base.GL_UI.Controls.EditTextField;
import CB_UI_Base.GL_UI.Controls.EditTextFieldBase;
import CB_UI_Base.GL_UI.Controls.Label;
import CB_UI_Base.GL_UI.Controls.MultiToggleButton;
import CB_UI_Base.GL_UI.Controls.MultiToggleButton.OnStateChangeListener;
import CB_UI_Base.GL_UI.Controls.MessageBox.ButtonScrollDialog;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBoxButtons;
import CB_UI_Base.GL_UI.Controls.MessageBox.MessageBoxIcon;
import CB_UI_Base.GL_UI.GL_Listener.GL;
import CB_UI_Base.Math.CB_RectF;
import CB_UI_Base.Math.GL_UISizes;
import CB_UI_Base.Math.SizeF;
import CB_UI_Base.Math.UI_Size_Base;

import com.badlogic.gdx.graphics.Color;

public class SolverDialog extends ButtonScrollDialog implements OnStateChangeListener, KeyboardFocusChangedEvent
{
	private enum pages
	{
		Nothing, Text, Function, Variable, Operator, Waypoint
	}

	private float initialYpos;
	private float boxYPosStart;
	private float boxYPosStored;

	private Box mBox;
	private boolean ignoreStateChange = false;
	private MultiToggleButton btnTxt;
	private MultiToggleButton btnFx;
	private MultiToggleButton btnVar;
	private MultiToggleButton btnOp;
	private MultiToggleButton btnWp;
	private SizeF msgBoxContentSize;
	private float TextFieldHeight;
	private pages page;
	private String sVar;
	private String sForm;
	private float startY;
	// Controls for TextView
	private EditTextField mFormulaField;

	// Controls for FormulaView
	// Controls for VariableView
	private EditTextField tbFunction;
	private Button bFunction;
	private EditTextField[] tbFunctionParam = null;
	private Label[] lFunctionParam = null;

	// Controls for OperatorView
	// Controls for WaypointView

	public interface SolverBackStringListner
	{
		public void BackString(String backString);
	}

	private EditTextField mVariableField;
	private String mSolverString;

	private SolverBackStringListner mBackStringListner;
	private Solver solver;

	public SolverDialog(CB_RectF rec, String name, Solver solver, String SolverString)
	{
		super(rec, name, "Solver", Translation.Get("solver_formula"), MessageBoxButtons.OKCancel, MessageBoxIcon.None, null);
		this.solver = solver;
		mSolverString = SolverString;
		ignoreStateChange = false;
		page = pages.Nothing;
		dontRenderDialogBackground = true;
	}

	private void initialLayout()
	{
		// Split Solver String by =
		String[] solverStrings = mSolverString.split("=", 2);
		sVar = "";
		sForm = "";
		if (solverStrings.length == 1)
		{
			sForm = solverStrings[0];
		}
		else if (solverStrings.length > 1)
		{
			sVar = solverStrings[0];
			sForm = solverStrings[1];
		}

		msgBoxContentSize = getContentSize();
		// initial VariableField
		TextFieldHeight = Fonts.getNormal().getLineHeight() * 2.5f;

		float y = msgBoxContentSize.height - TextFieldHeight;

		CB_RectF rec = new CB_RectF(0, y, msgBoxContentSize.width, TextFieldHeight);
		mVariableField = new EditTextField(this, rec, WrapType.SINGLELINE, "SolverDialogTextField");
		mVariableField.setText(sVar);
		// mVariableField.setMsg("Enter formula");
		scrollBox.addLast(mVariableField);
		y -= TextFieldHeight * 0.8;

		rec = new CB_RectF(0, y, msgBoxContentSize.width, TextFieldHeight);
		Label lbGleich = new Label(CB_RectF.ScaleCenter(rec, 0.8f), "=");
		lbGleich.setFont(Fonts.getNormal());
		lbGleich.setText("=");
		setBackground(SpriteCacheBase.activityBackground);
		scrollBox.addLast(lbGleich);
		y -= TextFieldHeight * 0.8;

		// Buttons zur Auswahl des Dialog-Typs
		float w = msgBoxContentSize.width / 5;
		float x = 0;
		btnTxt = new MultiToggleButton(x, y, w, UI_Size_Base.that.getButtonHeight(), "TXT");
		scrollBox.addNext(btnTxt);
		x += w;
		btnFx = new MultiToggleButton(x, y, w, UI_Size_Base.that.getButtonHeight(), "f(x)");
		scrollBox.addNext(btnFx);
		x += w;
		btnVar = new MultiToggleButton(x, y, w, UI_Size_Base.that.getButtonHeight(), "@");
		scrollBox.addNext(btnVar);
		x += w;
		btnOp = new MultiToggleButton(x, y, w, UI_Size_Base.that.getButtonHeight(), "+-");
		scrollBox.addNext(btnOp);
		x += w;
		btnWp = new MultiToggleButton(x, y, w, UI_Size_Base.that.getButtonHeight(), "$GC");
		scrollBox.addLast(btnWp);

		// startposition for further controls
		this.startY = y;

		String caption = Translation.Get("TXT");
		btnTxt.setText(caption);
		btnTxt.addState(caption, Color.GRAY);
		btnTxt.addState(caption, Color.GREEN);
		btnTxt.setOnStateChangedListner(this);

		caption = Translation.Get("f(x)");
		btnFx.setText(caption);
		btnFx.addState(caption, Color.GRAY);
		btnFx.addState(caption, Color.GREEN);
		btnFx.setOnStateChangedListner(this);

		caption = Translation.Get("@");
		btnVar.setText(caption);
		btnVar.addState(caption, Color.GRAY);
		btnVar.addState(caption, Color.GREEN);
		btnVar.setOnStateChangedListner(this);

		caption = Translation.Get("+-*/");
		btnOp.setText(caption);
		btnOp.addState(caption, Color.GRAY);
		btnOp.addState(caption, Color.GREEN);
		btnOp.setOnStateChangedListner(this);

		caption = Translation.Get("$GC");
		btnWp.setText(caption);
		btnWp.addState(caption, Color.GRAY);
		btnWp.addState(caption, Color.GREEN);
		btnWp.setOnStateChangedListner(this);

		button3.setText(Translation.Get("close"));
		button1.setText(Translation.Get("ok"));

		button1.setOnClickListener(OnOkClickListner);

		// y -= UiSizes.getButtonHeight();
		float restPlatz = this.getHeight() - y;
		// Dieses LinearLayout wird dann in eine ScrollBox verpackt, damit dies Scrollbar ist, wenn die L�nge den Anzeige Bereich
		// �berschreitet!

		rec = new CB_RectF(0, y - restPlatz, msgBoxContentSize.width, restPlatz);

		// Initial LinearLayout
		// Dieses wird nur mit der Breite Initialisiert, die H�he ergibt sich aus dem Inhalt
		mBox = new Box(rec.getWidth(), innerHeight, "SelectSolverFunction-Box");
		float margin = GL_UISizes.margin;
		mBox.setMargins(margin, margin);
		mBox.initRow();
		boxYPosStart = mBox.getRowYPos(); // Startposition der Controls merken
		// damit das LinearLayout auch Events erh�llt
		mBox.setClickable(true);

		// add LinearLayout zu ScrollBox und diese zu der Activity
		scrollBox.addLast(mBox);

		showPage(pages.Text);
	}

	private OnClickListener OnOkClickListner = new OnClickListener()
	{

		@Override
		public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
		{
			// damit die �nderungen in sForm gespeichert werden
			showPage(pages.Nothing);
			String result = mVariableField.getText();
			if (result.length() > 0) result += "=";
			result += sForm;
			if (mBackStringListner != null) mBackStringListner.BackString(result);
			GL.that.closeDialog(SolverDialog.this);
			return true;
		}
	};

	public void show(SolverBackStringListner listner)
	{
		mBackStringListner = listner;
		initialLayout();
		GL.that.showDialog(this);

	}

	@Override
	public void onStateChange(GL_View_Base v, int State)
	{
		// Status�nderung eines MultiToggleButtons
		if (State == 1)
		{
			if (v == btnTxt)
			{
				showPage(pages.Text);
			}
			if (v == btnFx)
			{
				showPage(pages.Function);
			}
			if (v == btnVar)
			{
				showPage(pages.Variable);
			}
			if (v == btnOp)
			{
				showPage(pages.Operator);
			}
			if (v == btnWp)
			{
				showPage(pages.Waypoint);
			}
		}
	}

	private void showPage(pages page)
	{
		if (page == this.page) return;
		// set State of buttons
		btnTxt.setState(page == pages.Text ? 1 : 0);
		btnFx.setState(page == pages.Function ? 1 : 0);
		btnVar.setState(page == pages.Variable ? 1 : 0);
		btnOp.setState(page == pages.Operator ? 1 : 0);
		btnWp.setState(page == pages.Waypoint ? 1 : 0);

		// remove old controls
		switch (this.page)
		{
		case Text:
			hidePageText();
			break;
		case Function:
			hidePageFunction();
			break;
		case Variable:
			hidePageVariable();
			break;
		case Operator:
			hidePageOperator();
			break;
		case Waypoint:
			hidePageWaypoint();
			break;
		default:
			break;
		}

		// y-Position der Controls zur�cksetzen
		mBox.initRow(TOPDOWN, boxYPosStart);

		switch (page)
		{
		case Text:
			showPageText();
			break;
		case Function:
			showPageFunction();
			break;
		case Variable:
			showPageVariable();
			break;
		case Operator:
			showPageOperator();
			break;
		case Waypoint:
			showPageWaypoint();
			break;
		default:
			break;
		}
		this.page = page;
	}

	private void hidePageWaypoint()
	{

	}

	private void hidePageOperator()
	{

	}

	private void hidePageVariable()
	{
	}

	private void hidePageFunction()
	{
		// ge�nderte Formel merken
		sForm = tbFunction.getText();
		sForm += "(";
		for (int i = 0; i < tbFunctionParam.length; i++)
		{
			if (i > 0) sForm += "; ";
			sForm += tbFunctionParam[i].getText();
		}
		sForm += ")";
		// Parameter entfernen
		removeFunctionParam();
		mBox.removeChild(tbFunction);
		mBox.removeChild(bFunction);
		tbFunction = null;
		bFunction = null;
	}

	private void removeFunctionParam()
	{
		if (tbFunctionParam != null)
		{
			for (int i = 0; i < tbFunctionParam.length; i++)
			{
				mBox.removeChild(tbFunctionParam[i]);
			}
			tbFunctionParam = null;
		}
		if (lFunctionParam != null)
		{
			for (int i = 0; i < lFunctionParam.length; i++)
			{
				mBox.removeChild(lFunctionParam[i]);
			}
			lFunctionParam = null;
		}
		mBox.initRow(TOPDOWN, boxYPosStored); // Position der n�chsten Controls zur�cksetzen
	}

	private void hidePageText()
	{
		// ge�nderten Text merken
		sForm = mFormulaField.getText();
		mBox.removeChild(mFormulaField);
		mFormulaField = null;
	}

	private void showPageWaypoint()
	{

	}

	private void showPageOperator()
	{

	}

	private void showPageVariable()
	{
	}

	private void showPageFunction()
	{
		// initial VariableField
		float y = startY;
		final CB_RectF rec = new CB_RectF(0, y, msgBoxContentSize.width - TextFieldHeight * 2, TextFieldHeight);

		tbFunction = new EditTextField(this, rec, WrapType.SINGLELINE, "SolverDialogTextField");
		tbFunction.setText(sForm);

		mBox.addNext(tbFunction, 0.8f);
		float btnWidth = TextFieldHeight * 2;
		bFunction = new Button(scrollBox.getInnerWidth() - btnWidth, y, btnWidth, TextFieldHeight, "SolverDialogBtnVariable");

		bFunction.setText("F(x)");
		mBox.addLast(bFunction, 0.2f);
		boxYPosStored = mBox.getRowYPos(); // Y-Pos speichern damit nach dem l�schen von Controls die n�chsten wieder an der
											// richtigen Stelle
		// eingef�gt werden k�nnen
		// Funktion aufsplitten nach Funktionsname und Parameter (falls m�glich!)
		String formula = sForm.trim();
		int posKlammerAuf = formula.indexOf("(");
		int posKlammerZu = formula.lastIndexOf(")");
		if ((posKlammerAuf >= 0) && (posKlammerZu > posKlammerAuf))
		{
			// g�ltige Formel erkannt
			String function = formula.substring(0, posKlammerAuf);
			tbFunction.setText(function);
			String parameter = formula.substring(posKlammerAuf + 1, posKlammerZu);
			// Parameter nach ";" trennen
			String[] parameters = parameter.split(";");
			CB_RectF rec2 = rec.copy();
			// Parameter einr�cken
			rec2.setX(rec2.getX() + TextFieldHeight / 2);

			tbFunctionParam = new EditTextField[parameters.length];
			lFunctionParam = new Label[parameters.length];
			for (int i = 0; i < parameters.length; i++)
			{
				// Eingabefelder f�r die Parameter einf�gen
				rec2.setY(rec2.getY() - TextFieldHeight * 3 / 4);
				lFunctionParam[i] = new Label(rec2.ScaleCenter(0.6f), "LabelFunctionParam");
				// lFunctionParam[i].setVAlignment(VAlignment.BOTTOM);
				lFunctionParam[i].setText("Parameter" + " " + i);
				mBox.addNext(lFunctionParam[i], 0.3f);

				rec2.setY(rec2.getY() - lFunctionParam[i].getHeight() * 3 / 4);
				tbFunctionParam[i] = new EditTextField(SolverDialog.this, rec2, WrapType.SINGLELINE, "SolverDialogTextFieldParam");
				tbFunctionParam[i].setText(parameters[i].trim());
				mBox.addLast(tbFunctionParam[i], 0.7f);

			}
		}
		bFunction.setOnClickListener(new OnClickListener()
		{
			@Override
			public boolean onClick(GL_View_Base v, int x, int y, int pointer, int button)
			{
				// Funktionsauswahl zeigen
				SelectSolverFunction ssf = new SelectSolverFunction(solver, DataType.None, new IFunctionResult()
				{
					@Override
					public void selectedFunction(Function function)
					{
						if (function == null) return;
						tbFunction.setText(function.getLongLocalName());
						// evtl. vorhandene Parameter-Eingaben entfernen
						removeFunctionParam();
						tbFunctionParam = new EditTextField[function.getAnzParam()];
						lFunctionParam = new Label[function.getAnzParam()];
						rec.setX(rec.getX() + TextFieldHeight / 2);
						for (int i = 0; i < function.getAnzParam(); i++)
						{
							rec.setY(rec.getY() - TextFieldHeight * 3 / 4);
							lFunctionParam[i] = new Label(rec, "LabelFunctionParam");
							lFunctionParam[i].setText("Parameter " + i);
							mBox.addNext(lFunctionParam[i], 0.3f);
							rec.setY(rec.getY() - lFunctionParam[i].getHeight() * 3 / 4);
							tbFunctionParam[i] = new EditTextField(SolverDialog.this, rec, WrapType.SINGLELINE,
									"SolverDialogTextFieldParam");
							mBox.addLast(tbFunctionParam[i], 0.7f);
						}
					}
				});
				GL.that.showDialog(ssf);
				return true;
			}
		});
		y -= TextFieldHeight;
	}

	private void showPageText()
	{
		// ButtonDialog bd = new ButtonDialog("Name", "Title", "Message", MessageBoxButtons.OK, MessageBoxIcon.Error, null);
		// bd.Show();
		// if (true) return;
		// initial FormulaField
		float y = startY;
		CB_RectF rec = new CB_RectF(0, y, scrollBox.getInnerWidth(), TextFieldHeight);
		mFormulaField = new EditTextField(this, rec, WrapType.SINGLELINE, "SolverDialogTextField");
		mFormulaField.setText(sForm);
		mFormulaField.setZeroPos();
		mBox.addLast(mFormulaField);
		y -= TextFieldHeight;
	}

	@Override
	public void onShow()
	{
		initialYpos = this.getY();
		KeyboardFocusChangedEventList.Add(this);
	}

	@Override
	public void onHide()
	{
		KeyboardFocusChangedEventList.Remove(this);
	}

	@Override
	public void KeyboardFocusChanged(EditTextFieldBase focus)
	{
		/*
		 * Gdx.app.debug(Tag.TAG,"SolverDialog FocusChangedEvent");
		 * 
		 * if (focus == null) { this.setY(initialYpos); Gdx.app.debug(Tag.TAG,"SolverDialog set InitialPos - noFocus"); } else { float WorldY =
		 * focus.getWorldRec().getY(); if (UiSizes.getWindowHeight() / 2 > WorldY) { this.setY(UiSizes.getWindowHeight() - WorldY);
		 * Gdx.app.debug(Tag.TAG,"SolverDialog set Pos - " + (UiSizes.getWindowHeight() - WorldY)); } else {
		 * Gdx.app.debug(Tag.TAG,"SolverDialog dont set Pos - " + WorldY); } }
		 * 
		 * GL.that.renderOnce("SolverDialog Y-Pos Changed");
		 */
	}
}
