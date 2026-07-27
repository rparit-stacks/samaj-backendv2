package com.rps.samaj.directory;

import java.util.UUID;

/**
 * Flat projection for directory list — avoids per-row User / Settings / DirectorySettings loads.
 */
public interface DirectoryListRow {

    UUID getId();

    String getFullName();

    String getAvatarUrl();

    String getCity();

    String getPhone();

    String getEmail();

    Boolean getShowPhone();

    String getActionsJson();
}
