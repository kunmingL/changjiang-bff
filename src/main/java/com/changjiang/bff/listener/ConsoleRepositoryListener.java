package com.changjiang.bff.listener;

import com.changjiang.bff.core.ApiScanner;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.LoggerFactory;

public class ConsoleRepositoryListener extends AbstractRepositoryListener {
    @Override
    public void artifactResolved(RepositoryEvent event) {
        LoggerFactory.getLogger(ApiScanner.class).info("Artifact resolved: {}",
                event.getArtifact());
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        LoggerFactory.getLogger(ApiScanner.class).info("Downloading artifact: {}",
                event.getArtifact());
    }
}
