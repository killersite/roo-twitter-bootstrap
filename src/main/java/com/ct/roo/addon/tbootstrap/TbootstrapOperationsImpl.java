package com.ct.roo.addon.tbootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link TbootstrapOperations} interface.
 *
 * @since 1.1.1
 */
@Component
@Service
public class TbootstrapOperationsImpl extends AbstractOperations implements TbootstrapOperations {
    
    /**
     * Get hold of a JDK Logger
     */
    private Logger log = Logger.getLogger(getClass().getName());

    private static final char SEPARATOR = File.separatorChar;

    /**
     * Get a reference to the FileManager from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     * 
     * Using the Roo file manager instead if java.io.File gives you automatic rollback in case
     * an Exception is thrown.
     */
//    @Reference private FileManager fileManager;
    
    /**
     * Get a reference to the ProjectOperations from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     */
    @Reference private ProjectOperations projectOperations;

    /** {@inheritDoc} */
	public boolean isInstallTwitterBootstrapAvailable() {
        log.info("isInstallTwitterBootstrapAvailable START");
        return isProjectAvailable() && isControllerAvailable();
	}

    private boolean isProjectAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }

    private boolean isControllerAvailable() {
        PathResolver pathResolver = projectOperations.getPathResolver();
        return fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views"))
                && !projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.JSF) 
                && fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "spring" + SEPARATOR + "webmvc-config.xml"))
		        && fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags"));
    }

    /** {@inheritDoc} */
    public String getProperty(String propertyName) {
        Validate.notBlank(propertyName, "Property name required");
        return System.getProperty(propertyName);
    }

    /** {@inheritDoc} */
	public void installTwitterBootstrap() {
        // Use PathResolver to get canonical resource names for a given artifact
        PathResolver pathResolver = projectOperations.getPathResolver();

        // Install 
		copyDirectoryContents("images/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "images"), true);
		copyDirectoryContents("styles/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "styles"), true);
		copyDirectoryContents("WEB-INF/layouts/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "layouts"), true);
		copyDirectoryContents("WEB-INF/views/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "views"), true);
		copyDirectoryContents("WEB-INF/tags/form/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "form"), true);
		copyDirectoryContents("WEB-INF/tags/form/fields/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "form" + SEPARATOR + "fields"), true);
		copyDirectoryContents("WEB-INF/tags/menu/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "menu"), true);
		copyDirectoryContents("WEB-INF/tags/util/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "util"), true);
		
		// update menu
		String menuFileLocation = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "views" + SEPARATOR + "menu.jspx");
		final Document menu = XmlUtils.readXml(fileManager.getInputStream(menuFileLocation));
		
		// add class attribute
        if (XmlUtils.findFirstElement("//div[@id='menu']", menu.getDocumentElement()) != null) {
            final Element span = XmlUtils.findRequiredElement("//div[@id='menu']", menu.getDocumentElement());
            span.setAttribute("class", "well");
            fileManager.createOrUpdateTextFileIfRequired(menuFileLocation, XmlUtils.nodeToString(menu), false);
        }

        // TODO update layouts.xml
		String layoutsFileLocation = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "layouts" + SEPARATOR + "layouts.xml");
		final Document layouts = XmlUtils.readXml(fileManager.getInputStream(layoutsFileLocation));
		
		// TODO update footer
		
		// TODO update header
		
		// TODO put menu into Header instead 
		
		// TODO make use of the THEME facilities
		
	}

    /**
     * A private method which illustrates how to reference and manipulate resources
     * in the target project as well as the bundle classpath.
     * 
     * @param path
     * @param fileName
     */
    private void createOrReplaceFile(String path, String fileName) {
        String targetFile = path + SEPARATOR + fileName;
        
        // Use MutableFile in combination with FileManager to take advantage of Roo's transactional file handling which 
        // offers automatic rollback if an exception occurs
        MutableFile mutableFile = fileManager.exists(targetFile) ? fileManager.updateFile(targetFile) : fileManager.createFile(targetFile);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Use FileUtils to open an InputStream to a resource located in your bundle
            inputStream = FileUtils.getInputStream(getClass(), fileName);
            outputStream =  mutableFile.getOutputStream();
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

}