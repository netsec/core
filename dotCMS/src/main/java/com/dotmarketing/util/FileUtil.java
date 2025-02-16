package com.dotmarketing.util;

import com.dotcms.util.CloseUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;

public class FileUtil {

	private static Set<String> extensions = new HashSet<String>();

	/**
	 * This method takes a string of a filename or extension and maps it to a
	 * known .png file in the /html/image/icons/directory
	 * 
	 * @param x
	 *            is the filename or extension
	 * @return
	 */

	public static String getIconExtension(String x) {
		if(x==null){
			return "ukn";
		}
		x = x.toLowerCase();
		if (x.lastIndexOf(".") > -1) {
			x = x.substring(x.lastIndexOf(".")+1, x.length());
		}

		if (extensions.size() == 0) {
			synchronized (FileUtil.class) {
				if (extensions.size() == 0) {
					String path = com.liferay.util.FileUtil.getRealPath("/html/images/icons");

					String[] files = new File(path).list(new PNGFileNameFilter());
					for (String name : files) {
						name =name.toLowerCase();
						if (name.indexOf(".png") > -1)
							extensions.add(name.replace(".png", ""));
					}
				}
			}
		}
		// if known extension
		if (extensions.contains(x)) {
			return x;
		} else {
			return "ukn";
		}

	}

	/**
	 * This will return the full path to the file asset as a String
	 * 
	 * @param inode
	 * @param extenstion
	 * @return
	 */
	public static String getAbsoluteFileAssetPath(String inode, String extenstion) {
		String _inode = inode;
		String path = "";
		String realPath = Config.getStringProperty("ASSET_REAL_PATH");
		String assetPath = Config.getStringProperty("ASSET_PATH");
		path = java.io.File.separator + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode
				+ "." + extenstion;
		if (UtilMethods.isSet(realPath)) {
			return realPath + path;
		} else {
			return com.liferay.util.FileUtil.getRealPath(assetPath + path);
		}

	}

  final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
      24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

  /**
   * cleans filenames and allows unicode-  taken from
   * https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
   * @param badFileName
   * @return
   */
  public static String sanitizeFileName(final String badFileName) {
    StringBuilder cleanName = new StringBuilder();
    int len = badFileName.codePointCount(0, badFileName.length());
    for (int i = 0; i < len; i++) {
      final int c = badFileName.codePointAt(i);
      if (Arrays.binarySearch(illegalChars, c) < 0) {
        cleanName.appendCodePoint(c);
      }
    }
    final String cleanFileName = cleanName.toString();
    return (cleanFileName.length()>0) ? cleanFileName : RandomStringUtils.randomAlphabetic(10);
  }

	/**
	 * This will write the given InputStream to a new File in the given location
	 * 
	 * @param uploadedInputStream
	 * @param uploadedFileLocation
	 * @return
	 */
	public static void writeToFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {

        OutputStream out = null;
	    try {
			int read;
			byte[] bytes = new byte[1024];

			final File file = new File(uploadedFileLocation);
			out = Files.newOutputStream(file.toPath());
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
            out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            CloseUtils.closeQuietly(out);
        }

	}
	

	/**
	 * This method will figure out if the passed in path is relative meaning relative to the WAR and will
	 * use the Servlet.CONTEXT to find the real path or if the passed in path is Absolutely meaning absolute 
	 * to the file system.  It will return the full path or the file or directory
	 * @param path the passed in path. If it starts with a / or \ or [a-z]: then it is considered a full path
	 * @return
	 */
	public static String getAbsolutlePath(String path){
		if(RegEX.contains(path, "^[a-zA-Z]:|^/|^\\\\")){
			return path;
		}else{
			return com.liferay.util.FileUtil.getRealPath(path);
		}
	}

	/**
	 * Delete directories recursively including the initial directory, taking into account symbolic links
	 * @param path path to be file/directory to be deleted
	 * @throws IOException
	 */
	public static void deleteDir(String path) throws IOException{
		// initial directory
		Path directory = Paths.get(path);
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
		   @Override
		   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		       Files.delete(file);
		       return FileVisitResult.CONTINUE;
		   }
		   @Override
		   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		       Files.delete(dir);
		       return FileVisitResult.CONTINUE;
		   }
		});
	}

    public static FileFilter getOnlyFolderFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if ( pathname.isDirectory() ){
                    return true;
                } else {
                    return false;
                }
            }
        };
    }
}

final class PNGFileNameFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.indexOf(".png") > -1);
	}

}



