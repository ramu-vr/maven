/*
 * The MIT License
 *
 * Copyright (c) 2011, Nigel Magnay / NiRiMa
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nirima.jenkins.repo.build;

import hudson.maven.MavenBuild;
import hudson.maven.reporters.MavenArtifact;
import hudson.model.Run;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a {@code maven-metadata.xml} file.
 */
public class MetadataRepositoryItem extends TextRepositoryItem {

    private static final String VFMT_FORMAT_PATTERN = "yyyyMMdd.HHmmss";
    private static final String UFMT_FORMAT_PATTERN = "yyyyMMddHHmmss";
    private Run<?,?> build;
    private String groupId, artifactId, version;
    private Map<MavenArtifact,ArtifactRepositoryItem> items = new HashMap<>();

    private static String formatDateVersion(Date date, int buildNo) {
        // we used to tack the build number on here, but that causes problems because the build
        // number seems to be always for the latest build, not the build that generated this
        // artifact; so we just use -1; it is essentially impossible that a project will be built
        // twice in the same millsecond, so there's no risk of collision
        return new SimpleDateFormat(VFMT_FORMAT_PATTERN).format(date) + "-" + buildNo;
    }

    public static String formatDateVersion(Run buildRun) {
        // Create a date/time stamp from a particular run.
        return formatDateVersion(buildRun.getTime(), buildRun.getNumber());
    }

    public MetadataRepositoryItem(Run<?,?> build, MavenArtifact artifact) {
        this.build      = build;
        this.groupId    = artifact.groupId;
        this.artifactId = artifact.artifactId;
        this.version    = artifact.version;
    }

    public String getPath() {
        return groupId.replace('.','/') + "/" + artifactId + "/" + version + "/" + getName();
    }

    public void addArtifact(MavenArtifact artifact, ArtifactRepositoryItem item) {
        this.items.put(artifact, item);
    }

    public String getName() {
        return "maven-metadata.xml";
    }

    public Date getLastModified() {
        long lastModified = 0L;
        for (ArtifactRepositoryItem item : items.values()) {
            lastModified = Math.max(lastModified, item.getLastModified().getTime());
        }
        return new Date(lastModified);
    }

    public String getDescription() {
        if( build instanceof MavenBuild ) {
            return "From Build #" + build.getNumber() + " of " + ((MavenBuild)build).getParentBuild().getParent()
                .getName();
    }
        else {
            return "From Build #" + build.getNumber() + " of " + build.getDisplayName();
        }
    }

    public String getContentType() {
        return "application/xml";
    }

    @Override
    protected String generateContent() {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buf.append("<metadata modelVersion=\"1.1.0\">\n");
        buf.append("  <groupId>" + groupId + "</groupId>\n");
        buf.append("  <artifactId>" + artifactId + "</artifactId>\n");
        buf.append("  <version>" + version + "</version>\n");
        buf.append("  <versioning>\n");
        buf.append("    <snapshotVersions>\n");

        // It is possible that "items" contains many entries for the same artifact; we
        // just want the latest.

        Map<String,Entry> entryToBuild = new HashMap<>();

        for (Map.Entry<MavenArtifact,ArtifactRepositoryItem> entry : items.entrySet()) {

            Entry e = new Entry(entry);
            String id = e.toString();

            if( entryToBuild.containsKey(id))
            {
                Entry current = entryToBuild.get(id);
                if( e.isNewerThan(current) )
                    entryToBuild.put(id, e);
            }
            else
            {
                entryToBuild.put(id, e);
            }
        }

        for(Entry e : entryToBuild.values())
        {
            e.getSnapshotXml(buf);
        }

        buf.append("    </snapshotVersions>\n");
        buf.append("  </versioning>\n");
        buf.append("</metadata>\n");
        return buf.toString();
    }

    class Entry {
        MavenArtifact          theArtifact;
        ArtifactRepositoryItem theItem;

        public Entry(Map.Entry<MavenArtifact, ArtifactRepositoryItem> entry)
        {
            theArtifact = entry.getKey();

            theItem = entry.getValue();
        }

        public void getSnapshotXml( StringBuilder buf )
        {
            //String dateVers = formatDateVersion(entry.getValue().getLastModified());
            String dateVers = formatDateVersion(theItem.getBuild());

            String itemVersion = version.replaceAll("SNAPSHOT", dateVers);
            String lastMod = new SimpleDateFormat(UFMT_FORMAT_PATTERN).format(theItem.getLastModified());
            buf.append("      <snapshotVersion>\n");

            // Optional classifier.
            if( theArtifact.classifier != null && theArtifact.classifier.length() > 0 )
            {
                buf.append("        <classifier>").append(theArtifact.classifier).append("</classifier>\n");
            }

            buf.append("        <extension>").append(theArtifact.type).append("</extension>\n");
            buf.append("        <value>").append(itemVersion).append("</value>\n");
            buf.append("        <updated>").append(lastMod).append("</updated>\n");
            buf.append("      </snapshotVersion>\n");
        }

        public String toString() {
            return theArtifact.type + ":" + theArtifact.classifier;
        }


        public boolean isNewerThan(Entry otherEntry) {
            return theItem.getLastModified().after(otherEntry.theItem.getLastModified());
        }
    }

}
