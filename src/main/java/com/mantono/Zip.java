import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Zip
{
	private File zipFile;
	private final int BUFFER = 2048;
	private byte dataBuffer[] = new byte[BUFFER];
	private String destinationPath;
	
	public Zip(File zipFile)
	{
		this(zipFile, zipFile.getAbsolutePath());
	}
	
	public Zip(String zipFile)
	{
		this(new File(zipFile), System.getProperty("user.dir"));
	}
	
	public Zip(String zipFile, String destinationPath)
	{
		this.zipFile = new File(zipFile);
		this.destinationPath = destinationPath;
	}
	
	public Zip(File zipFile, String destinationPath)
	{
		this.zipFile = zipFile;
		this.destinationPath = destinationPath;
	}
	
	public int unzip()
	{
		int unpackedFiles = 0;
		try
		{
			FileInputStream fileInput = new FileInputStream(zipFile);
			BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
			ZipInputStream zipInput = new ZipInputStream(bufferedInput);
			ZipEntry zipContent;
			while((zipContent = zipInput.getNextEntry()) != null)
			{
				int remainingData;
				File nextFile = new File(destinationPath + zipContent.getName());
				System.out.println("Creating file " + nextFile);
				nextFile.createNewFile();
				System.out.println("\t... [" + (unpackedFiles + 1) + "] " + zipContent.getName());
				FileOutputStream fileOutput = new FileOutputStream(nextFile);
				BufferedOutputStream dataOutput = new BufferedOutputStream(fileOutput, BUFFER);
				System.out.println("Writing file " + zipContent.getName() + " to " + destinationPath);
				while ((remainingData = zipInput.read(dataBuffer, 0, BUFFER)) != -1)
				{
					dataOutput.write(dataBuffer, 0, remainingData);
				}
				unpackedFiles++;
			}
			fileInput.close();
			bufferedInput.close();
			zipInput.close();
		}
		catch(FileNotFoundException exception)
		{
			System.err.println("The file could not be found.\n" + exception.getMessage() + "\n" + exception.getCause());
		}
		catch(IOException exception)
		{
			System.err.println("General I/O exception: " + exception.getMessage());
		}
		return unpackedFiles;
		
	}
	
	public static void main(String args[])
	{
		if(args.length == 1)
		{
			Zip file = new Zip(args[0]);
			file.unzip();
		}
		else if(args.length == 2)
		{
			Zip file = new Zip((args[0]), args[1]);
			file.unzip();
		}
	}
	//public File zip(File file)
}
