/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */
package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.VariableReplacer;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class DisplayContentStepPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = 3889317365724913608L;
    @Getter
    private String title = "intranda_step_displayContent";
    @Getter
    private Step step;

    private String returnPath;

    @Getter
    private transient List<FolderConfiguration> configuredFolder = new ArrayList<>();

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        Process process = step.getProzess();
        VariableReplacer replacer = new VariableReplacer(null, null, process, step);

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        for (HierarchicalConfiguration hc : myconfig.configurationsAt("/folder")) {
            // get foldername, get filter

            String foldername = hc.getString("@label");
            Path folderPath = Paths.get(replacer.replace(hc.getString("@path")));
            String filter = hc.getString("@filter", "");

            FolderConfiguration fc = new FolderConfiguration(foldername, folderPath, filter);
            configuredFolder.add(fc);
            // find all files in folder, use filter

            if (StorageProvider.getInstance().isDirectory(folderPath)) {
                List<Path> content = StorageProvider.getInstance().listFiles(folderPath.toString());
                for (Path file : content) {
                    if (StringUtils.isBlank(filter) || file.getFileName().toString().matches(filter)) {
                        fc.addFile(file);
                    }
                }
            }
        }
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.PART;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_displayContent.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null; //NOSONAR
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = true;
        // your logic goes here

        log.info("DisplayContent step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }

    /**
     * get the size of a file that is listed inside of the configured directory
     * 
     * @param file name of the file to get the size of
     * @return size as String in MB, GB or TB
     */
    public String getFileSize(String file) {
        String result = "-";
        try {
            long fileSize = StorageProvider.getInstance().getFileSize(Paths.get(file));
            result = FilesystemHelper.getFileSizeShort(fileSize);
        } catch (IOException e) {
            log.error(e);
        }
        return result;
    }

    public void downloadFile(String file) {
        Path f = Paths.get(file);
        try (InputStream in = StorageProvider.getInstance().newInputStream(f)) {
            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            ExternalContext ec = facesContext.getExternalContext();
            ec.responseReset();
            ec.setResponseContentType(NIOFileUtils.getMimeTypeFromFile(f));
            ec.setResponseHeader("Content-Disposition", "attachment; filename=" + f.getFileName().toString());
            ec.setResponseContentLength((int) StorageProvider.getInstance().getFileSize(f));

            IOUtils.copy(in, ec.getResponseOutputStream());

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
    }

}
