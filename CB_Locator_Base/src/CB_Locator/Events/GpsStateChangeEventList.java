package CB_Locator.Events;

import java.util.ArrayList;

import CB_Locator.Locator;

public class GpsStateChangeEventList
{
	public static ArrayList<GpsStateChangeEvent> list = new ArrayList<GpsStateChangeEvent>();

	public static void Add(GpsStateChangeEvent event)
	{
		synchronized (list)
		{
			if (!list.contains(event)) list.add(event);
		}

	}

	public static void Remove(GpsStateChangeEvent event)
	{
		synchronized (list)
		{
			list.remove(event);
		}
	}

	private static int count = 0;

	public static long minEventTime = Long.MAX_VALUE;

	public static long lastTime = 0;

	public static long maxEventListTime = 0;
	private static long lastChanged = 0;

	static Thread threadCall;

	public static void Call()
	{
		synchronized (list)
		{

			minEventTime = Math.min(minEventTime, System.currentTimeMillis() - lastTime);
			lastTime = System.currentTimeMillis();

			if (lastChanged != 0 && lastChanged > System.currentTimeMillis() - Locator.getMinUpdateTime())
			{
				return;
			}
			lastChanged = System.currentTimeMillis();

			if (threadCall != null)
			{
				if (threadCall.getState() != Thread.State.TERMINATED) return;
				else
					threadCall = null;
			}

			threadCall = new Thread(new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						synchronized (list)
						{
							long thradStart = System.currentTimeMillis();
							count++;
							for (GpsStateChangeEvent event : list)
							{

								FireEvent(event);

							}
							if (count > 10) count = 0;

							maxEventListTime = Math.max(maxEventListTime, System.currentTimeMillis() - thradStart);
						}
					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});

			threadCall.run();

		}
	}

	private static void FireEvent(GpsStateChangeEvent event)
	{
		event.GpsStateChanged();
		// Log.d("CACHEBOX", "GPS State Change called " + event.toString());
	}

}
