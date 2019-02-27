package com.mantono;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

public class FileManager
{
	private static String getLastCreatedSrtFile(String directory)
	{
		File path = new File(directory);
		Set<File> filesInDirectory = new TreeSet<File>();
		File[] files = path.listFiles();
		for(File f : files)
		{
			if(hasFileExtension(f, "srt"))
			{
				filesInDirectory.add(f);
				System.out.println(f);
			}
		}
		return "jkjki";
	}
	
	private static boolean hasFileExtension(File file, String extension)
	{
		extension = extension.toLowerCase();
		String fileName = file.getName();
		fileName = fileName.toLowerCase();
		String[] row = fileName.split("\\.");
		String fileExtension = row[row.length-1];
		return (fileExtension.equals(extension));
	}
	
	public static void main(String args[])
	{
		if(args.length == 1)
			getLastCreatedSrtFile(args[0]);
		else
			getLastCreatedSrtFile(System.getProperty("user.dir"));
	}	
}
