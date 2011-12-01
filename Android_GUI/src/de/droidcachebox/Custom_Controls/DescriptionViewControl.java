package de.droidcachebox.Custom_Controls;

import java.util.ArrayList;
import java.util.Iterator;

import CB_Core.Config;
import CB_Core.GlobalCore;
import CB_Core.DAO.CacheDAO;
import CB_Core.DB.Database;
import CB_Core.Enums.Attributes;
import CB_Core.Log.Logger;
import CB_Core.Types.Cache;
import CB_Core.Types.Waypoint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import de.droidcachebox.Global;
import de.droidcachebox.main;
import de.droidcachebox.Events.ViewOptionsMenu;
import de.droidcachebox.Geocaching.DescriptionImageGrabber;
import de.droidcachebox.Views.Forms.MessageBox;
import de.droidcachebox.Views.Forms.MessageBoxButtons;
import de.droidcachebox.Views.Forms.MessageBoxIcon;

public class DescriptionViewControl extends WebView implements ViewOptionsMenu
{

	public static boolean mustLoadDescription;
	private Cache aktCache;
	private ArrayList<String> NonLocalImages = new ArrayList<String>();
	private ArrayList<String> NonLocalImagesUrl = new ArrayList<String>();
	private static ProgressDialog pd;

	public DescriptionViewControl(Context context)
	{
		super(context);

	}

	public DescriptionViewControl(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mustLoadDescription = false;

		this.setDrawingCacheEnabled(false);
		this.setAlwaysDrawnWithCacheEnabled(false);

		// this.getSettings().setJavaScriptEnabled(true);
		this.getSettings().setLightTouchEnabled(false);
		this.getSettings().setLoadWithOverviewMode(true);
		this.getSettings().setSupportZoom(true);
		this.getSettings().setBuiltInZoomControls(true);
		this.getSettings().setJavaScriptEnabled(true);
		this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

		this.setWebViewClient(new WebViewClient()
		{

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if (url.contains("fake://fake.de/Attr"))
				{
					int pos = url.indexOf("+");
					if (pos < 0) return true;

					final String attr = url.substring(pos + 1, url.length() - 1);

					MessageBox.Show(GlobalCore.Translations.Get(attr));
					return true;
				}
				else if (url.contains("fake://fake.de/download"))
				{

					Thread thread = new Thread()
					{
						public void run()
						{

							String accessToken = Config.GetAccessToken();
							if (!CB_Core.Api.GroundspeakAPI.CacheStatusValid)
							{
								int result = CB_Core.Api.GroundspeakAPI.GetCacheLimits(accessToken);
								if (result != 0)
								{
									onlineSearchReadyHandler.sendMessage(onlineSearchReadyHandler.obtainMessage(1));
									return;
								}
							}
							if (CB_Core.Api.GroundspeakAPI.CachesLeft <= 0)
							{
								String s = "Download limit is reached!\n";
								s += "You have downloaded the full cache details of " + CB_Core.Api.GroundspeakAPI.MaxCacheCount
										+ " caches in the last 24 hours.\n";
								if (CB_Core.Api.GroundspeakAPI.MaxCacheCount < 10) s += "If you want to download the full cache details of 6000 caches per day you can upgrade to Premium Member at \nwww.geocaching.com!";

								message = s;

								onlineSearchReadyHandler.sendMessage(onlineSearchReadyHandler.obtainMessage(2));

								return;
							}

							if (!CB_Core.Api.GroundspeakAPI.IsPremiumMember(accessToken))
							{
								String s = "Download Details of this cache?\n";
								s += "Full Downloads left: " + CB_Core.Api.GroundspeakAPI.CachesLeft + "\n";
								s += "Actual Downloads: " + CB_Core.Api.GroundspeakAPI.CurrentCacheCount + "\n";
								s += "Max. Downloads in 24h: " + CB_Core.Api.GroundspeakAPI.MaxCacheCount;
								message = s;
								onlineSearchReadyHandler.sendMessage(onlineSearchReadyHandler.obtainMessage(3));
								return;
							}
							else
							{
								// call the download directly
								onlineSearchReadyHandler.sendMessage(onlineSearchReadyHandler.obtainMessage(4));
								return;
							}
						}
					};
					pd = ProgressDialog.show(getContext(), "", "Download Description", true);

					thread.start();

					return true;
				}
				view.loadUrl(url);
				return true;
			}

		});

	}

	private String message = "";
	private Handler onlineSearchReadyHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 1:
			{
				pd.dismiss();
				break;
			}
			case 2:
			{
				pd.dismiss();
				MessageBox.Show(message, GlobalCore.Translations.Get("GC_title"), MessageBoxButtons.OKCancel,
						MessageBoxIcon.Powerd_by_GC_Live, null);
				break;
			}
			case 3:
			{
				pd.dismiss();
				MessageBox.Show(message, GlobalCore.Translations.Get("GC_title"), MessageBoxButtons.OKCancel,
						MessageBoxIcon.Powerd_by_GC_Live, DownloadCacheDialogResult);
				break;
			}
			case 4:
			{
				pd.dismiss();
				DownloadCacheDialogResult.onClick(null, -1);
				break;
			}
			}
		}
	};

	private DialogInterface.OnClickListener DownloadCacheDialogResult = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialog, int button)
		{
			switch (button)
			{
			case -1:
				CacheDAO dao = new CacheDAO();
				Cache newCache = dao.LoadApiDetails(aktCache);
				if (newCache != null)
				{
					aktCache = newCache;
					setCache(newCache);

					// hier ist kein AccessToke mehr notwendig, da diese Info
					// bereits im Cache sein muss!
					if (!CB_Core.Api.GroundspeakAPI.IsPremiumMember(""))
					{
						String s = "Download successful!\n";
						s += "Downloads left for today: " + CB_Core.Api.GroundspeakAPI.CachesLeft + "\n";
						s += "If you upgrade to Premium Member you are allowed to download the full cache details of 6000 caches per day and you can search not only for traditional caches (www.geocaching.com).";

						MessageBox.Show(s, GlobalCore.Translations.Get("GC_title"), MessageBoxButtons.OKCancel,
								MessageBoxIcon.Powerd_by_GC_Live, null);
					}
				}
				break;
			}
			if (dialog != null) dialog.dismiss();
		}
	};

	public DescriptionViewControl(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

	}

	public DescriptionViewControl(Context context, String text)
	{
		super(context);

	}

	private int downloadTryCounter = 0;

	public void setCache(Cache cache)
	{
		final String mimeType = "text/html";
		final String encoding = "utf-8";
		if (cache != null)
		{
			NonLocalImages = new ArrayList<String>();
			NonLocalImagesUrl = new ArrayList<String>();
			String cachehtml = Database.GetDescription(cache);
			String html = "";
			if (cache.ApiStatus == 1)// GC.com API lite
			{ // Load Standard HTML
				String nodesc = GlobalCore.Translations.Get("GC_NoDescription");
				html = "</br>" + nodesc + "</br></br></br><form action=\"download\"><input type=\"submit\" value=\" "
						+ GlobalCore.Translations.Get("GC_DownloadDescription") + " \"></form>";
			}
			else
			{
				html = DescriptionImageGrabber.ResolveImages(cache, cachehtml, false, NonLocalImages, NonLocalImagesUrl);

				if (!Config.settings.DescriptionNoAttributes.getValue()) html = getAttributesHtml(cache) + html;

			}

			try
			{
				this.loadDataWithBaseURL("fake://fake.de", html, mimeType, encoding, null);
			}
			catch (Exception e)
			{
				return; // if an exception here, then this is not initializes
			}
		}
		this.getSettings().setLightTouchEnabled(true);

		// Falls nicht geladene Bilder vorliegen und eine Internetverbindung
		// erlaubt ist, diese laden und Bilder erneut aufl�sen
		if (NonLocalImagesUrl.size() > 0)
		{
			downloadThread = new Thread()
			{
				public void run()
				{

					if (downloadTryCounter > 0)
					{
						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException e)
						{
							Logger.Error("DescriptionViewControl.setCache()", "Thread.sleep fehler", e);
							e.printStackTrace();
						}
					}

					while (NonLocalImagesUrl != null && NonLocalImagesUrl.size() > 0)
					{
						String local, url;
						local = NonLocalImages.get(0);
						url = NonLocalImagesUrl.get(0);
						NonLocalImagesUrl.remove(0);
						NonLocalImages.remove(0);
						try
						{
							DescriptionImageGrabber.Download(url, local);
						}
						catch (Exception e)
						{
							Logger.Error("DescriptionViewControl.setCache()", "downloadThread run()", e);
						}
					}
					downloadReadyHandler.post(downloadComplete);

				}
			};
			downloadThread.start();
		}

	}

	final Handler downloadReadyHandler = new Handler();
	Thread downloadThread;

	final Runnable downloadComplete = new Runnable()
	{
		public void run()
		{
			Global.setDebugMsg("Reload " + String.valueOf(downloadTryCounter++));
			if (downloadTryCounter < 10) // nur 10 Download versuche zu lassen
			setCache(aktCache);
		}
	};

	private String getAttributesHtml(Cache cache)
	{
		StringBuilder sb = new StringBuilder();

		Iterator<Attributes> attrs = cache.getAttributes().iterator();

		if (attrs == null || !attrs.hasNext()) return "";

		do
		{

			Attributes attribute = attrs.next();
			sb.append("<form action=\"Attr\">");
			sb.append("<input name=\"Button\" type=\"image\" src=\"file://" + Config.WorkPath + "/data/Attributes/"
					+ attribute.getImageName() + ".png\" value=\" " + attribute.getImageName() + " \">");
		}
		while (attrs.hasNext());

		sb.append("</form>");

		if (sb.length() > 0) sb.append("<br>");
		return sb.toString();
	}

	@Override
	public boolean ItemSelected(MenuItem item)
	{
		return false;
	}

	@Override
	public void BeforeShowMenu(Menu menu)
	{
	}

	@Override
	public void OnShow()
	{

		SetSelectedCache(GlobalCore.SelectedCache(), GlobalCore.SelectedWaypoint());
		this.getParent().requestLayout();

		//
		//
		// android.view.ViewGroup.LayoutParams params = this.getLayoutParams();
		//
		// params.height = 416;
		// params.width = 480;
		//
		// this.setLayoutParams(params);

		if (downloadTryCounter > 9) mustLoadDescription = true; // Versuchs
																// nochmal mit
																// dem Download
		downloadTryCounter = 0;
		if (mustLoadDescription)
		{
			setCache(aktCache);
			mustLoadDescription = false;
		}

		// im Day Mode brauchen wir kein InvertView
		// das sollte mehr Performance geben
		if (main.N)
		{
			invertViewControl.Me.setVisibility(VISIBLE);
		}
		else
		{
			invertViewControl.Me.setVisibility(GONE);
		}

		this.setWillNotDraw(false);
		this.invalidate();
	}

	@Override
	public void OnHide()
	{
		try
		{
			// this.clearCache(true);
		}
		catch (Exception e)
		{
			Logger.Error("DescriptionViewControl.OnHide()", "clearCache", e);
			e.printStackTrace();
		}
	}

	@Override
	public void OnFree()
	{
		try
		{
			// this.clearCache(true);
		}
		catch (Exception e)
		{
			Logger.Error("DescriptionViewControl.OnFree()", "clearCache", e);
			e.printStackTrace();
		}
		this.destroy();
	}

	public void SetSelectedCache(Cache cache, Waypoint waypoint)
	{
		aktCache = cache;
		mustLoadDescription = true;
		setCache(aktCache);
	}

	@Override
	public int GetMenuId()
	{
		return 0;
	}

	@Override
	public void ActivityResult(int requestCode, int resultCode, Intent data)
	{
	}

	@Override
	public int GetContextMenuId()
	{
		return 0;
	}

	@Override
	public void BeforeShowContextMenu(Menu menu)
	{
	}

	@Override
	public boolean ContextMenuItemSelected(MenuItem item)
	{
		return false;
	}

	public static boolean isDrawn = false;

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		isDrawn = true;
		invertViewControl.Me.invalidate();
	}

}
