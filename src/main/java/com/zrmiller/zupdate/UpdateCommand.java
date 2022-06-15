package com.zrmiller.zupdate;

public enum UpdateCommand {

    NONE, DOWNLOAD, PATCH, CLEAN;

    private final String name;

    UpdateCommand() {
        name = name().toLowerCase();
    }

    @Override
    public String toString() {
        return name;
    }
}
