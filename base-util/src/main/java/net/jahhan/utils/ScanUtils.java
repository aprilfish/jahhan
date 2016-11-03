package net.jahhan.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanUtils {

	static Logger logger = LoggerFactory.getLogger(ScanUtils.class);

	public static Set<String> findResourceByPathRule(String rule, String... parentPaths) {
		Set<String> result = new LinkedHashSet<>();
		try {
			ClassLoader cl = ScanUtils.class.getClassLoader();

			for (String parentPath : parentPaths) {
				Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(parentPath)
						: ClassLoader.getSystemResources(parentPath));
				while (resourceUrls.hasMoreElements()) {
					URL url = resourceUrls.nextElement();
					Set<String> fileNameSet = new LinkedHashSet<>();
					Pattern pattern = Pattern.compile(rule);
					if (url.toString().startsWith("jar")) {
						fileNameSet = findPathMatchingJarResources(url, pattern);
					} else if (url.toString().startsWith("file")) {
						fileNameSet = findPathMatchingFiles(url, pattern);
					}

					for (String fileName : fileNameSet) {
						result.add(parentPath + fileName);
					}
				}
			}
		} catch (Exception e) {
			logger.error("scan resource", e);
		}
		return result;
	}

	private static Set<String> findPathMatchingFiles(URL url, Pattern pattern) {
		Set<String> result = new LinkedHashSet<>(8);

		File folder = new File(url.getFile());
		String[] fileNames = folder.list();
		for (String fileName : fileNames) {
			if (pattern.matcher(fileName).matches()) {
				result.add(fileName);
			}
		}
		return result;
	}

	private static Set<String> findPathMatchingJarResources(URL url, Pattern pattern) throws IOException {
		URLConnection con = url.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean newJarFile = false;

		if (con instanceof JarURLConnection) {
			JarURLConnection jarCon = (JarURLConnection) con;
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
		} else {
			String urlFile = url.getFile();
			try {
				int separatorIndex = urlFile.indexOf("!/");
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + "!/".length());
					jarFile = new JarFile(jarFileUrl);
				} else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				newJarFile = true;
			} catch (ZipException ex) {
				ex.printStackTrace();
				return Collections.emptySet();
			}
		}

		try {
			if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				rootEntryPath = rootEntryPath + "/";
			}
			
			Set<String> result = new LinkedHashSet<>(8);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());

					if (pattern.matcher(relativePath).matches() && relativePath.indexOf("/")==-1) {
						result.add(relativePath);
					}
				}
			}
			return result;
		} finally {
			if (newJarFile) {
				jarFile.close();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static Set<Class> findClassInPackage(String rule, Package... pkgs) {
		Set<String> pathSet = new HashSet<>();
		for (Package pkg : pkgs) {
			pathSet.add(pkg.getName().replace('.', '/') + "/");
		}

		String[] paths = pathSet.toArray(new String[pathSet.size()]);

		return findClassInPath(rule, paths);
	}

	@SuppressWarnings("rawtypes")
	public static Set<Class> findClassInPath(String rule, String... paths) {
		Set<Class> result = new HashSet<>();
		String classNameRule = rule + "\\.class";

		Set<String> classPaths = ScanUtils.findResourceByPathRule(classNameRule, paths);
		for (String classPath : classPaths) {
			String className = classPath.substring(0, classPath.length() - 6).replace('/', '.');
			try {
				result.add(Class.forName(className));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}
}
