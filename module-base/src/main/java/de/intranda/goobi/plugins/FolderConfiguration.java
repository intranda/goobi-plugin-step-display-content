package de.intranda.goobi.plugins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FolderConfiguration {

    // label
    @NonNull
    private String foldername;

    // actual path
    @NonNull
    private Path path;

    // regular expression to filter the files within the folder. e.g. to show only pdf files or different image formats
    @NonNull
    private String filter;

    // all files in the folder that match the filter
    private List<Path> files = new ArrayList<>();

    public void addFile(Path file) {
        files.add(file);
    }
}
