package CB_Core.Import;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.zip.ZipException;

import CB_Core.Config;
import CB_Core.FileIO;
import CB_Core.GlobalCore;
import CB_Core.Api.GroundspeakAPI;
import CB_Core.Api.PocketQuery.PQ;
import CB_Core.DAO.CacheDAO;
import CB_Core.DAO.GCVoteDAO;
import CB_Core.DAO.ImageDAO;
import CB_Core.DB.CoreCursor;
import CB_Core.DB.Database;
import CB_Core.DB.Database.Parameters;
import CB_Core.Events.ProgresssChangedEventList;
import CB_Core.GCVote.GCVote;
import CB_Core.GCVote.GCVoteCacheInfo;
import CB_Core.GCVote.RatingData;
import CB_Core.GL_UI.Controls.Dialogs.CancelWaitDialog;
import CB_Core.GL_UI.Controls.Dialogs.CancelWaitDialog.IcancelListner;
import CB_Core.Log.Logger;
import CB_Core.Types.Cache;
import CB_Core.Types.ImageEntry;

public class Importer
{
	public void importGC(ArrayList<PQ> pqList)
	{
		ProgresssChangedEventList.Call("import Gc.com", "", 0);

	}

	/**
	 * Importiert die GPX files, die sich in diesem Verzeichniss befinden. Auch wenn sie sich in einem Zip-File befinden. Oder das GPX-File
	 * falls eine einzelne Datei �bergeben wird.
	 * 
	 * @param directoryPath
	 * @param ip
	 * @return Cache_Log_Return mit dem Inhalt aller Importierten GPX Files
	 */
	public void importGpx(String directoryPath, ImporterProgress ip)
	{
		// resest import Counter

		GPXFileImporter.CacheCount = 0;
		GPXFileImporter.LogCount = 0;

		// Extract all Zip Files!

		File file = new File(directoryPath);

		if (file.isDirectory())
		{
			ArrayList<File> ordnerInhalt_Zip = FileIO.recursiveDirectoryReader(file, new ArrayList<File>(), "zip", false);

			ip.setJobMax("ExtractZip", ordnerInhalt_Zip.size());

			for (File tmpZip : ordnerInhalt_Zip)
			{
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e2)
				{
					return; // Thread Canceld
				}

				ip.ProgressInkrement("ExtractZip", "", false);
				// Extract ZIP
				try
				{
					UnZip.extractFolder(tmpZip.getAbsolutePath());
				}
				catch (ZipException e)
				{
					Logger.Error("Core.Importer.ImportGPX", "ZipException", e);
					e.printStackTrace();
				}
				catch (IOException e)
				{
					Logger.Error("Core.Importer.ImportGPX", "IOException", e);
					e.printStackTrace();
				}
			}

			if (ordnerInhalt_Zip.size() == 0)
			{
				ip.ProgressInkrement("ExtractZip", "", true);
			}

			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e2)
			{
				return; // Thread Canceld
			}
		}

		// Import all GPX files
		File[] FileList = GetFilesToLoad(directoryPath);

		ip.setJobMax("AnalyseGPX", FileList.length);

		ImportHandler importHandler = new ImportHandler();

		Integer countwpt = 0;
		HashMap<String, Integer> wptCount = new HashMap<String, Integer>();

		for (File File : FileList)
		{

			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e2)
			{
				return; // Thread Canceld
			}

			ip.ProgressInkrement("AnalyseGPX", File.getName(), false);

			BufferedReader br;
			String strLine;
			try
			{
				br = new BufferedReader(new InputStreamReader(new FileInputStream(File)));
				while ((strLine = br.readLine()) != null)
				{
					if (strLine.contains("<wpt")) countwpt++;
				}
			}
			catch (FileNotFoundException e1)
			{

				e1.printStackTrace();
			}
			catch (IOException e)
			{

				e.printStackTrace();
			}

			wptCount.put(File.getAbsolutePath(), countwpt);
			countwpt = 0;
		}

		if (FileList.length == 0)
		{
			ip.ProgressInkrement("AnalyseGPX", "", true);
		}

		for (Integer count : wptCount.values())
		{
			countwpt += count;
		}

		// Indiziere DB
		CacheInfoList.IndexDB();

		ip.setJobMax("ImportGPX", FileList.length + countwpt);
		for (File File : FileList)
		{
			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e2)
			{
				return; // Thread Canceled
			}

			ip.ProgressInkrement("ImportGPX", "Import: " + File.getName(), false);
			GPXFileImporter importer = new GPXFileImporter(File, ip);
			try
			{
				importer.doImport(importHandler, wptCount.get(File.getAbsolutePath()));
			}
			catch (Exception e)
			{
				Logger.Error("Core.Importer.ImportGpx", "importer.doImport => " + File.getAbsolutePath(), e);
				e.printStackTrace();
			}
		}

		if (FileList.length == 0)
		{
			ip.ProgressInkrement("ImportGPX", "", true);
		}

		importHandler.GPXFilenameUpdateCacheCount();

		// Indexierte CacheInfos zur�ck schreiben
		CacheInfoList.writeListToDB();
		CacheInfoList.dispose();

	}

	public void importGcVote(String whereClause, ImporterProgress ip)
	{

		GCVoteDAO gcVoteDAO = new GCVoteDAO();

		ArrayList<GCVoteCacheInfo> pendingVotes = gcVoteDAO.getPendingGCVotes();

		ip.setJobMax("sendGcVote", pendingVotes.size());
		int i = 0;

		for (GCVoteCacheInfo info : pendingVotes)
		{

			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e2)
			{
				return; // Thread Canceld
			}

			i++;

			ip.ProgressInkrement("sendGcVote", "Sending Votes (" + String.valueOf(i) + " / " + String.valueOf(pendingVotes.size()) + ")",
					false);

			Boolean ret = GCVote.SendVotes(Config.settings.GcLogin.getValue(), Config.settings.GcVotePassword.getValue(), info.Vote,
					info.URL, info.GcCode);

			if (ret)
			{
				gcVoteDAO.updatePendingVote(info.Id);
			}
		}

		if (pendingVotes.size() == 0)
		{
			ip.ProgressInkrement("sendGcVote", "No Votes to send.", true);
		}

		Integer count = gcVoteDAO.getCacheCountToGetVotesFor(whereClause);

		ip.setJobMax("importGcVote", count);

		int packageSize = 100;
		int offset = 0;
		int failCount = 0;
		i = 0;

		while (offset < count)
		{
			ArrayList<GCVoteCacheInfo> workpackage = gcVoteDAO.getGCVotePackage(whereClause, packageSize, i);
			ArrayList<String> requests = new ArrayList<String>();
			HashMap<String, Boolean> resetVote = new HashMap<String, Boolean>();
			HashMap<String, Long> idLookup = new HashMap<String, Long>();

			for (GCVoteCacheInfo info : workpackage)
			{
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e2)
				{
					return; // Thread Canceld
				}

				if (!info.GcCode.toLowerCase().startsWith("gc"))
				{
					ip.ProgressInkrement("importGcVote", "Not a GC.com Cache", false);
					continue;
				}

				requests.add(info.GcCode);
				resetVote.put(info.GcCode, !info.VotePending);
				idLookup.put(info.GcCode, info.Id);
			}

			ArrayList<RatingData> ratingData = GCVote.GetRating(Config.settings.GcLogin.getValue(),
					Config.settings.GcVotePassword.getValue(), requests);

			if (ratingData == null)
			{
				failCount += packageSize;
				ip.ProgressInkrement("importGcVote", "Query failed...", false);
			}
			else
			{
				for (RatingData data : ratingData)
				{
					if (idLookup.containsKey(data.Waypoint))
					{
						if (resetVote.containsKey(data.Waypoint))
						{
							gcVoteDAO.updateRatingAndVote(idLookup.get(data.Waypoint), data.Rating, data.Vote);
						}
						else
						{
							gcVoteDAO.updateRating(idLookup.get(data.Waypoint), data.Rating);
						}
					}

					i++;

					ip.ProgressInkrement("importGcVote",
							"Writing Ratings (" + String.valueOf(i + failCount) + " / " + String.valueOf(count) + ")", false);
				}

			}

			offset += packageSize;

		}

		if (count == 0)
		{
			ip.ProgressInkrement("importGcVote", "", true);
		}

	}

	public void importImages(ImporterProgress ip) // wir brachen kein delay mehr
	{
		CacheDAO CacheDao = new CacheDAO();

		// Index DB
		CacheInfoList.IndexDB();

		// get all GcCodes from Listing changed caches without Typ==4 (ErthCache)
		ArrayList<String> gcCodes = CacheDao.getGcCodesFromMustLoadImages();

		// refresch all Image Url�s
		ip.setJobMax("importImageUrls", gcCodes.size());
		int counter = 0;
		for (String gccode : gcCodes)
		{
			importApiImages(gccode, CacheInfoList.getIDfromGcCode(gccode));
			ip.ProgressInkrement("importImageUrls",
					"get Image Url�s for " + gccode + " (" + String.valueOf(counter++) + " / " + String.valueOf(gcCodes.size()) + ")",
					false);
		}

		ImageDAO imageDAO = new ImageDAO();

		// Die Where Clusel sorgt df�r, dass nur die Anzahl der zu ladenden Bilder zur�ck gegeben wird.
		// Da keine Bilder von ErthCaches geladen werden, wird hier auch der Typ 4 ausgelassen.
		String where = " Type<>4 and (ImagesUpdated=0 or DescriptionImagesUpdated=0)";

		Integer count = imageDAO.getImageCount(where);

		ip.setJobMax("importImages", count);

		if (count == 0)
		{
			ip.ProgressInkrement("importImages", "", true);
			return;
		}

		int i = 0;

		for (String gccode : gcCodes)
		{
			Boolean downloadedImage = false;
			ArrayList<String> imageURLs = imageDAO.getImageURLsForCache(gccode);

			boolean downloadFaild = false;

			for (String url : imageURLs)
			{
				try
				{
					Thread.sleep(5);
				}
				catch (InterruptedException e2)
				{
					return; // Thread Canceld
				}
				String localFile = DescriptionImageGrabber.BuildImageFilename(gccode, URI.create(url));

				if (!FileIO.FileExists(localFile))
				{
					downloadedImage = true;
					if (downloadFaild || !DescriptionImageGrabber.Download(url, localFile))
					{
						downloadFaild = true;
					}
				}

				i++;

				ip.ProgressInkrement("importImages",
						"Importing Images for " + gccode + " (" + String.valueOf(i) + " / " + String.valueOf(count) + ")", false);
			}

			if (downloadedImage)
			{
				ip.ProgressInkrement("importImages",
						"Importing Images for " + gccode + " (" + String.valueOf(i) + " / " + String.valueOf(count) + ")", false);

			}
			if (!downloadFaild)
			{
				// set DescriptionImagesUpdated and ImagesUpdated
				CacheInfoList.setImageUpdated(gccode);
			}
		}

		// Write CacheInfoList back
		CacheInfoList.writeListToDB();
		CacheInfoList.dispose();
	}

	public void importImagesNew(ImporterProgress ip)
	{
		// Import images in WinCB style
		String where = GlobalCore.LastFilter.getSqlWhere();

		// refresch all Image Url�s

		String sql = "select Id, Description, Name, GcCode, Url, ImagesUpdated, DescriptionImagesUpdated from Caches";
		if (where.length() > 0) sql += " where " + where;
		CoreCursor reader = Database.Data.rawQuery(sql, null);

		int cnt = -1;
		int numCaches = reader.getCount();
		ip.setJobMax("importImages", numCaches);

		if (reader.getCount() > 0)
		{
			reader.moveToFirst();
			while (reader.isAfterLast() == false)
			{
				cnt++;
				long id = reader.getLong(0);
				String name = reader.getString(2);
				String gcCode = reader.getString(3);

				ip.ProgressInkrement("importImages",
						"Importing Images for " + gcCode + " (" + String.valueOf(cnt) + " / " + String.valueOf(numCaches) + ")", false);

				boolean additionalImagesUpdated = false;
				boolean descriptionImagesUpdated = false;

				if (!reader.isNull(5))
				{
					additionalImagesUpdated = reader.getInt(5) != 0;
				}
				if (!reader.isNull(6))
				{
					descriptionImagesUpdated = reader.getInt(6) != 0;
				}

				String description = reader.getString(1);
				String uri = reader.getString(4);

				importImagesForCacheNew(ip, descriptionImagesUpdated, additionalImagesUpdated, id, gcCode, name, description, uri);
				reader.moveToNext();
			}
		}

		reader.close();
	}

	/**
	 * Importiert alle Spoiler Images f�r einen Cache (�ber die API-Funktion)
	 * 
	 * @param ip
	 * @param id
	 * @param gcCode
	 * @param name
	 */
	public void importSpoilerForCacheNew(ImporterProgress ip, Cache cache)
	{
		importImagesForCacheNew(ip, true, false, cache.Id, cache.GcCode, cache.Name, "", "");
	}

	private void importImagesForCacheNew(ImporterProgress ip, boolean descriptionImagesUpdated, boolean additionalImagesUpdated, long id,
			String gcCode, String name, String description, String uri)
	{
		boolean dbUpdate = false;

		if (!descriptionImagesUpdated)
		{
			descriptionImagesUpdated = CheckLocalImages(Config.settings.DescriptionImageFolder.getValue(), gcCode);

			if (descriptionImagesUpdated)
			{
				dbUpdate = true;
			}
		}
		if (!additionalImagesUpdated)
		{
			additionalImagesUpdated = CheckLocalImages(Config.settings.SpoilerFolder.getValue(), gcCode);

			if (additionalImagesUpdated)
			{
				dbUpdate = true;
			}
		}
		if (dbUpdate)
		{
			Parameters args = new Parameters();
			args.put("ImagesUpdated", additionalImagesUpdated);
			args.put("DescriptionImagesUpdated", descriptionImagesUpdated);
			long ret = Database.Data.update("Caches", args, "Id = ?", new String[]
				{ String.valueOf(id) });
		}

		DescriptionImageGrabber.GrabImagesSelectedByCache(ip, descriptionImagesUpdated, additionalImagesUpdated, id, gcCode, name,
				description, uri);
	}

	private boolean CheckLocalImages(String path, final String GcCode)
	{
		boolean retval = true;

		String imagePath = path + "/" + GcCode.substring(0, 4);
		boolean imagePathDirExists = FileIO.DirectoryExists(imagePath);

		if (imagePathDirExists)
		{
			File dir = new File(imagePath);
			FilenameFilter filter = new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String filename)
				{

					filename = filename.toLowerCase();
					if (filename.indexOf(GcCode.toLowerCase()) == 0)
					{
						return true;
					}
					return false;
				}
			};
			String[] files = dir.list(filter);

			if (files.length > 0)
			{
				for (String file : files)
				{
					if (file.endsWith(".1st") || file.endsWith(".changed"))
					{
						if (file.endsWith(".changed"))
						{
							File f = new File(file);
							try
							{
								f.delete();
							}
							catch (Exception ex)
							{
							}
						}
						retval = false;
					}
				}
			}
			else
			{
				retval = false;
			}
		}
		else
		{
			retval = false;
		}

		return retval;
	}

	private void importApiImages(String GcCode, long ID)
	{

		ImportHandler importHandler = new ImportHandler();
		LinkedList<String> allImages = new LinkedList<String>();
		ArrayList<String> apiImages = new ArrayList<String>();
		GroundspeakAPI.getImagesForGeocache(Config.GetAccessToken(true), GcCode, apiImages);
		for (String image : apiImages)
		{
			if (image.contains("/log/")) continue; // do not import log-images
			if (!allImages.contains(image)) allImages.add(image);
		}
		while (allImages != null && allImages.size() > 0)
		{
			String url;
			url = allImages.poll();

			ImageEntry image = new ImageEntry();

			image.CacheId = ID;
			image.GcCode = GcCode;
			image.Name = url.substring(url.lastIndexOf("/") + 1);
			image.Description = "";
			image.ImageUrl = url;
			image.IsCacheImage = true;

			importHandler.handleImage(image, true);

		}
	}

	public void importMaps()
	{
		ProgresssChangedEventList.Call("import Map", "", 0);

	}

	public void importMail()
	{
		ProgresssChangedEventList.Call("import from Mail", "", 0);

	}

	private File[] GetFilesToLoad(String directoryPath)
	{
		ArrayList<File> files = new ArrayList<File>();

		File file = new File(directoryPath);
		if (file.isFile())
		{
			files.add(file);
		}
		else
		{
			FileIO.DirectoryExists(directoryPath);
			files = FileIO.recursiveDirectoryReader(new File(directoryPath), files);
		}

		File[] fileArray = files.toArray(new File[files.size()]);

		Arrays.sort(fileArray, new Comparator<File>()
		{
			public int compare(File f1, File f2)
			{

				if (f1.getName().equalsIgnoreCase(f2.getName().replace(".gpx", "") + "-wpts.gpx"))
				{
					return 1;
				}
				else if (f2.getName().equalsIgnoreCase(f1.getName().replace(".gpx", "") + "-wpts.gpx"))
				{
					return -1;
				}
				else if (f1.lastModified() > f2.lastModified())
				{
					return 1;
				}

				else if (f1.lastModified() < f2.lastModified())
				{
					return -1;
				}

				else
				{
					return f1.getAbsolutePath().compareToIgnoreCase(f2.getAbsolutePath()) * -1;
				}
			}
		});

		return fileArray;
	}

	private static CancelWaitDialog WD;

	public static void ImportSpoiler()
	{

		WD = CancelWaitDialog.ShowWait(GlobalCore.Translations.Get("chkApiState"), new IcancelListner()
		{

			@Override
			public void isCanceld()
			{
				// TODO Handle Cancel

			}
		}, new Runnable()
		{

			@Override
			public void run()
			{
				Importer importer = new Importer();
				ImporterProgress ip = new ImporterProgress();
				importer.importSpoilerForCacheNew(ip, GlobalCore.getSelectedCache());
				WD.close();
			}
		});
	}

}
