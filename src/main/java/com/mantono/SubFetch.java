package com.mantono;

import java.io.*;
import java.nio.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.filechooser.*;
import javax.swing.*;

public class SubFetch
{
	private File videoFile;
	private String hash;
	private final int maxDataTransfer = 240 * 1024;
	private final int bufferSize = 2048;
	private final String subtitleBaseURL = "http://www.opensubtitles.org";
	private final String searchKeyword = "/en/subtitleserve/sub/";
	private final String baseURL = "http://www.opensubtitles.org/en/search/sublanguageid-";
	private final String downloadDirectory = System.getProperty("user.dir");
	private final Pattern searchPattern = Pattern.compile(searchKeyword + "\\d+");
	private final File htmlFile = new File(downloadDirectory + "/subtitledata.html");
	private final static FileNameExtensionFilter saveFileFilter = new FileNameExtensionFilter("Video files", "mkv", "flv", "wmv", "avi", "iso", "mp4", "mov", "mpeg", "rar");
	private File subtitleFile;
	private String destinationPath;
	private String languageTag = "eng";
	private URL webpage;
	private URL downloadURL;

	SubFetch(File input)
	{
		this(input, "eng");
	}

	SubFetch(File input, String languageTag)
	{
		this.videoFile = input;
		this.languageTag = languageTag;
		this.hash = getHash(input);

		String[] pathArray = input.getAbsolutePath().split("/");
		destinationPath = "/";
		for(int i = 0; i < pathArray.length-1; i++)
			destinationPath += pathArray[i] + "/";

		loadWebPage();
		int unpackedFiles = extractZipFile(getZipFile());
		if(unpackedFiles < 1)
		{
			System.err.println("Error: No files were extracted");
			System.exit(1);
		}
		else if(unpackedFiles < 3)
			renameSubFile();
		System.exit(0);
	}

	private String getHash(File file)
	{
		waitForFileCompletion(file);
		try
		{
			return OpenSubtitlesHasher.computeHash(file);
		}
		catch(IOException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		return null;
	}

	private void waitForFileCompletion(File file)
	{
		long fileSize1 = -1;
		long fileSize2 = 0;
		int n = 0;
		while(fileSize1 != fileSize2)
		{
			fileSize1 = file.length();
			try {
					Thread.sleep(1000+(50*n++));
				}
			catch(InterruptedException exception)
				{
					Thread.currentThread().interrupt();
				}
			fileSize2 = file.length();
		}
	}

	private void loadWebPage()
	{
		System.out.println("Retrieving webpage...");
		try
		{
			webpage = new URL(baseURL + this.languageTag + "/movieHash-" + hash);
			ReadableByteChannel openSubtitles = Channels.newChannel(webpage.openStream());
			FileOutputStream htmlData = new FileOutputStream(downloadDirectory + "/subtitledata.html");
			htmlData.getChannel().transferFrom(openSubtitles, 0, this.maxDataTransfer);
			return;
		}
		catch(UnknownHostException exception)
		{
			System.err.println(exception + "\nInvalid hostname or missing internet connection.");
		}
		catch(MalformedURLException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		catch(IOException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
			System.exit(1);
	}

	private String findSubtitlePath(File htmlFile)
	{
		try
		{
			Scanner file = new Scanner(htmlFile);
			while(file.hasNextLine())
			{
				String line = file.nextLine();
				if(line.contains(this.searchKeyword))
					return line;
			}
		}
		catch(FileNotFoundException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		finally
		{
			htmlFile.deleteOnExit();
		}
		return null;
	}

	private URL buildSubtitleURL(String dataLine)
	{
		try
		{
			String[] row = dataLine.split(this.searchKeyword);
			String[] field = row[1].split("\"");

			return new URL(this.subtitleBaseURL + this.searchKeyword + field[0]);
		}
		catch(MalformedURLException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		catch(NullPointerException exception)
		{
			System.err.println("The combination of language " + this.languageTag + " and the selected movie did not yield any hit for subtitles.");
			System.exit(1);
		}

		return null;
	}

	private File downloadSubtitleFile()
	{
		System.out.println("Downloading " + downloadURL + "...");
		try
		{

			ReadableByteChannel zipFile = Channels.newChannel(this.downloadURL.openStream());
			FileOutputStream zipData = new FileOutputStream(downloadDirectory + "/" + this.hash + ".zip");
			zipData.getChannel().transferFrom(zipFile, 0, this.maxDataTransfer);

			System.out.println("Downloaded file " + downloadDirectory + "/" + this.hash + ".zip");
			return new File(downloadDirectory + "/" + this.hash + ".zip");
		}
		catch(FileNotFoundException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		catch(IOException exception)
		{
			System.err.println(exception + exception.getMessage());
		}
		return null;
	}

	private File getZipFile()
	{
		this.downloadURL = buildSubtitleURL(findSubtitlePath(htmlFile));
		return downloadSubtitleFile();
	}

	private int extractZipFile(File file)
	{
		System.out.println("Unpacking file " + file + "...");
		Zip zipFile = new Zip(file, destinationPath);
		file.deleteOnExit();
		return zipFile.unzip();
	}

	private void renameSubFile()
	{
		System.out.println(destinationPath);
		subtitleFile = getLastCreatedSrtFile(destinationPath);
		String[] videoNameArray = videoFile.getName().split("\\.");
		String newSubFileName = "";
		for(int i = 0; i < videoNameArray.length-1; i++)
			newSubFileName += videoNameArray[i] + ".";
		newSubFileName += "srt";
		subtitleFile.renameTo(new File(destinationPath + newSubFileName));
	}

	private File getLastCreatedSrtFile(String directory)
	{
		File path = new File(directory);
		System.out.println(path);
		TreeSet<File> filesInDirectory = new TreeSet<File>(new CompareModTime());
		File[] files = path.listFiles();
		for(File f : files)
		{
			if(hasFileExtension(f, "srt"))
			{
				filesInDirectory.add(f);
			}
		}
		return filesInDirectory.last();
	}

	public static boolean hasFileExtension(File file, String extension)
	{
		extension = extension.toLowerCase();
		String fileName = file.getName().toLowerCase();
		String[] row = fileName.split("\\.");
		String fileExtension = row[row.length-1];
		return (fileExtension.equals(extension));
	}

	public static void main(String[] args)
	{
		if(args.length == 0)
		{
			JFileChooser loadFileDialog = new JFileChooser();
			loadFileDialog.setFileFilter(saveFileFilter);
			JFrame parentFrame = new JFrame();
			parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			int selectedOption = loadFileDialog.showOpenDialog(parentFrame);
			if (selectedOption != JFileChooser.APPROVE_OPTION)
				System.exit(0);
			new SubFetch(loadFileDialog.getSelectedFile());
			System.exit(0);
		}
		else if(args.length == 1)
			new SubFetch(new File(args[0]));
		else if(args.length == 2)
			new SubFetch(new File(args[0]), args[1]);
		else
		{
			System.out.println("Too many arguments.\nUse syntax SubFetch.jar FILE LANGUAGE-ID \nIf LANGUAGE-ID is omitted eng (english) will be chosen");
			System.exit(1);
		}
		System.exit(0);
	}
}

class CompareModTime implements Comparator<File>
{
	public int compare(File f1, File f2)
    {
        return (int) (f1.lastModified() - f2.lastModified());
    }
}
