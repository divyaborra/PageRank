import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PageRank
{

	static Map<String, ArrayList<String>> outLinksMap = new TreeMap<>();
	static Map<String, Integer> outlinksIndexMap = new TreeMap<>();

	static Map<String, Integer> inlinksMap = new HashMap<>();
	static Map<Integer, ArrayList<String>> invertedInlinksMap = new TreeMap<Integer, ArrayList<String>>(
			Collections.reverseOrder());

	static double[] currPageRank = null;
	static double[] betterPageRank = null;

	static double lambda;
	static double tau;

	public static void main(String[] args)
	{
		String inputfilename = args[0];
		lambda = Double.parseDouble(args[1]);
		tau = Double.parseDouble(args[2]);
		String pageRanksFile = args[3];
		String inlinksFile = args[4];

		doWork(inputfilename, pageRanksFile, inlinksFile);
	}

	private static void doWork(String inFile, String pageRanksFile, String inlinksFile)
	{

		readGraph(inFile);
		initalizeCurrPageRank();
		updatePageranks();
		Map<Double, String> pageToRankMap = getTop50();
		writePageRankToFile(pageRanksFile, inlinksFile, pageToRankMap);

	}

	public static void readGraph(String inputfile)
	{

		Map<String, ArrayList<String>> oldOutlinks = new HashMap<>();
		List<String> allLines = new ArrayList<String>();
		try
		{
			allLines = Files.readAllLines(Paths.get(inputfile));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String line : allLines)
		{
			String[] sourceAndTarget = line.split("\\t");
			String source = sourceAndTarget[0];
			String target = sourceAndTarget[1];

			// handling source
			if (oldOutlinks.containsKey(source))
			{
				ArrayList<String> outlinksList = oldOutlinks.get(source);
				outlinksList.add(target);
			}
			else
			{
				ArrayList<String> outlinksList = new ArrayList<String>();
				outlinksList.add(target);
				oldOutlinks.put(source, outlinksList);
			}

			// handling target - adding to map
			if (!oldOutlinks.containsKey(target))
			{
				ArrayList<String> emptyList = new ArrayList<String>();
				oldOutlinks.put(target, emptyList);
			}

			// handling target - counting inlinks
			if (inlinksMap.containsKey(target))
			{
				int c = inlinksMap.get(target);
				inlinksMap.put(target, c + 1);
			}
			else
			{
				inlinksMap.put(target, 1);
			}
		}
		outLinksMap.putAll(oldOutlinks);

		// create index map
		int count = 0;
		for (String page : outLinksMap.keySet())
		{
			outlinksIndexMap.put(page, count);
			count++;
		}
	}

	public static void initalizeCurrPageRank()
	{
		int numPages = outLinksMap.size();
		currPageRank = new double[numPages];
		double dnumPages = numPages;
		double val = (1 / dnumPages);

		Arrays.fill(currPageRank, val);
	}

	public static void updatePageranks()
	{
		int numPages = outLinksMap.size();
		double dval = lambda / numPages;
		double dval_contra = (1 - lambda) / numPages;
		betterPageRank = new double[numPages];
		int count = 0;
		while (!hasConverged())
		{
			count++;
			Arrays.fill(betterPageRank, dval);

			int numPagesWithNoTargets = 0;
			for (String page : outLinksMap.keySet())
			{
				ArrayList<String> targetPages = outLinksMap.get(page);
				if (targetPages.isEmpty())
				{
					numPagesWithNoTargets++;
				}
				else
				{
					double dval_contra_x = (1 - lambda) / targetPages.size();
					for (String q : targetPages)
					{
						int targetPageIndex = outlinksIndexMap.get(q);
						betterPageRank[targetPageIndex] += dval_contra_x * currPageRank[targetPageIndex];
					}
				}
			}
			// Update for numPagesWithNoTargets times
			int innermapcounter = 0;
			for (String q : outLinksMap.keySet())
			{
				betterPageRank[innermapcounter] += numPagesWithNoTargets
						* (dval_contra * currPageRank[innermapcounter]);
				innermapcounter++;
			}

			System.arraycopy(betterPageRank, 0, currPageRank, 0, betterPageRank.length);
		}

	}

	public static boolean hasConverged()
	{
		double sum = 0;
		for (int i = 0; i < currPageRank.length; i++)
		{
			double diff = Math.abs(currPageRank[i] - betterPageRank[i]);
			sum += diff;
		}

		if (sum < tau)
			return true;

		return false;
	}

	public static Map<Double, String> getTop50()
	{
		// get top 50 pageranks
		// create new map with page-to-double
		Map<Double, String> pageToRankMap = new TreeMap<Double, String>(Collections.reverseOrder());

		int count = 0;
		for (String str : outLinksMap.keySet())
		{
			pageToRankMap.put(currPageRank[count], str);
			count++;
		}

		// get top 50 inlinks
		Map<Integer, ArrayList<String>> invertedmap = invertMap(inlinksMap);

		return pageToRankMap;
	}

	public static void writePageRankToFile(String pageRanksFile, String inlinksFile, Map<Double, String> pageToRankMap)
	{
		writeInlinksToFile(inlinksFile);
		FileWriter writer;
		try
		{
			writer = new FileWriter(pageRanksFile);

			int count = 0;
			for (Double d : pageToRankMap.keySet())
			{
				if (count < 50)
				{
					writer.write(pageToRankMap.get(d) + " " + (count + 1) + " " + d + "\n");
					count++;
				}
			}

			writer.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writeInlinksToFile(String outFile)
	{
		FileWriter writerInlinks = null;
		try
		{
			writerInlinks = new FileWriter(outFile);

			int countInlinks = 1;
			boolean countReached = false;
			for (int num : invertedInlinksMap.keySet())
			{
				ArrayList<String> list = invertedInlinksMap.get(num);
				for (String s : list)
				{
					writerInlinks.write(s + " " + countInlinks + " " + num + "\n");
					countInlinks++;

					if (countInlinks == 51)
					{
						countReached = true;
						break;
					}
				}
				if (countReached)
					break;
			}

			writerInlinks.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Map<Integer, ArrayList<String>> invertMap(Map<String, Integer> inlinks)
	{
		Map<Integer, ArrayList<String>> swappedmap = new HashMap<>();

		for (String key : inlinks.keySet())
		{
			int value = inlinks.get(key);

			if (!swappedmap.containsKey(value))
			{
				ArrayList<String> newlist = new ArrayList();
				newlist.add(key);
				swappedmap.put(value, newlist);
			}
			else
			{
				ArrayList<String> list = swappedmap.get(value);
				list.add(key);
			}
		}

		invertedInlinksMap.putAll(swappedmap);
		return invertedInlinksMap;
	}

}
